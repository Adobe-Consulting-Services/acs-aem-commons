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
package com.adobe.acs.commons.replication;

import aQute.bnd.annotation.ProviderType;

/**
 * The result of a replication.
 */
@ProviderType
public class ReplicationResult {

    private final Status status;
    private final String version;
    private final String path;

    public ReplicationResult(String path, Status status) {
        this.path = path;
        this.status = status;
        this.version = null;
    }

    public ReplicationResult(String path, Status status, String version) {
        this.path = path;
        this.status = status;
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public Status getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return String.format("ReplicationResult [path=%s, status=%s, version=%s]", path, status, version);
    }

    @SuppressWarnings("squid:S00115")
    public enum Status {
        replicated, not_replicated, error
    }

}
