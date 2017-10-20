/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

package com.adobe.acs.commons.dam.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service(value = Feature.class)
@Properties({@Property(name = "service.ranking", intValue = 200)})
@Component(label = CopyAssetPublishUrlFeature.LABEL, description = CopyAssetPublishUrlFeature.DESCRIPTION, metatype = true)
public class CopyAssetPublishUrlFeature implements Feature {

    private static final Logger log = LoggerFactory.getLogger(CopyAssetPublishUrlFeature.class);


    static final String LABEL = "ACS Commons - Copy Asset publish url feature flag";

    static final String DESCRIPTION = "ACS Commons feature flag enables or disables the copy publish url button.";

    static final String FEATURE_FLAG_PID = "com.adobe.acs.commons.dam.impl.assetsurl.feature.flag";

    static final boolean DEFAULT_FLAG_ACTIVE_STATUS = true;

    Boolean isEnabled = DEFAULT_FLAG_ACTIVE_STATUS;

    @Property(boolValue = CopyAssetPublishUrlFeature.DEFAULT_FLAG_ACTIVE_STATUS, label = "Is Active?", description = "Disable to deactivate the copy asset publish url feature.")
    public static final String FEATURE_FLAG_ACTIVE_STATUS = "feature.flag.active.status";


    @Activate
    protected final void activate(Map<String, Object> config) {
        isEnabled = PropertiesUtil.toBoolean(
                config.get(FEATURE_FLAG_ACTIVE_STATUS), DEFAULT_FLAG_ACTIVE_STATUS);
        log.info("{} activated", this);
    }

    @Override
    public String getName() {
        return FEATURE_FLAG_PID;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean isEnabled(ExecutionContext executionContext) {
        return isEnabled;
    }
}
