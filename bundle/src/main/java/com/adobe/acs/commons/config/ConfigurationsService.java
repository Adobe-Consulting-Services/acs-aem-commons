package com.adobe.acs.commons.config;

import com.day.cq.wcm.api.Page;

public interface ConfigurationsService {
Configuration getConfiguration(Page currentPage, String configPagePath);
Configuration getConfiguration(Page currentPage);
}
