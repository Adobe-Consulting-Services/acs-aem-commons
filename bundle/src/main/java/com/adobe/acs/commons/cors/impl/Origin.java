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


import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class Origin {
    private String originStr;
    private String host;
    private int port;
    private String scheme;

    public Origin(String origin) throws URISyntaxException {
        if (StringUtils.isEmpty(origin)) {
            throw new URISyntaxException("origin is empty", "Origin is empty");
        }
        if (!origin.startsWith("http")) {
            origin = "http://" + origin;
        }
        this.originStr = origin;
        try {
            URI uri = new URI(origin);
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.scheme = uri.getScheme();
        } catch (URISyntaxException e) {
            throw e;
        }
    }

    public String getOriginStr() {
        return this.originStr;
    }

    public String getHost() {
        return this.host;
    }

    public String getScheme() {
        return this.scheme;
    }

    public int getPort() {
        return this.getPort();
    }

    @Override
    public int hashCode() {
        return this.originStr.hashCode();
    }

    @Override
    public String toString() {
        return this.originStr;
    }

    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof Origin && this.toString().equals(object.toString());
    }
}
