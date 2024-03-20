/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.RequireAem;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * ACS AEM Commons - RequireAem
 *
 * Allows ACS Commons features determine if they are running on AEM as a Cloud Service or not.
 */
@Component(
        service = {},
        immediate = true
)
public class RequireAemImpl implements RequireAem {
    private static final Logger log = LoggerFactory.getLogger(RequireAemImpl.class);

    private static final String PN_DISTRIBUTION = "distribution";
    private static final String PN_VERSION = "version";

    // This is the first Major/Minor GA Version of AEM as a Cloud Service
    private static final Version originalCloudServiceVersion = new Version(2019, 12,   0);

    @Reference
    private ProductInfoProvider productInfoProvider;

    private ProductInfo productInfo;
    private ServiceRegistration<?> serviceRegistration;

    @Override
    public Distribution getDistribution() {
        if (productInfo.getVersion().compareTo(originalCloudServiceVersion) > 0) {
            return Distribution.CLOUD_READY;
        } else {
            return Distribution.CLASSIC;
        }
    }

    @Activate
    protected void activate(final BundleContext bundleContext) {
        productInfo = productInfoProvider.getProductInfo();

        @SuppressWarnings("squid:java:S1149")
        final Dictionary<String, Object> properties = new Hashtable<>();

        String distribution;
        String version = productInfo.getShortVersion();

        if (Distribution.CLOUD_READY.equals(getDistribution())) {
            distribution = Distribution.CLOUD_READY.getValue();
        } else {
            distribution =  Distribution.CLASSIC.getValue();
        }

        properties.put(PN_DISTRIBUTION, distribution);
        properties.put(PN_VERSION, version);

        serviceRegistration = bundleContext.registerService(RequireAem.class.getName(), this, properties);

        log.info("Registering [ RequireAem.class ] as an OSGi Service with OSGi properties [ distribution = {}, version = {} ] so it can be used to enable/disable other OSGi Components",
                properties.get(PN_DISTRIBUTION), properties.get(PN_VERSION));
    }

    @Deactivate
    protected void deactivate() {
        productInfo = null;

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
