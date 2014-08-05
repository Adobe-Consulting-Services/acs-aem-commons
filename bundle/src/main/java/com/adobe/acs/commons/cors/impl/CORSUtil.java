/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.cors.impl;


public class CORSUtil {

    private CORSUtil() {

    }

    public static String[] getHeadersAsArray(final String headerValue) {

        if (headerValue == null)
            return new String[0]; // empty array

        String trimmedHeaderValue = headerValue.trim();

        if (trimmedHeaderValue.isEmpty())
            return new String[0];

        return trimmedHeaderValue.split("\\s*,\\s*|\\s+");
    }
}
