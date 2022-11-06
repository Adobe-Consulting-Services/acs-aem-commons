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
package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.internal.DelimiterConfiguration;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.adobe.acs.commons.reports.internal.ExporterUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

/**
 * An exporter for exporting formatted string values
 */
@Model(adaptables = Resource.class)
public class StringReportCellCSVExporter implements ReportCellCSVExporter {

  @Inject
  private String property;

  @Inject
  @Optional
  private String format;

  @OSGiService
  private DelimiterConfiguration delimiterConfiguration;

  public StringReportCellCSVExporter() {}

  /**
   * Used only for testing.
   * @param delimiterConfiguration the delimiter configuration to use for this exporter
   */
  StringReportCellCSVExporter(DelimiterConfiguration delimiterConfiguration) {
    this.delimiterConfiguration = delimiterConfiguration;
  }

  @Override
  public String getValue(Object result) {
    final String relativePropertyPath = ExporterUtil.relativizePath(property);

    Resource resource = (Resource) result;
    ReportCellValue val = new ReportCellValue(resource, relativePropertyPath);
    List<String> values = new ArrayList<>();
    if (val.getValue() != null) {
      if (val.isArray()) {
        for (String value : val.getMultipleValues()) {
          values.add(value);
        }
      } else {
        values.add(val.getSingleValue());
      }
    }
    if (StringUtils.isNotBlank(format)) {
      for (int i = 0; i < values.size(); i++) {
        values.set(i, String.format(format, values.get(i)));
      }
    }
    return StringUtils.join(values, delimiterConfiguration.getMultiValueDelimiter());
  }
}