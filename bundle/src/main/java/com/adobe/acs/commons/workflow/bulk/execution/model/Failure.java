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

package com.adobe.acs.commons.workflow.bulk.execution.model;

import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Model(adaptables = Resource.class)
public class Failure {

    public static final String PN_PATH = "path";
    public static final String PN_PAYLOAD_PATH = "payloadPath";
    public static final String PN_FAILED_AT = "failedAt";

    @Inject
    @Optional
    private String path;

    @Inject
    private String payloadPath;

    @Inject
    private Calendar failedAt;

    public String getPath() {
        return StringUtils.removeStart(path, "-");
    }

    public String getPayloadPath() {
        return StringUtils.removeStart(payloadPath, "-");
    }

    public String getDereferencedPath() {
        return path;
    }

    public String getDereferencedPayloadPath() {
        return payloadPath;
    }

    public Calendar getFailedAt() {
        return failedAt;
    }

    public JsonObject toJSON() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss aaa");

        JsonObject json = new JsonObject();
        json.addProperty(PN_PATH, getPath());
        json.addProperty(PN_PAYLOAD_PATH, getPayloadPath());
        json.addProperty(PN_FAILED_AT, sdf.format(getFailedAt().getTime()));

        return json;
    }
}
