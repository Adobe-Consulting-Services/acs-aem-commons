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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;


/**
 * An exporter for exporting the containing page
 */
@Model(adaptables = Resource.class)
public class ContainingPageReportCellCSVExporter implements ReportCellCSVExporter {

  @Override
  public String getValue(Object obj) {
    Resource result = (Resource) obj;
    PageManager pageMgr = result.getResourceResolver().adaptTo(PageManager.class);
    Page page = pageMgr.getContainingPage(result);
    return page != null ? page.getPath() : "";
  }

}
