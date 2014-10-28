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

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.results.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public abstract class AbstractOperation implements Operation {
    private static final Logger log = LoggerFactory.getLogger(AbstractOperation.class);

    @Override
    public Collection<Result> getResults(final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response,
                                         final Command cmd) throws JSONException {

        if (StringUtils.isBlank(cmd.getParam())) {
            return this.withoutParams(request, response, cmd);
        } else {
            return this.withParams(request, response, cmd);
        }
    }

    protected abstract List<Result> withoutParams(final SlingHttpServletRequest request,
                                                  final SlingHttpServletResponse response,
                                                  final Command cmd);

    protected abstract List<Result> withParams(final SlingHttpServletRequest slingRequest,
                                               final SlingHttpServletResponse response,
                                               final Command cmd);
}
