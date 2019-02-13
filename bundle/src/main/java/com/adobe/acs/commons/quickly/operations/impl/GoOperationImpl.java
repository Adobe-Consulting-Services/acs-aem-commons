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

package com.adobe.acs.commons.quickly.operations.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.operations.AbstractOperation;
import com.adobe.acs.commons.quickly.operations.Operation;
import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.Action;
import com.adobe.acs.commons.quickly.results.impl.lists.ACSToolsResults;
import com.adobe.acs.commons.quickly.results.impl.lists.ClassicConsoleResults;
import com.adobe.acs.commons.quickly.results.impl.lists.DevConsoleResults;
import com.adobe.acs.commons.quickly.results.impl.lists.OpsConsoleResults;
import com.adobe.acs.commons.quickly.results.impl.lists.TouchConsoleResults;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * ACS AEM Commons - Quickly - Go Operation
 */
@Component
@Properties({
        @Property(
                name = Operation.PROP_CMD,
                value = GoOperationImpl.CMD
        ),
        @Property(
                name = Operation.PROP_DESCRIPTION,
                value = "Go straight to specific consoles in AEM"
        )
})
@Service
public class GoOperationImpl extends AbstractOperation {
    public static final String CMD = "go";

    @Override
    public boolean accepts(final SlingHttpServletRequest request,
                           final Command cmd) {

        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    public String getCmd() {
        return CMD;
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response,
                                         final Command cmd) {
        return this.buildShortcuts(request.getResourceResolver());
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest request,
                                      final SlingHttpServletResponse response,
                                      final Command cmd) {

        /* Look for shortcuts first. Add directly to results to maintain their initial order */

        final List<Result> results = new ArrayList<Result>();

        for (final Result shortcut : this.buildShortcuts(request.getResourceResolver())) {
            if (StringUtils.containsIgnoreCase(shortcut.getTitle(), cmd.getParam())) {
                results.add(shortcut);
            }
        }

        return results;
    }

    /**
     * Build the manually curated shortcuts lists. This must be built every time due to permissions checking
     *
     * @param resourceResolver
     * @return
     */
    private List<Result> buildShortcuts(final ResourceResolver resourceResolver) {
        final List<Result> shortcuts = new ArrayList<Result>();

        shortcuts.addAll(new ClassicConsoleResults().getResults(resourceResolver));
        shortcuts.addAll(new TouchConsoleResults().getResults(resourceResolver));
        shortcuts.addAll(DevConsoleResults.getResults());
        shortcuts.addAll(new OpsConsoleResults().getResults());
        shortcuts.addAll(new ACSToolsResults().getResults(resourceResolver));

        /* Switching Authoring UI Modes */

        shortcuts.add(new Result.Builder("touch")
                .description("Switch to Touch UI")
                .action(new Action.Builder()
                    .script("document.cookie='cq-authoring-mode=TOUCH;path=/;'")
                    .build())
                .classic()
                .build());

        shortcuts.add(new Result.Builder("classic")
                .description("Switch to Classic UI")
                .action(new Action.Builder()
                        .script("document.cookie='cq-authoring-mode=CLASSIC;path=/;'")
                        .build())
                .classic()
                .touch()
                .build());

        return shortcuts;
    }
}