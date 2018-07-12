/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nanoframework.toolkit.scan.vfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link AbstractVFS} that works for most application servers.
 * @author Ben Gunter
 * @since 2.0.0
 */
public class DefaultVFS extends AbstractVFS {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverUtil.class);

    /** The magic header that indicates a JAR (ZIP) file. */
    private static final byte[] JAR_MAGIC = {'P', 'K', 3, 4 };

    private static final String JAR_SUFFIX = ".jar";

    private static final int JAR_SUFFIX_LENGTH = 4;

    @Override
    public boolean isValid() {
        return true;
    }

    @SuppressWarnings("resource")
    @Override
    public List<String> list(URL url, String path) throws IOException {
        InputStream is = null;
        try {
            var resources = (List<String>) new ArrayList<String>();

            // First, try to find the URL of a JAR file containing the requested resource. If a JAR
            // file is found, then we'll list child resources by reading the JAR.
            var jarUrl = findJarForResource(url);
            if (jarUrl != null) {
                is = jarUrl.openStream();
                LOGGER.debug("Listing " + url);
                resources = listResources(new JarInputStream(is), path);
            } else {
                var children = (List<String>) new ArrayList<String>();
                try {
                    if (isJar(url)) {
                        // Some versions of JBoss VFS might give a JAR stream even if the resource
                        // referenced by the URL isn't actually a JAR
                        is = url.openStream();
                        var jarInput = new JarInputStream(is);
                        LOGGER.debug("Listing " + url);
                        for (;;) {
                            var entry = jarInput.getNextJarEntry();
                            if (entry == null) {
                                break;
                            }

                            LOGGER.debug("Jar entry: " + entry.getName());
                            children.add(entry.getName());
                        }
                    } else {
                        /*
                         * Some servlet containers allow reading from directory resources like a text file, listing the
                         * child resources one per line. However, there is no way to differentiate between directory and
                         * file resources just by reading them. To work around that, as each line is read, try to look
                         * it up via the class loader as a child of the current resource. If any line fails then we
                         * assume the current resource is not a directory.
                         */
                        is = url.openStream();
                        var reader = new BufferedReader(new InputStreamReader(is));
                        var lines = new ArrayList<String>();
                        for (;;) {
                            var line = reader.readLine();
                            if (line == null) {
                                break;
                            }

                            LOGGER.debug("Reader entry: " + line);
                            lines.add(line);
                            if (getResources(path + '/' + line).isEmpty()) {
                                lines.clear();
                                break;
                            }
                        }

                        if (!lines.isEmpty()) {
                            LOGGER.debug("Listing " + url);
                            children.addAll(lines);
                        }
                    }
                } catch (FileNotFoundException e) {
                    /*
                     * For file URLs the openStream() call might fail, depending on the servlet container, because
                     * directories can't be opened for reading. If that happens, then list the directory directly
                     * instead.
                     */
                    if ("file".equals(url.getProtocol())) {
                        File file = new File(url.getFile());
                        LOGGER.debug("Listing directory " + file.getAbsolutePath());
                        if (file.isDirectory()) {
                            LOGGER.debug("Listing " + url);
                            children = List.of(file.list());
                        }
                    } else {
                        // No idea where the exception came from so rethrow it
                        throw e;
                    }
                }

                // The URL prefix to use when recursively listing child resources
                var prefix = url.toExternalForm();
                if (!prefix.endsWith("/")) {
                    prefix = prefix + '/';
                }

                // Iterate over immediate children, adding files and recursing into directories
                for (var child : children) {
                    var resourcePath = path + '/' + child;
                    resources.add(resourcePath);
                    var childUrl = new URL(prefix + child);
                    resources.addAll(list(childUrl, resourcePath));
                }
            }

            return resources;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {}
        }
    }

    /**
     * List the names of the entries in the given {@link JarInputStream} that begin with the specified {@code path}.
     * Entries will match with or without a leading slash.
     * @param jar The JAR input stream
     * @param path The leading path to match
     * @return The names of all the matching entries
     * @throws IOException If I/O errors occur
     */
    protected List<String> listResources(JarInputStream jar, String path) throws IOException {
        // Include the leading and trailing slash when matching names
        if (!path.startsWith("/")) {
            path = '/' + path;
        }

        if (!path.endsWith("/")) {
            path = path + '/';
        }

        // Iterate over the entries and collect those that begin with the requested path
        var resources = new ArrayList<String>();
        for (;;) {
            var entry = jar.getNextJarEntry();
            if (entry == null) {
                break;
            }

            if (!entry.isDirectory()) {
                // Add leading slash if it's missing
                var name = entry.getName();
                if (!name.startsWith("/")) {
                    name = '/' + name;
                }

                // Check file name
                if (name.startsWith(path)) {
                    LOGGER.debug("Found resource: " + name);
                    resources.add(name.substring(1)); // Trim leading slash
                }
            }
        }

        return resources;
    }

    /**
     * Attempts to deconstruct the given URL to find a JAR file containing the resource referenced by the URL. That is,
     * assuming the URL references a JAR entry, this method will return a URL that references the JAR file containing
     * the entry. If the JAR cannot be located, then this method returns null.
     * @param url The URL of the JAR entry.
     * @return The URL of the JAR file, if one is found. Null if not.
     * @throws MalformedURLException MalformedURLException
     */
    protected URL findJarForResource(URL url) throws MalformedURLException {
        LOGGER.debug("Find JAR URL: " + url);

        // If the file part of the URL is itself a URL, then that URL probably points to the JAR
        try {
            for (;;) {
                url = new URL(url.getFile());
                LOGGER.debug("Inner URL: " + url);
            }
        } catch (MalformedURLException e) {
            // This will happen at some point and serves as a break in the loop
        }

        // Look for the .jar extension and chop off everything after that
        var jarUrl = new StringBuilder(url.toExternalForm());
        var index = jarUrl.lastIndexOf(JAR_SUFFIX);
        if (index >= 0) {
            jarUrl.setLength(index + JAR_SUFFIX_LENGTH);
            LOGGER.debug("Extracted JAR URL: " + jarUrl);
        } else {
            LOGGER.debug("Not a JAR: " + jarUrl);
            return null;
        }

        // Try to open and test it
        try {
            var testUrl = new URL(jarUrl.toString());
            if (isJar(testUrl)) {
                return testUrl;
            } else {
                // WebLogic fix: check if the URL's file exists in the filesystem.
                LOGGER.debug("Not a JAR: " + jarUrl);
                jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
                var file = new File(jarUrl.toString());

                // File name might be URL-encoded
                if (!file.exists()) {
                    try {
                        file = new File(URLEncoder.encode(jarUrl.toString(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Unsupported encoding?  UTF-8?  That's unpossible.");
                    }
                }

                if (file.exists()) {
                    LOGGER.debug("Trying real file: " + file.getAbsolutePath());
                    testUrl = file.toURI().toURL();
                    if (isJar(testUrl)) {
                        return testUrl;
                    }
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.warn("Invalid JAR URL: " + jarUrl);
        }

        LOGGER.debug("Not a JAR: " + jarUrl);
        return null;
    }

    /**
     * Converts a Java package name to a path that can be looked up with a call to
     * {@link ClassLoader#getResources(String)}.
     * @param packageName The Java package name to convert to a path
     * @return package path
     */
    protected String getPackagePath(String packageName) {
        return packageName == null ? null : packageName.replace('.', '/');
    }

    /**
     * @param url The URL of the resource to test.
     * @return Returns true if the resource located at the given URL is a JAR file.
     */
    protected boolean isJar(URL url) {
        return isJar(url, new byte[JAR_MAGIC.length]);
    }

    /**
     * @param url The URL of the resource to test.
     * @param buffer A buffer into which the first few bytes of the resource are read. The buffer must be at least the
     *            size of {@link #JAR_MAGIC}. (The same buffer may be reused for multiple calls as an optimization.)
     * @return Returns true if the resource located at the given URL is a JAR file.
     */
    protected boolean isJar(URL url, byte[] buffer) {
        InputStream is = null;
        try {
            is = url.openStream();
            is.read(buffer, 0, JAR_MAGIC.length);
            if (Arrays.equals(buffer, JAR_MAGIC)) {
                LOGGER.debug("Found JAR: " + url);
                return true;
            }
        } catch (Exception e) {
            // Failure to read the stream means this is not a JAR
        } finally {
            try {
                is.close();
            } catch (Exception e) {}
        }

        return false;
    }
}
