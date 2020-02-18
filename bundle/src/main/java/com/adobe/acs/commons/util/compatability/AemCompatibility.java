package com.adobe.acs.commons.util.compatability;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This OSGi Service can be used in 2 different ways....
 *
 * 1. It can be used In-line in code to detect the Hosting type (Cloud or Non-Cloud)
 *
 * @Reference
 * AemCompatibility aemCompatibility;
 * ...
 * if (Hosting.CLOUD.equals(aemCompatibility.getHosting()) {
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
 *        target="&(hosting=cloud)"
 *        scope = ReferenceCardinality.MANDATORY
 *    )
 *    AemCompatibility aemCompatibility;
 *    ...
 * }
 *
 * ... OR ....
 *
 * @Component
 *   public class IOnlyWorkOnAmsOrOnPrem implements Bar {
 *
 *      @Reference(
 *          target="&(hosting=non-cloud)"
 *          scope = ReferenceCardinality.MANDATORY
 *      )
 *      AemCompatibility aemCompatibility;
 *      ...
 *  }
 */
@ProviderType
public interface AemCompatibility {
    enum Hosting {
        CLOUD,
        NON_CLOUD
    }

    /**
     *
     * @return
     */
    Hosting getHosting();
}
