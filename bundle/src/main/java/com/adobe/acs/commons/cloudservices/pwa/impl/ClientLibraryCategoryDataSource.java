package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@org.osgi.service.component.annotations.Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=acs-commons/cloudservices/pwa/data-sources/client-library-categories",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        }
)
public class ClientLibraryCategoryDataSource extends SlingSafeMethodsServlet {
    static final String PN_CATEGORIES = "categories";
    static final String PN_TYPES = "types";

    @Reference
    private DataSourceBuilder dataSourceBuilder;

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        final ResourceResolver resolver = request.getResourceResolver();
        final ValueMap properties = request.getResource().getValueMap();
        final String[] types = properties.get(PN_TYPES, String[].class);

        final List<DataSourceOption> options = htmlLibraryManager.getLibraries().values().stream()
                .filter(cl -> cl.allowProxy())
                .filter(cl -> cl.getTypes().stream().anyMatch(type -> ".js".equals(type.extension)))
                .map(cl -> resolver.getResource(cl.getPath()))
                .filter(r -> r.getValueMap().get(PN_TYPES, String[].class) != null)
                .filter(r -> Arrays.stream(r.getValueMap().get(PN_TYPES, String[].class)).anyMatch(type -> ArrayUtils.contains(types, type)))
                .filter(r -> r.getValueMap().get(JcrConstants.JCR_TITLE, String.class) != null)
                .map(r -> {
                    final String title = r.getValueMap().get(JcrConstants.JCR_TITLE, String.class);
                    final String categories = StringUtils.join(r.getValueMap().get(PN_CATEGORIES, String[].class));

                    if (StringUtils.isNotBlank(categories)) {
                        return new DataSourceOption(title + " (" + categories + ")", categories);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        dataSourceBuilder.addDataSource(request, options);
    }
}