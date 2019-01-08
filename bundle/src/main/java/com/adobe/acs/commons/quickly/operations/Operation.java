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

package com.adobe.acs.commons.quickly.operations;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.results.Result;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.util.Collection;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface Operation {

    final String PROP_CMD = "cmd";
    final String PROP_DESCRIPTION = "description";

    /**
     * Checks if the Operation should handle the Command/Request.
     *
     * @param slingRequest the Request object
     * @param cmd the Command
     * @return true is the Operation handles the Command
     */
    boolean accepts(SlingHttpServletRequest slingRequest, Command cmd);

    /**
     * Returns a list of Results.
     *
     * @param request the Request object
     * @param response the Response object
     * @param cmd the Command
     * @return list of Results
     * @throws JSONException
     */
    Collection<Result> getResults(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                  Command cmd);

    /**
     * Returns the command operation this Operation is registered for.
     *
     * @return the command operation
     */
    String getCmd();
}
