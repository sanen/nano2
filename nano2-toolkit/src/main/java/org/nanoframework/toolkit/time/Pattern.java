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
package org.nanoframework.toolkit.time;

/**
 * @author yanghe
 * @since 1.0
 */
public enum Pattern {
    /** The 'Date' Pattern. */
    DATE("yyyy-MM-dd"),
    /** The ‘Time’ Pattern. */
    TIME("HH:mm:ss"),
    /** The 'DateTime' Pattern. */
    DATETIME("yyyy-MM-dd HH:mm:ss"),
    /** The 'Timestamp' Pattern. */
    TIMESTAMP("yyyy-MM-dd HH:mm:ss.SSS");

    private String pattern;

    Pattern(final String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return 时间转化格式
     */
    public String get() {
        return pattern;
    }

}
