/*
 * Copyright 2015-2018 the original author or authors.
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
package org.nanoframework.core.rest.exception;

/**
 * 重复组件服务异常.
 * @author yanghe
 * @since 2.0.0
 */
public class RouteException extends RuntimeException {
    private static final long serialVersionUID = -4050783744076776903L;

    /** */
    public RouteException() {

    }

    /**
     * @param message the message
     */
    public RouteException(String message) {
        super(message);
    }

    /**
     * @param message the message
     * @param cause the cause
     */
    public RouteException(String message, Throwable cause) {
        super(message, cause);
    }

}
