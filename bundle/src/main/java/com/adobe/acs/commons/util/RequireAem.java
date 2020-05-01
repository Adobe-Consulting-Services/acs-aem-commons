/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

import org.osgi.annotation.versioning.ProviderType;

/**
 * This OSGi Service can be used in 2 different ways....
 *
 * 1. It can be used In-line in code to detect if AEM is running as Cloud Ready (AEM as a Cloud Service or local AEM as a Cloud Service SDK QuickStart Jar) or not.
 *
 * @Reference
 * RequireAem requireAem;
 * ...
 * if ((Distribution.CLOUD_READY.equals(requireAem.getDistribution()) {
 *     .. Do something that only works for the Cloud ..
 * } else {
 *     .. Do something for AMS / On-Prem ..
 * }
 *
 * 2. It can be used to allow/stop OSGi Components from starting entirely
 *
 * @Component
 * public class IOnlyWorkOnTheCloud implements Foo {
 *
 *    @Reference(target="(distribution=cloud-ready)")
 *    RequireAem requireAem;
 *    ...
 * }
 *
 * ... OR ...
 *
 * @Component
 * public class IOnlyWorkOnAmsOrOnPrem implements Bar {
 *
 *      @Reference(target="(distribution=classic)")
 *      RequireAem requireAem;
 *      ...
 *  }
 */
@ProviderType
public interface RequireAem {
    enum Distribution {
        CLOUD_READY("cloud-ready"),
        CLASSIC("classic");

        private String value;

        Distribution(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * @return the running AEM's environments distribution type.
     */
    Distribution getDistribution();
}
