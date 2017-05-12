/*
 * Copyright 2017 Adobe.
 *
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
 */
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for interacting with MCP.
 */
@SlingServlet(paths = "/bin/mcp")
public class ControlledProcessManagerServlet extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ControlledProcessManagerServlet.class);

    @Reference
    ControlledProcessManager manager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        Object result = null;
        try {
            switch (action) {
                case "start":
                    result = doStartProcess(request);
                    break;
                case "status":
                    result = doProcessStatusCheck(request);
                    break;
                case "halt":
                    result = doHaltProcess(request);
                    break;
                case "haltAll":
                    result = doHaltAllProcesses(request);
                    break;
                case "purge":
                    result = doPurgeCompleted(request);
                    break;
                default:
                    throw new Exception("Action not understood.");
            }
        } catch (Exception ex) {
            result = "Exception occurred "+ex.getMessage();
            LOG.error(ex.getMessage() + " -- End of line.", ex);
        }
        Gson gson = new Gson();
        gson.toJson(result, response.getWriter());
    }

    private ProcessInstance doStartProcess(SlingHttpServletRequest request) throws RepositoryException, ReflectiveOperationException {
        String def = request.getParameter("definition");
        String description = request.getParameter("description");
        ProcessDefinition definition = manager.findDefinitionByNameOrPath(def);
        ProcessInstance instance = manager.createManagedProcessInstance(definition, description);
        instance.init(request.getResourceResolver(), convertRequestMap(request.getParameterMap()));
        instance.run(request.getResourceResolver());
        return instance;
    }

    private ProcessInstance doProcessStatusCheck(SlingHttpServletRequest request) {
        ProcessInstance instance = getProcessFromRequest(request);
        return instance;
    }

    private Object doHaltProcess(SlingHttpServletRequest request) {
        ProcessInstance instance = getProcessFromRequest(request);
        instance.halt();
        return instance;
    }

    private boolean doHaltAllProcesses(SlingHttpServletRequest request) {
        manager.haltActiveProcesses();
        return true;
    }

    private boolean doPurgeCompleted(SlingHttpServletRequest request) {
        manager.purgeCompletedProcesses();
        return true;
    }

    private ProcessInstance getProcessFromRequest(SlingHttpServletRequest request) {
        String id = request.getParameter("id");
        if (id != null) {
            return manager.getManagedProcessInstanceByIdentifier(id);
        } else {
            String path = request.getParameter("path");
            return manager.getManagedProcessInstanceByPath(path);
        }
    }

    List<String> ignoredInputs = Arrays.asList("definition", "description", "action");

    private Map<String, Object> convertRequestMap(Map<String, String[]> parameterMap) {
        return parameterMap.entrySet().stream()
                .filter(entry -> !ignoredInputs.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> (Object) (entry.getValue().length == 1 ? entry.getValue()[0] : entry.getValue())
                ));
    }
}
