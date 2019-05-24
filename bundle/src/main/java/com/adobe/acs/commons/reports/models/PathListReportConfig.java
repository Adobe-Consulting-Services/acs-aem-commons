package com.adobe.acs.commons.reports.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

@Model(adaptables = Resource.class)
public interface PathListReportConfig {
    @Inject
    int getPageSize();

    @Inject
    String getPathsArea();
}
