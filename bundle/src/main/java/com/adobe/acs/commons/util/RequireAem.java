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
package com.adobe.acs.commons.util;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This OSGi Service allows to distinguish between AEM Classic or AEM as a Cloud Service instance types. It can be used in two different ways:
 *
 * <ol>
 * <li>To check if AEM is running as Cloud Ready (AEM as a Cloud Service or local AEM as a Cloud Service SDK QuickStart Jar) or not by leveraging its method in code.
 * <pre>
 * &#64;Reference
 * RequireAem requireAem;
 * ...
 * if ((Distribution.CLOUD_READY.equals(requireAem.getDistribution()) {
 *     .. Do something that only works for the Cloud ..
 * } else {
 *     .. Do something for AMS / On-Prem ..
 * }
 * </pre>
 * </li>
 * <li>To prevent OSGi Components from starting by using it as service reference with target (without actually leveraging it in code).
 * <pre>
 * &#64;Component
 * public class IOnlyWorkOnTheCloud implements Foo {
 *     &#64;Reference(target="(distribution=cloud-ready)")
 *     RequireAem requireAem;
 *     ...
 * }
 * </pre>
 * ... OR ...
 * <pre>
 * &#64;Component
 * public class IOnlyWorkOnAmsOrOnPrem implements Bar {
 *     &#64;Reference(target="(distribution=classic)")
 *     RequireAem requireAem;
 *     ...
 * }
 * </pre>
 * </li>
 * </ol>
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
