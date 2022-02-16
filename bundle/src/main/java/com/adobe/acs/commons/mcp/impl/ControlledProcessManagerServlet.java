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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for interacting with MCP.
 */
@SlingServlet(
        resourceTypes = "acs-commons/components/utilities/manage-controlled-processes",
        selectors = {"start", "list", "status", "halt", "haltAll", "purge"},
        methods = {"GET", "POST"},
        extensions = {"json"}
)
public class ControlledProcessManagerServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ControlledProcessManagerServlet.class);

    private static final Collection<Class> IGNORED_CLASSES = Arrays.asList(
            Logger.class,
            Resource.class,
            ResourceResolver.class,
            byte[].class
    );
    private static final Collection<String> IGNORED_PACKAGES = Arrays.asList(
            "java.io"
    );
    private static final List<String> IGNORED_SERVLET_INPUTS = Arrays.asList("definition", "description", "action");

    @Reference
    transient ControlledProcessManager manager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        Object result = null;
        try {
            String action = request.getRequestPathInfo().getSelectorString();
            if (action == null) {
                throw new ServletException("action not provided in url selector");
            }
            switch (action) {
                case "start":
                    result = doStartProcess(request);
                    break;
                case "list":
                    result = doProcessList();
                    break;
                case "status":
                    result = doProcessStatusCheck(request);
                    break;
                case "halt":
                    result = doHaltProcess(request);
                    break;
                case "haltAll":
                case "halt.all":
                case "halt-all":
                    result = doHaltAllProcesses();
                    break;
                case "purge":
                    result = doPurgeCompleted();
                    break;
                default:
                    throw new IllegalArgumentException("Action not understood.");
            }
        } catch (Exception ex) {
            result = "Exception occurred " + ex.getMessage();
            LOG.error(ex.getMessage() + " -- End of line.", ex);
        }
        String json = getGson().toJson(result); // #2749
        response.getWriter().write(json);
    }

    Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fa) {
                return (fa.hasModifier(Modifier.TRANSIENT) || fa.hasModifier(Modifier.VOLATILE));
            }

            @Override
            public boolean shouldSkipClass(Class<?> type) {
                return IGNORED_CLASSES.contains(type)
                        || (type.getPackage() != null
                        && IGNORED_PACKAGES.contains(type.getPackage().getName()));
            }
        });
        gsonBuilder.disableInnerClassSerialization();
        Gson gson = gsonBuilder.create();
        return gson;
    }

    private ProcessInstance doStartProcess(SlingHttpServletRequest request) throws RepositoryException, ReflectiveOperationException, DeserializeException {
        String def = request.getParameter("definition");
        String description = request.getParameter("description");
        ProcessDefinition definition = manager.findDefinitionByNameOrPath(def);
        ProcessInstance instance = manager.createManagedProcessInstance(definition, description);
        instance.init(request.getResourceResolver(), convertRequestMap(request.getRequestParameterMap()));
        instance.run(request.getResourceResolver());
        return instance;
    }

    private List<ProcessInstance> doProcessStatusCheck(SlingHttpServletRequest request) {
        ProcessInstance instance = getProcessFromRequest(request);
        if (instance == null) {
            return getProcessesFromRequest(request);
        } else {
            return Arrays.asList(instance);
        }
    }

    private Object doHaltProcess(SlingHttpServletRequest request) {
        ProcessInstance instance = getProcessFromRequest(request);
        if (instance != null) {
            instance.halt();
        }
        return instance;
    }

    @SuppressWarnings("squid:S1172")
    private boolean doHaltAllProcesses() {
        manager.haltActiveProcesses();
        return true;
    }

    @SuppressWarnings("squid:S1172")
    private boolean doPurgeCompleted() {
        manager.purgeCompletedProcesses();
        return true;
    }

    private ProcessInstance getProcessFromRequest(SlingHttpServletRequest request) {
        String id = request.getParameter("id");
        if (id != null) {
            return manager.getManagedProcessInstanceByIdentifier(id);
        } else {
            String path = request.getParameter("path");
            if (path != null) {
                return manager.getManagedProcessInstanceByPath(path);
            }
        }
        return null;
    }

    private List<ProcessInstance> getProcessesFromRequest(SlingHttpServletRequest request) {
        String[] ids = request.getParameterValues("ids");
        if (ids != null) {
            return Stream.of(ids).map(manager::getManagedProcessInstanceByIdentifier).collect(Collectors.toList());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    Map<String, Object> convertRequestMap(RequestParameterMap requestParameterMap) {
        return requestParameterMap.entrySet().stream()
                .filter(entry -> !IGNORED_SERVLET_INPUTS.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> {
                            final RequestParameter[] values = entry.getValue();

                            if (values.length == 1) {
                                if (values[0].getFileName() != null) {
                                    return values[0];
                                } else {
                                    return values[0].getString();
                                }
                            } else {
                                return Arrays.stream(values).map(RequestParameter::getString).collect(Collectors.toList());
                            }
                        }
                ));
    }

    private Collection<ProcessInstance> doProcessList() {
        ArrayList<ProcessInstance> processes = new ArrayList();
        processes.addAll(manager.getActiveProcesses());
        processes.addAll(manager.getInactiveProcesses());
        processes.sort(Comparator.comparing((ProcessInstance p) -> p.getInfo().getStartTime()).reversed());
        return processes;
    }
}
