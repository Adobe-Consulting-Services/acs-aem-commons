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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.day.cq.tagging.TagManager;

/**
 * Model for rendering Tag properties to CSV cells.
 */
@Model(adaptables=Resource.class)
public class TagReportCellCSVExporter implements ReportCellCSVExporter {

  private static final Logger log = LoggerFactory.getLogger(TagReportCellCSVExporter.class);

  @Inject
  private String property;
  
  @Override
  public String getValue(Object result) {
    
    Resource resource = (Resource) result;
    
    TagManager tagMgr = resource.getResourceResolver().adaptTo(TagManager.class);

    log.debug("Loading tags from {}@{}", resource.getPath(), property);
    List<String> tags = new ArrayList<String>();
    String[] values = resource.getValueMap().get(property, String[].class);
    if (values != null) {
      for (String value : values) {
        tags.add(tagMgr.resolve(value).getTitle());
      }
    }
    log.debug("Loaded {} tags", tags);

    return StringUtils.join(tags,";");
  }

}
