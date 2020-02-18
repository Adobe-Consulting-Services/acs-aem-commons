package com.adobe.acs.commons.util.compatability.impl;

import com.adobe.acs.commons.util.compatability.AemCompatibility;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

@Component(service = {})
public class AemCompatibilityImpl implements AemCompatibility {
    private static final Logger log = LoggerFactory.getLogger(AemCompatibilityImpl.class);
    private static final String PN_HOSTING = "hosting";
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
    public Hosting getHosting() {
        if (productInfo.getVersion().compareTo(originalCloudServiceVersion) > 0) {
            return Hosting.CLOUD;
        } else {
            return Hosting.NON_CLOUD;
        }
    }

    private transient ServiceRegistration serviceRegistration;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        productInfo = productInfoProvider.getProductInfo();

        final Dictionary<String, String> properties = new Hashtable<>();

        String hosting;
        String version = productInfo.getShortVersion();

        if (Hosting.CLOUD.equals(getHosting())) {
            hosting = "cloud";
        } else {
            hosting = "non-cloud";
            log.debug("Registering [ {} ] as an OSGi Service so it be be used to enable/disable other OSGi Components", AemCompatibility.class.getSimpleName());
        }

        properties.put(PN_HOSTING, hosting);
        properties.put(PN_HOSTING, version);

        serviceRegistration = bundleContext.registerService(AemCompatibility.class.getName(), this, new Hashtable<>());

        log.debug("Registering [ AemAsACloudService ] as an OSGi Service with OSGi properties [ hosting = {}, version = {} ] so it be be used to enable/disable other OSGi Components",
                properties.get(PN_HOSTING), properties.get(PN_VERSION));

    }
    @Deactivate
    protected void deactivate() {
        productInfo = null;

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }
}
