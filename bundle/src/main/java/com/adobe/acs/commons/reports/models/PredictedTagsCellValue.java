package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.internal.ExporterUtil;
import com.adobe.acs.commons.reports.internal.PredictedTagsUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.inject.Inject;
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

    @Inject @Optional
    private Double lowerConfidenceThreshold;

    @Inject @Optional
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
