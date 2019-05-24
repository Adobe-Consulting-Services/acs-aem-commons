package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Model(adaptables = SlingHttpServletRequest.class)
public class PathListReportExecutor implements ReportExecutor {

    private static final Logger log = LoggerFactory.getLogger(PathListReportExecutor.class);

    private int currentPage;

    private PathListReportConfig config;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Override
    public String getDetails() throws ReportException {
        return StringUtils.EMPTY;
    }

    @Override
    public String getParameters() throws ReportException {
        return StringUtils.EMPTY;
    }

    private List<String> extractPaths() throws ReportException {
        try {
            Template template = new Handlebars().compileInline(config.getPathsArea());
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

    private List<Object> getResources(final List<String> paths) {
        return paths.stream().map(path -> resourceResolver.getResource(path))
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

