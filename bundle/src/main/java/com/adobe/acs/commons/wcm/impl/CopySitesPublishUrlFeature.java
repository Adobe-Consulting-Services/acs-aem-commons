/*
 * ACS AEM Commons Bundle
 *
 * Copyright (C) 2013 - 2024 Adobe
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

package com.adobe.acs.commons.wcm.impl;

import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        property = {
                "service.ranking=200"
        },
        service = Feature.class
)
@Designate(ocd = CopySitesPublishUrlFeature.Config.class)
public class CopySitesPublishUrlFeature implements Feature {
    static final String FEATURE_FLAG_PID = "com.adobe.acs.commons.wcm.impl.copysitespublishurlfeature.feature.flag";

    private Config config;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Copy Sites Publish URL Feature Flag",
            description = "ACS Commons feature flag enables or disables the copy publish url dropdown field in the Sites Editor."
    )
    @interface Config {
        @AttributeDefinition(
                name = "Enable",
                description = "Check to enable the AEM Sites Copy Publish URL feature."
        )
        boolean feature_flag_active_status() default false;
    }

    @Activate
    protected final void activate(Config config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return FEATURE_FLAG_PID;
    }

    @Override
    public String getDescription() {
        return "ACS AEM Commons feature flag enables or disables the copy publish URL dropdown field in the Sites Editor.";
    }

    @Override
    public boolean isEnabled(ExecutionContext executionContext) {
        return config.feature_flag_active_status();
    }
}