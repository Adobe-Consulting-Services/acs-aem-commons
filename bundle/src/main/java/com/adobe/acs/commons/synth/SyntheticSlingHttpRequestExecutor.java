/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.synth;

import org.apache.sling.api.SlingHttpServletRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Synthetic Request Executor
 * <p>
 * Defines an interface for getting server side rendered output of a {@link SlingHttpServletRequest}.
 * Can be used in combination with synthetic sling request builder to generate output of a resource (path).
 * As of now, the request dispatcher is not supported
 * </p>
 */
public interface SyntheticSlingHttpRequestExecutor {

    /**
     * Returns the HTML string for the given <code>resource</code>.
     * Funnels the request through the actual SlingRequestProcessor as REQUEST.
     *
     * @param syntheticRequest request that will be executed
     * @return response as String object
     * @throws ServletException
     * @throws IOException
     */
    String execute(SlingHttpServletRequest syntheticRequest) throws ServletException, IOException;

}
