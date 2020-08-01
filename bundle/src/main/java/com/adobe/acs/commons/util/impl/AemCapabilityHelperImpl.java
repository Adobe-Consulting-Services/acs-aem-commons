/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.util.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

/**
 * ACS AEM Commons - AEM Capability Helper
 *
 * Provides information about the current AEM installation and what it can and can't do.
 *
 * @deprecated All supported AEM's run on Oak repositories now, so this will always return true.
 */
@Deprecated
@SuppressWarnings("deprecation")
@Component
public class AemCapabilityHelperImpl implements com.adobe.acs.commons.util.AemCapabilityHelper {
    @Reference
    private transient SlingRepository slingRepository;

    @Override
    public final boolean isOak() throws RepositoryException {
        final String repositoryName = slingRepository.getDescriptorValue(Repository.REP_NAME_DESC).getString();
        return StringUtils.equalsIgnoreCase("Apache Jackrabbit Oak", repositoryName);
    }
}
