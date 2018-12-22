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
package com.adobe.acs.commons.util;

import javax.jcr.RepositoryException;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface AemCapabilityHelper {

    /**
     * Determines if the AEM installation is running on an Apache Jackrabbit Oak-based repository.
     * 
     * With the current versions of ACS AEM Commons the support for non-Oak based AEM versions has been dropped,
     * so the usage of this method is no longer required.
     * @return true is running on Oak
     * @throws RepositoryException
     * @Deprecated
     */
    boolean isOak() throws RepositoryException;
}
