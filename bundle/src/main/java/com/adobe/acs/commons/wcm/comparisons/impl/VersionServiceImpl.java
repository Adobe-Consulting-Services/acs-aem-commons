/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.wcm.comparisons.VersionService;
import com.day.cq.wcm.api.NameConstants;
import com.google.common.collect.Iterators;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import java.util.Iterator;

@Component
@Service
public class VersionServiceImpl implements VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionServiceImpl.class);

    @Override
    public Version lastVersion(Resource resource) {
        try {
            Resource versionableResource = resource.isResourceType(NameConstants.NT_PAGE) ? resource.getChild(NameConstants.NN_CONTENT) : resource;
            VersionManager versionManager = versionableResource.getResourceResolver().adaptTo(Session.class).getWorkspace().getVersionManager();
            final Iterator<Version> allVersions = versionManager.getVersionHistory(versionableResource.getPath()).getAllVersions();
            return Iterators.getLast(allVersions);
        } catch (RepositoryException e) {
            log.error("Error receiving last version of resource [ {} ]", resource.getName());
        }
        return null;
    }
}
