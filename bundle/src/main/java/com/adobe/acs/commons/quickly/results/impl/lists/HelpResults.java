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

import com.adobe.acs.commons.quickly.results.Action;
import com.adobe.acs.commons.quickly.results.Result;

import java.util.ArrayList;
import java.util.List;

public class HelpResults extends AbstractAccessibleResults {

    public final List<Result> getResults() {
        List<Result> results = new ArrayList<Result>();

        /* Help : These should be dynamically created in the future */

        results.add(new Result.Builder("Quickly is in Beta")
                .description("Please report issues at the ACS AEM Commons GitHub site.")
                .action(new Action.Builder()
                        .uri("https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues")
                        .method(Action.Method.GET)
                        .target(Action.Target.BLANK)
                        .build())
                .build());

        results.add(new Result.Builder("help")
                .description("This menu, also available via the '?' command.")
                .action(new Action.Builder().method(Action.Method.NOOP).build())
                .build());

        results.add(new Result.Builder("back")
                .description("Displays a list of the AEM pages you previously visited.")
                .action(new Action.Builder().method(Action.Method.NOOP).build())
                .build());

        results.add(new Result.Builder("docs")
                .description("Search the Adobe AEM documentation.")
                .action(new Action.Builder().method(Action.Method.NOOP).build())
                .build());

        results.add(new Result.Builder("go")
                .description("Go directly to AEM Web UIs. Use 'go!' to open in new window.")
                .action(new Action.Builder().method(Action.Method.NOOP).build())
                .build());

        results.add(new Result.Builder("lastmod")
                .description("Search for the last modified Pages in AEM by user and date modified")
                .action(new Action.Builder().method(Action.Method.NOOP).build())
                .build());

        results.add(new Result.Builder("*")
                .description("Self managed favorites. "
                        + "'Add Favorite' adds the current page as a favorite. '* rm' removes favorite'd pages.")
                .action(new Action.Builder().method(Action.Method.NOOP).build())
                .build());

        return results;
    }
}
