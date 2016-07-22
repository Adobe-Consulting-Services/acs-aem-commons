/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.workflow.synthetic.impl.granite;

import com.adobe.granite.workflow.exec.Route;
import com.adobe.granite.workflow.model.WorkflowTransition;

import java.util.ArrayList;
import java.util.List;

public class SyntheticRoute implements Route {

    private final boolean backRoute;

    public SyntheticRoute(boolean backRoute) {
        this.backRoute = backRoute;
    }

    @Override
    public final String getId() {
        return "synthetic-route";
    }

    @Override
    public final String getName() {
        return "Synthetic Route";
    }

    @Override
    public final boolean hasDefault() {
        return false;
    }

    @Override
    public final List<WorkflowTransition> getDestinations() {
        return new ArrayList<WorkflowTransition>();
    }

    @Override
    public final boolean isBackRoute() {
        return this.backRoute;
    }
}
