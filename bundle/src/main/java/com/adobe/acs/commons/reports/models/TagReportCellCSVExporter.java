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
package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.internal.DelimiterConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.adobe.acs.commons.reports.internal.ExporterUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.day.cq.tagging.TagManager;

/**
 * Model for rendering Tag properties to CSV cells.
 */
@Model(adaptables = Resource.class)
public class TagReportCellCSVExporter implements ReportCellCSVExporter {

  private static final Logger log = LoggerFactory.getLogger(TagReportCellCSVExporter.class);

  @OSGiService
  private DelimiterConfiguration delimiterConfiguration;

  @ValueMapValue
  private String property;

  public TagReportCellCSVExporter() {}

  /**
   * Used only for testing.
   * @param delimiterConfiguration the delimiter configuration to use for this exporter
   */
  TagReportCellCSVExporter(DelimiterConfiguration delimiterConfiguration) {
    this.delimiterConfiguration = delimiterConfiguration;
  }

  @Override
  public String getValue(Object result) {
    final String relativePropertyPath = ExporterUtil.relativizePath(property);

    Resource resource = (Resource) result;

    TagManager tagMgr = resource.getResourceResolver().adaptTo(TagManager.class);

    log.debug("Loading tags from {}@{}", resource.getPath(), relativePropertyPath);
    List<String> tags = new ArrayList<>();
    String[] values = resource.getValueMap().get(relativePropertyPath, String[].class);
    if (values != null) {
      for (String value : values) {
        tags.add(Optional.ofNullable(tagMgr).map(tm -> tm.resolve(value).getTitle()).orElse(value));
      }
    }
    log.debug("Loaded {} tags", tags);

    return StringUtils.join(tags, delimiterConfiguration.getMultiValueDelimiter());
  }

}
