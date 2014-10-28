/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.quickly.results.impl.serializers;

import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.ResultSerializer;
import com.day.cq.wcm.api.AuthoringUIMode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

@Component(
        label = "ACS AEM Commons - Quickly - Open Result Serializer"
)
@Properties({
        @Property(
                name = ResultSerializer.PROP_TYPE,
                value = OpenResultSerializerImpl.TYPE,
                propertyPrivate = true
        )
})
@Service
public class OpenResultSerializerImpl extends AbstractResultSerializer implements ResultSerializer {

    public static final String TYPE = "OPEN";

    public JSONObject toJSON(final Result result, final ValueMap config) throws JSONException {
        final AuthoringUIMode authoringUIMode = config.get(AuthoringUIMode.class.getName(), AuthoringUIMode.TOUCH);

        final JSONObject json = super.toJSON(result, config);
        final JSONObject action = json.getJSONObject("action");

        if(authoringUIMode != null && AuthoringUIMode.CLASSIC.equals(authoringUIMode)) {
            // Classic
            action.put("uri", "/cf#" + json.get("path") + ".html");
        } else {
            // TouchUI
            action.put("uri", "/editor.html" + json.get("path") + ".html");
        }

        json.put("action", action);

        return json;
    }
}
