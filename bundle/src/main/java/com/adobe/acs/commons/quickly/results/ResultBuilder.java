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
import com.adobe.acs.commons.quickly.Command;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ValueMap;

@ProviderType
public interface ResultBuilder {

    /**
     * Turns the Result combination into a JSON object by way of ResultSerializers.
     *
     * @param cmd the Command
     * @param result the Result
     * @param config configuration used by ResultSerializer implementations
     * @return the JSON representation of the result
     * @throws JSONException
     */
    JsonObject toJSON(Command cmd, Result result, ValueMap config);
}
