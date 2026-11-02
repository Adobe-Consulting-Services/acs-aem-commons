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
package com.adobe.acs.commons.mcp.model;

import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

/** Bounded HTML view of an MCP report. Exports continue to use the complete report. */
@Model(adaptables = SlingHttpServletRequest.class)
public class ReportPage {
    @Self
    private SlingHttpServletRequest request;

    private AbstractReport report;
    private int page;
    private int pageSize;
    private int totalRows;

    @PostConstruct
    protected void init() {
        report = request.getResource().isResourceType(GenericBlobReport.BLOB_REPORT_RESOURCE_TYPE)
                ? request.getResource().adaptTo(GenericBlobReport.class)
                : request.getResource().adaptTo(GenericReport.class);
        if (report == null) {
            throw new IllegalStateException("Unable to read MCP report");
        }
        totalRows = report.getRows().size();
        pageSize = parsePageSize(request.getParameter("pageSize"));
        page = Math.min(parsePage(request.getParameter("page")), getPageCount() - 1);
    }

    static int parsePageSize(String value) {
        int size = parsePage(value);
        return size == 0 ? 100 : Math.min(size, 500);
    }

    static int parsePage(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getName() { return report.getName(); }
    public List<String> getColumns() { return report.getColumns(); }
    public List<String> getColumnNames() { return report.getColumnNames(); }
    public List<ValueMap> getRows() {
        int start = page * pageSize;
        return report.getRows().subList(start, start + Math.min(pageSize, totalRows - start));
    }
    public int getPageSize() { return pageSize; }
    public int getPageCount() { return Math.max(1, (int) Math.ceil((double) totalRows / pageSize)); }
    public int getPageNumber() { return page + 1; }
    public int getTotalRows() { return totalRows; }
    public int getFirstRow() { return totalRows == 0 ? 0 : page * pageSize + 1; }
    public int getLastRow() { return page * pageSize + getRows().size(); }
    public boolean isHasPrevious() { return page > 0; }
    public boolean isHasNext() { return page < getPageCount() - 1; }
    public String getPreviousUrl() { return pageUrl(page - 1); }
    public String getNextUrl() { return pageUrl(page + 1); }
    private String pageUrl(int target) {
        return request.getRequestURI() + "?page=" + target + "&pageSize=" + pageSize;
    }
}
