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

package com.adobe.acs.commons.quickly.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.QuicklyEngine;
import com.adobe.acs.commons.quickly.operations.Operation;
import com.adobe.acs.commons.quickly.operations.impl.GoOperationImpl;
import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.ResultBuilder;
import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.AuthoringUIModeService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = QuicklyEngine.class,
        reference = {
                @Reference(
                        name = "operations",
                        service = Operation.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.AT_LEAST_ONE
                )
        }
)
@Designate(ocd = QuicklyEngineImpl.Config.class)
public class QuicklyEngineImpl implements QuicklyEngine {
    private static final Logger log = LoggerFactory.getLogger(QuicklyEngineImpl.class);

    private static final String KEY_RESULTS = "results";

    private ValueMap config;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Quickly"
    )
    public @interface Config {
        @AttributeDefinition(name = "Result Modes",
                description = "Additive - options: [ dev ], [ blank is the baseline ]",
                cardinality = 100,
                defaultValue = {})
        String[] result_modes();

    }


    private static final String[] DEFAULT_RESULT_MODES = {};


    public static final String PROP_RESULT_MODES = "result.modes";

    @Reference
    private AuthoringUIModeService authoringUIModeService;

    @Reference(target = "(cmd=" + GoOperationImpl.CMD + ")")
    private Operation defaultOperation;

    @Reference
    private ResultBuilder resultBuilder;

    private Map<String, Operation> operations = new ConcurrentHashMap<String, Operation>();

    @Override
    public final JsonObject execute(final SlingHttpServletRequest request, SlingHttpServletResponse response,
                                    final Command cmd) {

        for (final Operation operation : operations.values()) {
            if (operation.accepts(request, cmd)) {
                return this.getJSONResults(cmd, request, operation.getResults(request, response, cmd));
            }
        }

        /* Default Command */

        final Command defaultCmd = new Command(defaultOperation.getCmd() + " " + cmd.toString());
        return this.getJSONResults(cmd, request, defaultOperation.getResults(request, response, defaultCmd));
    }

    private JsonObject getJSONResults(Command cmd, SlingHttpServletRequest request, final Collection<Result> results) {
        final JsonObject json = new JsonObject();

        JsonArray resultArray = new JsonArray();
        json.add(KEY_RESULTS, resultArray);

        final ValueMap requestConfig = new ValueMapDecorator(new HashMap<String, Object>());

        // Collect all items collected from OSGi Properties
        requestConfig.putAll(this.config);

        // Add Request specific configurations
        requestConfig.put(AuthoringUIMode.class.getName(),
                authoringUIModeService.getAuthoringUIMode(request));

        for (final Result result : results) {
            final JsonObject tmp = resultBuilder.toJSON(cmd, result, requestConfig);

            if (tmp != null) {
                resultArray.add(tmp);
            }
        }

        return json;
    }

    protected final void bindOperations(final Operation service, final Map<Object, Object> props) {
        final String cmd = PropertiesUtil.toString(props.get(Operation.PROP_CMD), null);

        if (cmd != null) {
            log.debug("Collected Quickly Operation [ {} ]", cmd);
            operations.put(cmd, service);
        }
    }

    protected final void unbindOperations(final Operation service, final Map<Object, Object> props) {
        final String cmd = PropertiesUtil.toString(props.get(Operation.PROP_CMD), null);

        if (cmd != null) {
            log.debug("Discarded Quickly Operation [ {} ]", cmd);
            operations.remove(cmd);
        }
    }

    @Activate
    protected final void activate(Map<String, String> map) {
        config = new ValueMapDecorator(new HashMap<String, Object>());

        config.put(CONFIG_RESULTS,
                PropertiesUtil.toStringArray(map.get(PROP_RESULT_MODES), DEFAULT_RESULT_MODES));

    }
}
