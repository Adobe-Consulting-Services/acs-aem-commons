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

public class ClassicConsoleResults extends AbstractAccessibleResults {


    public final List<Result> getResults() {
        List<Result> results = new ArrayList<Result>();

        /* Classic UIs */

        results.add(new Result.Builder("sites")
                .description("Web page administration")
                .path("/siteadmin")
                .action(new Action.Builder().uri("/siteadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("dam")
                .description("DAM administration")
                .path("/damadmin")
                .action(new Action.Builder().uri("/damadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("wf")
                .description("Workflow administration")
                .path("/libs/cq/workflow/content/console.html")
                .action(new Action.Builder().uri("/libs/cq/workflow/content/console.html").build())
                .classic()
                .build());

        results.add(new Result.Builder("inbox")
                .description("My Inbox")
                .path("/inbox")
                .action(new Action.Builder().uri("/inbox").build())
                .classic()
                .build());

        results.add(new Result.Builder("users")
                .description("User and Group administration")
                .path("/useradmin")
                .action(new Action.Builder().uri("/useradmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("campaigns")
                .description("Campaign administration")
                .path("/mcmadmin")
                .action(new Action.Builder().uri("/mcmadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("soco")
                .description("Soco administration")
                .path("/socoadmin")
                .action(new Action.Builder().uri("/socoadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("publications")
                .description("DPS administration")
                .path("/publishingadmin")
                .action(new Action.Builder().uri("/publishingadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("manuscripts")
                .description("Manuscript administration")
                .path("/manuscriptsadmin")
                .action(new Action.Builder().uri("/manuscriptsadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("tools")
                .description("AEM Tools")
                .path("/miscadmin")
                .action(new Action.Builder().uri("/miscadmin").build())
                .classic()
                .build());

        results.add(new Result.Builder("tags")
                .description("Tag administration")
                .path("/tagging")
                .action(new Action.Builder().uri("/tagging").build())
                .classic()
                .build());

        return results;
    }

}
