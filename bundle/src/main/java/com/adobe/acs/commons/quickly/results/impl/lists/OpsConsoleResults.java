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

public class OpsConsoleResults  {

    public final List<Result> getResults() {
        List<Result> results = new ArrayList<Result>();

        /* Ops */
        results.add(new Result.Builder("packmgr")
                .description("CRX package manager")
                .actionURI("/crx/packmgr/index.jsp")
                .build());

        results.add(new Result.Builder("system/bundles")
                .description("System Console > Bundles")
                .actionURI("/system/console/bundles")
                .build());

        results.add(new Result.Builder("system/components")
                .description("System Console > Components")
                .actionURI("/system/console/components")
                .build());

        results.add(new Result.Builder("system/configmgr")
                .description("System Console > Config manager")
                .actionURI("/system/console/configMgr")
                .build());

        results.add(new Result.Builder("system/jmx")
                .description("System Console > JMX")
                .actionURI("/system/console/jmx")
                .build());

        results.add(new Result.Builder("system/slinglog")
                .description("System Console > Logs")
                .actionURI("/system/console/slinglog")
                .build());

        return results;
    }

}
