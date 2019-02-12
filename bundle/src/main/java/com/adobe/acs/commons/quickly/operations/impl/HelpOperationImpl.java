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
import com.adobe.acs.commons.quickly.results.impl.lists.HelpResults;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * ACS AEM Commons - Quickly - Help Operation
 */
@Component
@Property(
    name = Operation.PROP_CMD,
    value = HelpOperationImpl.CMD
)
@Service
public class HelpOperationImpl extends AbstractOperation {
    public static final String CMD = "help";

    public static final String CMD_ALIAS = "?";

    private static List<Result> operations = new HelpResults().getResults();

    @Override
    public boolean accepts(final SlingHttpServletRequest request,
                           final Command cmd) {

        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp())
                || StringUtils.equalsIgnoreCase(CMD_ALIAS, cmd.getOp());
    }

    @Override
    public String getCmd() {
        return CMD;
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response,
                                         final Command cmd) {

        return new ArrayList<Result>(operations);
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest request,
                                      final SlingHttpServletResponse response,
                                      final Command cmd) {

        final List<Result> results = new ArrayList<Result>();

        for (final Result result : operations) {
            if (StringUtils.startsWithIgnoreCase(result.getTitle(), cmd.getParam())) {
                results.add(result);
            }
        }

        return results;
    }
}