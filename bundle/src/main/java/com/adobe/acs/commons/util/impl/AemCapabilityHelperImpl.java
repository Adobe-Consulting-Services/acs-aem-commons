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

import javax.jcr.RepositoryException;

import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.jcr.api.SlingRepository;

import com.adobe.acs.commons.util.AemCapabilityHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * ACS AEM Commons - AEM Capability Helper
 *
 * Provides information about the current AEM installation and what it can and can't do.
 */
@Component
public class AemCapabilityHelperImpl implements AemCapabilityHelper {
    private static final Logger log = LoggerFactory.getLogger(AemCapabilityHelperImpl.class);
    private static final String PN_CLOUD_READY = "cloud-ready";
    private static final String PN_VERSION = "version";

    // This is the first Major/Minor GA Version of AEM as a Cloud Service
    private static final Version originalCloudServiceVersion = new Version(2019, 12,   0);

    @Reference(
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient ProductInfoProvider productInfoProvider;
    private transient ProductInfo productInfo;

    @Override
    public boolean isCloudReady() {
        if (productInfo.getVersion().compareTo(originalCloudServiceVersion) > 0) {
            return true;
        } else {
            return false;
        }
    }

    private transient ServiceRegistration serviceRegistration;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        productInfo = productInfoProvider.getProductInfo();

        final Dictionary<String, String> properties = new Hashtable<>();

        String cloudReady;
        String version = productInfo.getShortVersion();

        if (isCloudReady()) {
            cloudReady = Boolean.TRUE.toString();
        } else {
            cloudReady = Boolean.FALSE.toString();
            log.debug("Registering [ {} ] as an OSGi Service so it be be used to enable/disable other OSGi Components", this.getClass().getSimpleName());
        }

        properties.put(PN_CLOUD_READY, cloudReady);
        properties.put(PN_VERSION, version);

        serviceRegistration = bundleContext.registerService(this.getClass().getName(), this, new Hashtable<>());

        log.debug("Registering [ AemCapabilityHelper ] as an OSGi Service with OSGi properties [ cloud-ready = {}, version = {} ] so it be be used to enable/disable other OSGi Components",
                properties.get(PN_CLOUD_READY), properties.get(PN_VERSION));
    }

    @Deactivate
    protected void deactivate() {
        productInfo = null;

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    /** Deprecated **/

    @Reference
    private SlingRepository slingRepository;

    @Override
    public final boolean isOak() throws RepositoryException {
        final String repositoryName = slingRepository.getDescriptorValue(SlingRepository.REP_NAME_DESC).getString();
        return StringUtils.equalsIgnoreCase("Apache Jackrabbit Oak", repositoryName);
    }
}
