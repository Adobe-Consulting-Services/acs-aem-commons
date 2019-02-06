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
package com.adobe.acs.commons.wcm.impl;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import javax.servlet.Servlet;

import org.apache.commons.lang3.ArrayUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;

@Component(service = Servlet.class,
        property = {
                SLING_SERVLET_PATHS + "=/bin/acs-commons/dynamic-classicui-clientlibs.json"
        })
@Designate(ocd = DynamicClassicUiClientLibraryServlet.Config.class)
public class DynamicClassicUiClientLibraryServlet extends AbstractDynamicClientLibraryServlet {

    static final String CATEGORY_LIMIT = "acs-commons.cq-widgets.add-ons.classicui-limit-parsys";
    static final String CATEGORY_PLACEHOLDER = "acs-commons.cq-widgets.add-ons.classicui-parsys-placeholder";

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Dynamic Classic UI Client Library Loader",
            description = "Allows for dynamic loading of optional Classic UI Client Libraries"
    )
    public @interface Config {

        @AttributeDefinition(description = "Exclude all client library categories")
        boolean exclude_all();

        @AttributeDefinition(description = "Client Library Categories", defaultValue = {CATEGORY_LIMIT,
                CATEGORY_PLACEHOLDER})
        String[] categories();

    }

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    @Activate
    protected void activate(DynamicClassicUiClientLibraryServlet.Config config) {
        String[] categories = config.categories();
        if (ArrayUtils.isEmpty(config.categories())) {
            categories = new String[]{CATEGORY_LIMIT, CATEGORY_PLACEHOLDER};
        }
        super.activate(categories, config.exclude_all(), htmlLibraryManager);
    }
}
