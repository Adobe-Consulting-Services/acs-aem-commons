/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.redirectmaps.models;

/**
 * Simple POJO for map entry items based on Vanity paths for Redirect Maps.
 */
public class MapEntry {
    private final String origin;
    private final String source;
    private String status;
    private final String target;
    private boolean valid = true;

    public MapEntry(String source, String target, String origin) {
        source = source.trim();
        this.source = source;
        this.target = target;
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    public String getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public String getTarget() {
        return target;
    }

    public boolean isValid() {
        return valid;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public String toString() {
        return "MapEntry [origin=" + origin + ", source=" + source + ", status=" + status + ", target=" + target
                + ", valid=" + valid + "]";
    }
}