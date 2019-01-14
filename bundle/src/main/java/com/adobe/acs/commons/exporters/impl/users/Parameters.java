/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.exporters.impl.users;

import static com.adobe.acs.commons.exporters.impl.users.Constants.CUSTOM_PROPERTIES;
import static com.adobe.acs.commons.exporters.impl.users.Constants.GROUPS;
import static com.adobe.acs.commons.exporters.impl.users.Constants.GROUP_FILTER;
import static com.adobe.acs.commons.exporters.impl.users.Constants.GROUP_FILTER_BOTH;
import static com.adobe.acs.commons.exporters.impl.users.Constants.RELATIVE_PROPERTY_PATH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.adobe.acs.commons.json.JsonObjectUtil.*;

public class Parameters {
    private String[] customProperties;
    private String[] groups;
    private String groupFilter;

    public Parameters(Resource resource) {
        final ValueMap properties = resource.getValueMap();
        customProperties = properties.get(CUSTOM_PROPERTIES, new String[]{});
        groups = properties.get(GROUPS, new String[]{});
        groupFilter = properties.get(GROUP_FILTER, GROUP_FILTER_BOTH);
    }

    public Parameters(SlingHttpServletRequest request) throws IOException {
        final JsonObject json = new JsonParser().parse(request.getParameter("params")).getAsJsonObject();

        final List<String> tmpCustomProperties = new ArrayList<String>();
        final List<String> tmpGroups = new ArrayList<String>();

        groupFilter = getString(json, GROUP_FILTER);

        JsonArray groupsJSON = json.getAsJsonArray(GROUPS);
        for (int i = 0; i < groupsJSON.size(); i++) {
            tmpGroups.add(groupsJSON.get(i).getAsString());
        }

        this.groups = tmpGroups.toArray(new String[tmpGroups.size()]);

        JsonArray customPropertiesJSON = json.getAsJsonArray(CUSTOM_PROPERTIES);
        for (int i = 0; i < customPropertiesJSON.size(); i++) {
            JsonObject tmp = customPropertiesJSON.get(i).getAsJsonObject();

            if (tmp.has(RELATIVE_PROPERTY_PATH)) {
                String relativePropertyPath = getString(tmp, RELATIVE_PROPERTY_PATH);
                tmpCustomProperties.add(relativePropertyPath);
            }
        }

        this.customProperties = tmpCustomProperties.toArray(new String[tmpCustomProperties.size()]);
    }

    public String[] getCustomProperties() {
        return Arrays.copyOf(customProperties, customProperties.length);
    }

    protected JsonArray getCustomPropertiesAsJSON() {
        final JsonArray jsonArray = new JsonArray();

        for (String customProperty : customProperties) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(RELATIVE_PROPERTY_PATH, customProperty);
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    public String[] getGroups() {
        return Arrays.copyOf(groups, groups.length);
    }

    public String getGroupFilter() {
        return groupFilter;
    }
}
