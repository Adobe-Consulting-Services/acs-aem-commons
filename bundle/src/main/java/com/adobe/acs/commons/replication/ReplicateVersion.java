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

import java.util.Date;
import java.util.List;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * Service used to identify the latest version of the entire resource tree of the specified resources at the date specified and
 *  issue replication request to the agents specified.
 */
@ProviderType
public interface ReplicateVersion {

    /**
     * Identifies the latest version of the entire resource tree of the specified resources at the date specified and
     *   issue replication request to the  specified agents.
     * @param resolver the resource resolver
     * @param rootPaths one or more paths to replicate, recursively
     * @param agents one or more agent ids
     * @param date the date from which to replicate
     * @return a list of status objects
     */
    List<ReplicationResult> replicate(
            ResourceResolver resolver, String[] rootPaths, String[] agents,
            Date date);

}
