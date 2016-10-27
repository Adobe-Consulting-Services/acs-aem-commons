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

        final List<String> customProperties = new ArrayList<String>();
        final List<String> groups = new ArrayList<String>();

        groupFilter = json.getString(GROUP_FILTER);

        JSONArray groupsJSON = json.getJSONArray(GROUPS);
        for (int i = 0; i < groupsJSON.length(); i++) {
            groups.add(groupsJSON.getString(i));
        }

        this.groups = groups.toArray(new String[groups.size()]);

        JSONArray customPropertiesJSON = json.getJSONArray(CUSTOM_PROPERTIES);
        for (int i = 0; i < customPropertiesJSON.length(); i++) {
            JSONObject tmp = customPropertiesJSON.getJSONObject(i);
            String relativePropertyPath = tmp.optString(RELATIVE_PROPERTY_PATH);
            if (StringUtils.isNotBlank(relativePropertyPath)) {
                customProperties.add(relativePropertyPath);
            }
        }

        this.customProperties = customProperties.toArray(new String[customProperties.size()]);
    }

    public String[] getCustomProperties() {
        return customProperties;
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
        return groups;
    }

    public String getGroupFilter() {
        return groupFilter;
    }

    public String toString() {
        return "GROUP FILTER: " + this.groupFilter +
                " -- " +
                "GROUPS: " + StringUtils.join(this.groups, ",") +
                " -- " +
                "CUSTOM PROPERTIES: " + StringUtils.join(this.customProperties, ",");
    }
}
