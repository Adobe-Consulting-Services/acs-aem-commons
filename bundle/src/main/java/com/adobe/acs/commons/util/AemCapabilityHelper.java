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

import com.adobe.acs.commons.util.compatability.AemCompatibility;
import org.osgi.annotation.versioning.ProviderType;


import org.osgi.annotation.versioning.ProviderType;

/**
 * This OSGi Service can be used in 2 different ways....
 *
 * 1. It can be used In-line in code to detect the Hosting type (Cloud or Non-Cloud)
 *
 * @Reference
 * AemCapabilityHelper aemCapabilityHelper;
 * ...
 * if ((aemCapabilityHelper.isCloudReady()) {
 *     .. Do something that only works for the Cloud ..
 * } else {
 *     .. Do something for AMS / On-Prem ..
 * }
 *
 * 2. It can be used to allow/stop OSGi Components from starting entirely
 *
 *
 * @Component
 * public class IOnlyWorkOnTheCloud implements Foo {
 *
 *    @Reference(
 *        target="&(cloud-ready=true)"
 *        scope = ReferenceCardinality.MANDATORY
 *    )
 *    AemCapabilityHelper aemCapabilityHelper;
 *    ...
 * }
 *
 * ... OR ...
 *
 * @Component
 *   public class IOnlyWorkOnAmsOrOnPrem implements Bar {
 *
 *      @Reference(
 *          target="&(cloud-ready=false)"
 *          scope = ReferenceCardinality.MANDATORY
 *      )
 *      AemCapabilityHelper aemCapabilityHelper;
 *      ...
 *  }
 */
@ProviderType
public interface AemCapabilityHelper {
    /**
     * Determines if the AEM installation is running on an Apache Jackrabbit Oak-based repository.
     * 
     * With the current versions of ACS AEM Commons the support for non-Oak based AEM versions has been dropped,
     * so the usage of this method is no longer required.
     * @return true is running on Oak
     * @throws RepositoryException
     * @Deprecated All ACS AEM Commons supported Repos are Oak now; This should always return true.
     */
    boolean isOak() throws RepositoryException;

    /**
     * Checks if the AEM running is "AEM as a Cloud Service" or an AMS/On-Prem/6.x version of AEM
     *
     * @return true if the AEM is running as AEM as a Cloud Service (either in Adobe Cloud as AEM as a CLoud Service OR AEM as a Cloud Service SDK QuickStart Jar)
     */
    boolean isCloudReady();
}
