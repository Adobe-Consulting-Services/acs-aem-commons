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
import com.adobe.acs.commons.quickly.results.Action;

import java.util.ArrayList;
import java.util.List;

public final class TouchConsoleResults extends AbstractAccessibleResults {

    public final List<Result> getResults() {
        List<Result> results = new ArrayList<Result>();

         /* Touch UIs */

        results.add(new Result.Builder("projects")
                .description("Project administration")
                .path("/projects.html")
                .action(new Action.Builder().uri("/projects.html").build())
                .touch()
                .build());

        results.add(new Result.Builder("sites")
                .description("Web page administration")
                .path("/content")
                .action(new Action.Builder().uri("/sites.html/content").build())
                .touch()
                .build());

        results.add(new Result.Builder("dam")
                .description("DAM administration")
                .path("/content/dam")
                .action(new Action.Builder().uri("/assets.html/content/dam").build())
                .touch()
                .build());

        results.add(new Result.Builder("wf")
                .description("Workflow administration")
                .path("/libs/cq/workflow/content/console.html")
                .action(new Action.Builder().uri("/libs/cq/workflow/content/console.html").build())
                .touch()
                .build());

        results.add(new Result.Builder("inbox")
                .description("My Inbox")
                .path("/notifications.html")
                .action(new Action.Builder().uri("/notifications.html").build())
                .touch()
                .build());

        results.add(new Result.Builder("users")
                .description("User administration")
                .path("/libs/granite/security/content/useradmin.html")
                .action(new Action.Builder().uri("/libs/granite/security/content/useradmin.html").build())
                .touch()
                .build());

        results.add(new Result.Builder("groups")
                .description("Group administration")
                .path("/libs/granite/security/content/groupadmin.html")
                .action(new Action.Builder().uri("/libs/granite/security/content/groupadmin.html").build())
                .touch()
                .build());

        results.add(new Result.Builder("communities")
                .description("Communities administration")
                .path("/communities.html")
                .action(new Action.Builder().uri("/communities.html").build())
                .touch()
                .build());

        results.add(new Result.Builder("publications")
                .description("DPS administration")
                .path("/aem/publications.html/content/publications")
                .action(new Action.Builder().uri("/aem/publications.html/content/publications").build())

                .touch()
                .build());

        results.add(new Result.Builder("tools")
                .description("AEM Tools")
                .path("/miscadmin")
                .action(new Action.Builder().uri("/miscadmin").build())
                .touch()
                .build());

        results.add(new Result.Builder("tags")
                .description("Tag administration")
                .path("/tagging")
                .action(new Action.Builder().uri("/tagging").build())
                .touch()
                .build());

        return results;
    }
}
