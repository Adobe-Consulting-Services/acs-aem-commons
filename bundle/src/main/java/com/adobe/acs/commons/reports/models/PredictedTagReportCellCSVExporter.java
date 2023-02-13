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

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.acs.commons.reports.internal.ExporterUtil;
import com.adobe.acs.commons.reports.internal.PredictedTagsUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for rendering PredictedTag properties to CSV cells.
 */
@Model(adaptables = Resource.class)
public class PredictedTagReportCellCSVExporter implements ReportCellCSVExporter {

    public static final String CONFIDENCE_FORMAT_STRING = "%.4f";

    public static final String EMPTY_STRING = "";
    public static final String CONFIDENCE_BRACKET_OPEN = "[";
    public static final String CONFIDENCE_BRACKET_CLOSE = "]";
    public static final String VALUE_SEPARATOR = ";";
    public static final String SPACE_SEPARATOR = " ";

    @Inject
    private String property;
    @Inject @Optional
    private Double lowerConfidenceThreshold;

    @Inject @Optional
    private boolean confidenceShown;

    private PredictedTagsUtil predictedTagsUtil = new PredictedTagsUtil();

    @Override
    public String getValue(final Object result) {
        final Resource resource = (Resource) result;
        final String relativePropertyPath = ExporterUtil.relativizePath(property);

        final List<PredictedTag> predictedTags = predictedTagsUtil.getPredictedTags(resource, relativePropertyPath, lowerConfidenceThreshold);
        if (CollectionUtils.isEmpty(predictedTags)) {
            return EMPTY_STRING;
        }

        final List<String> predictedTagRenderedValue = new ArrayList<>();
        for (final PredictedTag predictedTag : predictedTags) {
            predictedTagRenderedValue.add(asCellCSVValue(predictedTag));
        }

        return StringUtils.join(predictedTagRenderedValue, VALUE_SEPARATOR);
    }


    public String asCellCSVValue(PredictedTag predictedTag) {
        if (predictedTag == null) {
            return EMPTY_STRING;
        }

        StringBuilder result = new StringBuilder(predictedTag.getName());
        if (confidenceShown) {
            result.append(SPACE_SEPARATOR);
            result.append(CONFIDENCE_BRACKET_OPEN);
            result.append(String.format(CONFIDENCE_FORMAT_STRING, predictedTag.getConfidence()));
            result.append(CONFIDENCE_BRACKET_CLOSE);
        }
        return result.toString();
    }
}
