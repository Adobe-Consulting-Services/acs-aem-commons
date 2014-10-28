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

package com.adobe.acs.commons.quickly.results.impl.lists;

import com.adobe.acs.commons.quickly.results.Result;

import java.util.ArrayList;
import java.util.List;

public class ClassicConsoleResults extends AbstractAccessibleResults {


    public final List<Result> getResults() {
        List<Result> results = new ArrayList<Result>();

        /* Classic UIs */

        results.add(new Result.Builder("sites")
                .description("Web page administration")
                .path("/siteadmin")
                .actionURI("/siteadmin")
                .classic()
                .build());

        results.add(new Result.Builder("dam")
                .description("DAM administration")
                .path("/damadmin")
                .actionURI("/damadmin")
                .classic()
                .build());

        results.add(new Result.Builder("wf")
                .description("Workflow administration")
                .path("/libs/cq/workflow/content/console.html")
                .actionURI("/libs/cq/workflow/content/console.html")
                .classic()
                .build());

        results.add(new Result.Builder("tools")
                .description("AEM Tools")
                .path("/miscadmin")
                .actionURI("/miscadmin")
                .classic()
                .build());

        results.add(new Result.Builder("inbox")
                .description("My Inbox")
                .path("/inbox")
                .actionURI("/inbox")
                .classic()
                .build());

        results.add(new Result.Builder("users")
                .description("User and Group administration")
                .path("/useradmin")
                .actionURI("/useradmin")
                .classic()
                .build());

        results.add(new Result.Builder("campaigns")
                .description("Campaign administration")
                .path("/mcmadmin")
                .actionURI("/mcmadmin")
                .classic()
                .build());

        results.add(new Result.Builder("soco")
                .description("Soco administration")
                .path("/socoadmin")
                .actionURI("/socoadmin")
                .classic()
                .build());

        results.add(new Result.Builder("publications")
                .description("DPS administration")
                .path("/publishingadmin")
                .actionURI("/publishingadmin")
                .classic()
                .build());

        results.add(new Result.Builder("manuscripts")
                .description("Manuscript administration")
                .path("/manuscriptsadmin")
                .actionURI("/manuscriptsadmin")
                .classic()
                .build());

        results.add(new Result.Builder("tools")
                .description("AEM Tools")
                .path("/miscadmin")
                .actionURI("/miscadmin")
                .classic()
                .build());

        results.add(new Result.Builder("tags")
                .description("Tag administration")
                .path("/tagging")
                .actionURI("/tagging")
                .classic()
                .build());

        return results;
    }

}
