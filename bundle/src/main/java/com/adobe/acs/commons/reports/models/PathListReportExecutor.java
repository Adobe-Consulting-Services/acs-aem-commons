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

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Model(adaptables = SlingHttpServletRequest.class)
public class PathListReportExecutor implements ReportExecutor {

    int currentPage;

    PathListReportConfig config;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    ResourceResolver resourceResolver;

    @Override
    public String getDetails() throws ReportException {
        return StringUtils.EMPTY;
    }

    @Override
    public String getParameters() throws ReportException {
        return StringUtils.EMPTY;
    }

    List<String> extractPaths() throws ReportException {
        try {
            Template template = new Handlebars().compileInline(config.getPathArea());
            String pathsArea = template.apply(getParamPatternMap(request));

            if (StringUtils.isNotEmpty(pathsArea)) {
                return Arrays.asList(pathsArea.split("\r?\n"));
            }
        } catch (IOException ioe) {
            throw new ReportException("Exception templating query", ioe);
        }

        return Collections.emptyList();
    }

    @Override
    public ResultsPage getAllResults() throws ReportException {
        return new ResultsPage(getResources(extractPaths()), config.getPageSize(), currentPage);
    }

    @Override
    public ResultsPage getResults() throws ReportException {
        final List<String> paths = extractPaths();
        List<String> sublistPaths = paths.subList(getFrom(currentPage), getTo(paths.size()));
        return new ResultsPage(getResources(sublistPaths), config.getPageSize(), currentPage);
    }

    List<Object> getResources(final List<String> paths) {
        return Optional.ofNullable(paths)
                       .map(Collection::stream)
                       .orElseGet(Stream::empty)
                       .map(path -> resourceResolver.getResource(path))
                       .filter(Objects::nonNull)
                       .collect(Collectors.toList());
    }

    private int getFrom(final int page) {
        return page * config.getPageSize();
    }

    private int getTo(final int listSize) {
        return Math.min(getFrom(currentPage + 1), listSize);
    }

    @Override
    public void setConfiguration(final Resource config) {
        this.config = config.adaptTo(PathListReportConfig.class);
    }

    @Override
    public void setPage(final int page) {
        if (page <= 0) {
            this.currentPage = 0;
        } else {
            this.currentPage = page;
        }
    }
}
