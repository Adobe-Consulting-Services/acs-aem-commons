/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Removes archived process instances.
 */
public class ProcessCleanup extends ProcessDefinition {
    @FormField(
            name = "Age",
            description = "Minimum age (in days) to be considered for removal.",
            hint = "e.g. 7 will remove anything from a week ago or earlier",
            options = {"default=7"}
    )
    public int miniumumAge;

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.defineAction("Remove old instances", rr, this::performCleanupActivity);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        // No report, do nothing.
    }

    @Override
    public void init() throws RepositoryException {
        // No init needed, do nothing.
    }

    @SuppressWarnings("squid:S00112")
    private void performCleanupActivity(ActionManager manager) throws Exception {
        Calendar c = Calendar.getInstance();
        c.clear(Calendar.HOUR_OF_DAY);
        c.clear(Calendar.MINUTE);
        c.clear(Calendar.SECOND);
        c.add(Calendar.DATE, -miniumumAge); // minus 1 day
        manager.withResolver(rr->{
            TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
            visitor.setLeafVisitor((res,level)->{
                if (isCreatedDateBefore(res, c)) {
                    String path = res.getPath();
                    manager.deferredWithResolver(rr2->{
                        rr2.delete(rr2.getResource(path));
                    });
                }
            });
            visitor.accept(rr.getResource(ProcessInstanceImpl.BASE_PATH));
        });
    }

    private boolean isCreatedDateBefore(Resource res, Calendar threshold) {
        Date created = res.getValueMap().get(JcrConstants.JCR_CREATED, Date.class);
        Calendar createDate = Calendar.getInstance();
        createDate.setTime(created);
        return createDate.before(threshold);
    }
}
