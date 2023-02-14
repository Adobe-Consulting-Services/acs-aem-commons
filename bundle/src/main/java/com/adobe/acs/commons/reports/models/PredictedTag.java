/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2023 Adobe
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
package com.adobe.acs.commons.reports.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

/**
 * Sling Model used for rendering PredictedTags in reports
 */
@Model(adaptables = Resource.class)
public class PredictedTag {

    @Inject
    private String name;
    @Inject
    private Double confidence;
    @Inject
    private Boolean isCustom;

    public String getName() {
        return name;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Boolean isCustom() {
        return isCustom;
    }

    @Override
    public String toString() {
        return "PredictedTag{"
                + "name='" + name + "',"
                + "confidence='" + confidence + "',"
                + "custom=" + isCustom
                + '}';
    }
}
