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

package com.adobe.acs.commons.quickly.results.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.QuicklyEngine;
import com.adobe.acs.commons.quickly.results.Action;
import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.ResultBuilder;
import com.adobe.acs.commons.quickly.results.ResultSerializer;
import com.adobe.acs.commons.quickly.results.impl.serializers.GenericResultSerializerImpl;
import com.day.cq.wcm.api.AuthoringUIMode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACS AEM Commons - Quickly - Result Builder
 */
@Component
@Reference(
        name = "resultSerializers",
        referenceInterface = ResultSerializer.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
@Service
public class ResultBuilderImpl implements ResultBuilder {
    private static final Logger log = LoggerFactory.getLogger(ResultBuilderImpl.class);

    private Map<String, ResultSerializer> resultSerializers = new ConcurrentHashMap<String, ResultSerializer>();

    @Reference(target = "(" + ResultSerializer.PROP_TYPE + "=" + GenericResultSerializerImpl.TYPE + ")")
    private ResultSerializer defaultResultSerialize;

    @Override
    public JsonObject toJSON(final Command cmd, Result result, final ValueMap config) {

        if (!this.acceptsAuthoringUIMode(result,
                config.get(AuthoringUIMode.class.getName(), AuthoringUIMode.TOUCH))) {
            // Rejected by Authoring UI Mode
            log.debug("Rejected by Authoring UI Mode: {}", result.getTitle());
            return null;
        } else if (!this.acceptsQuicklyMode(result, Arrays.asList(config.get(QuicklyEngine.CONFIG_RESULTS,
                String[].class)))) {
            // Rejected by Quickly Mode
            log.debug("Rejected by Quickly Mode: {}", result.getTitle());
            return null;
        }

        // Apply punctuation rules
        result = this.processPunctuation(cmd.getPunctuation(), result);

        ResultSerializer serializer = null;

        if (StringUtils.isNotBlank(result.getResultType()) && resultSerializers.containsKey(result.getResultType())) {
            serializer = resultSerializers.get(result.getResultType());
        }

        if (serializer == null) {
            serializer = resultSerializers.get(GenericResultSerializerImpl.TYPE);
        }

        if (serializer != null) {
            log.trace("Serializing results using Result Serializer [ {} ] w/ [ {} ]", result.getResultType(), serializer.getClass().getSimpleName());
            return serializer.toJSON(result, config);
        } else {
            log.trace("Could not find Quickly Result Serializer for type [ {} ]", result.getResultType());

            if (defaultResultSerialize != null) {
                log.trace("Using Default Quickly Result Serializer for type [ {} ]", GenericResultSerializerImpl.TYPE);
                return defaultResultSerialize.toJSON(result, config);
            } else {
                log.error("Could not find Default Quickly Result Serializer of type [ {} ]",
                        GenericResultSerializerImpl.TYPE);
            }
        }

        return null;
    }

    @SuppressWarnings("checkstyle:abbreviationaswordinname")
    protected final boolean acceptsAuthoringUIMode(Result result, AuthoringUIMode authoringUIMode) {
        if (result.getAuthoringMode() == null) {
            // All Authoring Modes
            return true;
        } else if (result.getAuthoringMode().equals(authoringUIMode)) {
            return true;
        } else {
            return false;
        }
    }

    protected final boolean acceptsQuicklyMode(Result result, List<String> modes) {
        if (result.getModes().contains(Result.Mode.ANY)
                || CollectionUtils.isEmpty(result.getModes())) {
            return true;
        }

        for (Result.Mode mode : result.getModes()) {
            for (String resultMode : modes) {
                if (StringUtils.equalsIgnoreCase(mode.toString(), resultMode)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected final Result processPunctuation(final String[] punctuation, final Result result) {
        for (final String p : punctuation) {

            if ("!".equals(p) && (Action.Method.GET.equals(result.getAction().getMethod())
                        || Action.Method.POST.equals(result.getAction().getMethod()))) {

                result.getAction().setTarget(Action.Target.BLANK);
            }
        }

        return result;
    }

    @Deactivate
    protected final void deactivate(Map<String, String> map) {
        resultSerializers = new ConcurrentHashMap<String, ResultSerializer>();
    }

    // Bind
    protected final void bindResultSerializers(final ResultSerializer service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(ResultSerializer.PROP_TYPE), null);

        if (type != null) {
            resultSerializers.put(type.toUpperCase(), service);
            log.info("Collected Result Serializer [ {} ]", type.toUpperCase());
        }
    }

    // Unbind
    protected final void unbindResultSerializers(final ResultSerializer service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(ResultSerializer.PROP_TYPE), null);

        if (type != null) {
            resultSerializers.remove(type.toUpperCase());
            log.info("Discarded Result Serializer [ {} ]", type.toUpperCase());
        }
    }
}
