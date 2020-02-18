package com.adobe.acs.commons.util.compatability.impl;

import com.adobe.acs.commons.util.compatability.AemCompatibility;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component
public class AemCompatibilityImpl implements AemCompatibility {
    private static final Version originalCloudServiceVersion = new Version(2019, 12,   0);

    @Reference(
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private transient ProductInfoProvider productInfoProvider;

    @Override
    public boolean isCloudService() {
        final ProductInfo productInfo = productInfoProvider.getProductInfo();
        return productInfo.getVersion().compareTo(originalCloudServiceVersion) > 0;
    }

    @Override
    public boolean isQuickStart() {
        return !isCloudService();
    }
}
