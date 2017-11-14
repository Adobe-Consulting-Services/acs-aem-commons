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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.adobe.acs.commons.exporters.impl.users.Constants.*;

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

    public Parameters(SlingHttpServletRequest request) throws IOException, JSONException {
        final JSONObject json = new JSONObject(request.getParameter("params"));

        final List<String> tmpCustomProperties = new ArrayList<String>();
        final List<String> tmpGroups = new ArrayList<String>();

        groupFilter = json.getString(GROUP_FILTER);

        JSONArray groupsJSON = json.getJSONArray(GROUPS);
        for (int i = 0; i < groupsJSON.length(); i++) {
            tmpGroups.add(groupsJSON.getString(i));
        }

        this.groups = tmpGroups.toArray(new String[tmpGroups.size()]);

        JSONArray customPropertiesJSON = json.getJSONArray(CUSTOM_PROPERTIES);
        for (int i = 0; i < customPropertiesJSON.length(); i++) {
            JSONObject tmp = customPropertiesJSON.getJSONObject(i);
            String relativePropertyPath = tmp.optString(RELATIVE_PROPERTY_PATH);
            if (StringUtils.isNotBlank(relativePropertyPath)) {
                tmpCustomProperties.add(relativePropertyPath);
            }
        }

        this.customProperties = tmpCustomProperties.toArray(new String[tmpCustomProperties.size()]);
    }

    public String[] getCustomProperties() {
        return Arrays.copyOf(customProperties, customProperties.length);
    }

    public JSONArray getCustomPropertiesAsJSON() throws JSONException {
        final JSONArray jsonArray = new JSONArray();

        for (String customProperty : customProperties) {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(RELATIVE_PROPERTY_PATH, customProperty);
            jsonArray.put(jsonObject);
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
