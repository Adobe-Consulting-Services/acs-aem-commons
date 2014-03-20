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

package com.adobe.acs.commons.configuration.osgi.impl;


public final class OsgiConfigConstants {
    public static enum OsgiConfigurationType {
        SINGLE,
        FACTORY
    }

    public static final String PN_REQUIRED_PROPERTIES = "acs.requiredProperties";
    public static final String PN_CONFIGURATION_SRC = "acs.configurationSrc";
    public static final String PN_CONFIGURATION_TYPE = "acs.configurationType";
    public static final String PN_TARGET_CONFIG = "acs.targetConfig";
    public static final String PN_PID = "acs.pid";
    public static final String NT_SLING_OSGI_CONFIG = "sling:OsgiConfig";

    private OsgiConfigConstants() {
    }
}
