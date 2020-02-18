package com.adobe.acs.commons.util.compatability.impl;

import com.adobe.acs.commons.util.compatability.AemAsACloudService;
import com.adobe.acs.commons.util.compatability.AemCompatibility;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Component(service = {})
public class AemAsACloudServiceImpl implements AemAsACloudService {
    private static final Logger log = LoggerFactory.getLogger(AemAsACloudServiceImpl.class);

    @Reference(
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient AemCompatibility aemCompatibility;

    private transient ServiceRegistration serviceRegistration;

    @Activate
    protected void activate(final BundleContext bundleContext) {
        if (aemCompatibility.isCloudService()) {
            log.debug("Registering [ {} ] as an OSGi Service so it be be used to enable/disable other OSGi Components", AemAsACloudService.class.getSimpleName());
            serviceRegistration = bundleContext.registerService(AemAsACloudService.class.getName(), this, new Hashtable<>());
        }
    }

    @Deactivate
    protected void deactivate() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }
}
