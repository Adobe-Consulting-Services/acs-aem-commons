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

import com.adobe.acs.commons.reports.internal.ExporterUtil;
import com.adobe.acs.commons.reports.internal.PredictedTagsUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.List;

/**
 * Model used for rendering a predicted tag as a report entry (a "cell")
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class PredictedTagsCellValue {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String property;

    @ValueMapValue @Optional
    private Double lowerConfidenceThreshold;

    @ValueMapValue @Optional
    private boolean confidenceShown;

    private PredictedTagsUtil predictedTagsUtil = new PredictedTagsUtil();

    public List<PredictedTag> getPredictedTags() {
        final String relativePropertyPath = ExporterUtil.relativizePath(property);
        final Resource resource = (Resource) request.getAttribute("result");

        return predictedTagsUtil.getPredictedTags(resource, relativePropertyPath, lowerConfidenceThreshold);
    }

    public boolean isConfidenceShown() {
        return confidenceShown;
    }
}
