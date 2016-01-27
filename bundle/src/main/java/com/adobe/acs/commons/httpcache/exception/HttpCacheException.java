/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing all failure conditions in http cache. All other custom exceptions in http cache has to
 * extend this.
 */
public class HttpCacheException extends Exception {

    public HttpCacheException() {
    }

    public HttpCacheException(String message) {
        super(message);
    }

    public HttpCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheException(Throwable cause) {
        super(cause);
    }

    public HttpCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        this(message, cause);
    }
}
