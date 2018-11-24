/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.wcm.datasources.impl;

import com.adobe.acs.commons.wcm.datasources.DataSourceBuilder;
import com.adobe.acs.commons.wcm.datasources.DataSourceOption;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - WCM DataSource Builder
 */
@Component
@Service
public class DataSourceBuilderImpl implements DataSourceBuilder {

    @Override
    public void addDataSource(final SlingHttpServletRequest slingRequest, final List<DataSourceOption> options) {

        final ArrayList<Resource> resourceList = new ArrayList<Resource>();

        DataSource dataSource = null;

        for (final DataSourceOption option : options) {

            final Map map = new HashMap();

            map.put(TEXT, option.getText());
            map.put(VALUE, option.getValue());

            resourceList.add(new ValueMapResource(slingRequest.getResourceResolver(),
                    new ResourceMetadata(),
                    "",
                    new ValueMapDecorator(map)));
        }

        if (resourceList.size() > 0) {
            dataSource = new SimpleDataSource(resourceList.iterator());
        } else {
            dataSource = EmptyDataSource.instance();
        }

        slingRequest.setAttribute(DataSource.class.getName(), dataSource);
    }

    @Override
    public void writeDataSourceOptions(final SlingHttpServletRequest slingRequest,
            final SlingHttpServletResponse slingResponse) throws IOException {
        final DataSource datasource = (DataSource) slingRequest.getAttribute(DataSource.class.getName());
        final JsonArray jsonArray = new JsonArray();

        if (datasource != null) {
            final Iterator<Resource> iterator = datasource.iterator();

            if (iterator != null) {
                while (iterator.hasNext()) {
                    final Resource dataResource = iterator.next();

                    if (dataResource != null) {
                        final ValueMap dataProps = dataResource.adaptTo(ValueMap.class);

                        if (dataProps != null) {
                            final JsonObject json = new JsonObject();

                            json.addProperty(TEXT, dataProps.get(TEXT, ""));
                            json.addProperty(VALUE, dataProps.get(VALUE, ""));

                            jsonArray.add(json);
                        }
                    }
                }
            }
        }

        slingResponse.setContentType("application/json; charset=UTF-8");
        slingResponse.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();
        gson.toJson(jsonArray, slingResponse.getWriter());
    }
}
