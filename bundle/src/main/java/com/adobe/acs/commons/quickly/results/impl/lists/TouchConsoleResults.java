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

public final class TouchConsoleResults extends AbstractAccessibleResults {

    public final List<Result> getResults() {
        List<Result> results = new ArrayList<Result>();

         /* Touch UIs */

        results.add(new Result.Builder("projects")
                .description("Project administration")
                .path("/projects.html")
                .actionURI("/projects.html")
                .touch()
                .build());

        results.add(new Result.Builder("sites")
                .description("Web page administration")
                .path("/content")
                .actionURI("/sites.html/content")
                .touch()
                .build());

        results.add(new Result.Builder("dam")
                .description("DAM administration")
                .path("/content/dam")
                .actionURI("/assets.html/content/dam")
                .touch()
                .build());

        results.add(new Result.Builder("wf")
                .description("Workflow administration")
                .path("/libs/cq/workflow/content/console.html")
                .actionURI("/libs/cq/workflow/content/console.html")
                .classic()
                .build());

        results.add(new Result.Builder("inbox")
                .description("My Inbox")
                .path("/notifications.html")
                .actionURI("/notifications.html")
                .classic()
                .build());

        results.add(new Result.Builder("users")
                .description("User administration")
                .path("/libs/granite/security/content/useradmin.html")
                .actionURI("/libs/granite/security/content/useradmin.html")
                .touch()
                .build());

        results.add(new Result.Builder("groups")
                .description("Group administration")
                .path("/libs/granite/security/content/groupadmin.html")
                .actionURI("/libs/granite/security/content/groupadmin.html")
                .touch()
                .build());

        results.add(new Result.Builder("communities")
                .description("Communities administration")
                .path("/communities.html")
                .actionURI("/communities.html")
                .touch()
                .build());

        results.add(new Result.Builder("publications")
                .description("DPS administration")
                .path("/aem/publications.html/content/publications")
                .actionURI("/aem/publications.html/content/publications")
                .touch()
                .build());

        results.add(new Result.Builder("tools")
                .description("AEM Tools")
                .path("/miscadmin")
                .actionURI("/miscadmin")
                .touch()
                .build());

        results.add(new Result.Builder("tags")
                .description("Tag administration")
                .path("/tagging")
                .actionURI("/tagging")
                .touch()
                .build());

        return results;
    }
}
