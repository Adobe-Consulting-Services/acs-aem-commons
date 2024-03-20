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

package com.adobe.acs.commons.dam.impl;

import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        property = {
                "service.ranking=200"
        },
        service = Feature.class
)
@Designate(ocd = CopyAssetPublishUrlFeature.Config.class)
public class CopyAssetPublishUrlFeature implements Feature {
    static final String FEATURE_FLAG_PID = "com.adobe.acs.commons.dam.impl.copyassetpublishurlfeature.feature.flag";

    private Config config;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Copy Asset Publish URL Feature Flag",
            description = "ACS Commons feature flag enables or disables the copy publish url button. This also requires creating the requisite Wrapper Client Library to expose the supporting JS and CSS."
    )
    @interface Config {
        @AttributeDefinition(
                name = "Enable",
                description = "Check to enable the AEM Assets Copy Asset Publish URL feature."
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
        return "ACS AEM Commons feature flag enables or disables the copy publish URL button.";
    }

    @Override
    public boolean isEnabled(ExecutionContext executionContext) {
        return config.feature_flag_active_status();
    }
}