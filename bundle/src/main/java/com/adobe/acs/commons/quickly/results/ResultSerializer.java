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

package com.adobe.acs.commons.quickly.results;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface ResultSerializer {
    String PROP_TYPE = "type";

    /**
     * Turns a Result object into a JSON object
     *
     * @param result the Result
     * @param config configuration used by ResultSerializer implementations
     * @return the JSON representation of the result
     * @throws JSONException
     */
    JSONObject toJSON(Result result, ValueMap config) throws JSONException;

}
