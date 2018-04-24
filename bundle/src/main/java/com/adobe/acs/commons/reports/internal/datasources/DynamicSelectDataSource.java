/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.reports.internal.datasources;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.jcr.query.Query.JCR_SQL2;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=acs-commons/components/utilities/report-builder/data-sources/dynamic-select",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        }
)
public class DynamicSelectDataSource extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(DynamicSelectDataSource.class);

    static final String PN_DROP_DOWN_QUERY_LANGUAGE = "dropDownQueryLanguage";
    static final String PN_DROP_DOWN_QUERY = "dropDownQuery";
    static final String PN_ALLOW_PROPERTY_NAMES = "allowedPropertyNames";

    @Reference
    private DataSourceBuilder dataSourceBuilder;

    @Reference
    private QueryHelper queryHelper;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {

        final ResourceResolver resolver = request.getResourceResolver();
        final ValueMap properties = request.getResource().getValueMap();

        final List<DataSourceOption> options = new ArrayList<>();

        try {
            // The query language property
            final String queryLanguage = properties.get(PN_DROP_DOWN_QUERY_LANGUAGE, JCR_SQL2);
            // The query statement (this must match the queryLanguage else the query will fail)
            final String queryStatement = properties.get(PN_DROP_DOWN_QUERY, String.class);
            // The property names to extract; these are specified as a String[] property
            final String[] allowedPropertyNames = properties.get(PN_ALLOW_PROPERTY_NAMES, new String[0]);

            if (StringUtils.isNotBlank(queryStatement)) {
                // perform the query
                final List<Resource> results = queryHelper.findResources(resolver, queryLanguage, queryStatement, StringUtils.EMPTY);
                final List<String> distinctOptionValues = new ArrayList<>();

                for (final Resource resource : results) {
                    // For each result...
                    // - ensure the property value is a String
                    // - ensure either no properties have been specified (which means ALL properties are eligible) OR the property is in the list of enumerated propertyNames
                    // - ensure this property value has not already been processed
                    // -- if the above criteria is satisfied, add to the options
                    resource.getValueMap().entrySet().stream()
                            .filter(entry -> entry.getValue() instanceof String)
                            .filter(entry -> ArrayUtils.isEmpty(allowedPropertyNames) || ArrayUtils.contains(allowedPropertyNames, entry.getKey()))
                            .filter(entry -> !distinctOptionValues.contains(entry.getValue().toString()))
                            .forEach(entry -> {
                                String value = entry.getValue().toString();
                                distinctOptionValues.add(value);
                                options.add(new DataSourceOption(value, value));
                            });
                }
            }

            // Create a datasource from the collected options, even if there are 0 options.
            dataSourceBuilder.addDataSource(request, options);

        } catch (Exception e) {
            log.error("Unable to collect the information to populate the ACS Commons Report Builder dynamic-select drop-down.", e);
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
