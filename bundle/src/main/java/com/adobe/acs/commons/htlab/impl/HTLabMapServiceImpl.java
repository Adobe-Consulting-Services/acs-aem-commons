/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.htlab.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.adobe.acs.commons.htlab.HTLabFunction;
import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabMapService;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import com.adobe.acs.commons.htlab.use.MapUse;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central service coordinating map function implementations for the {@link MapUse} class.
 */
@Component(specVersion = "1.2")
@Service
public class HTLabMapServiceImpl implements HTLabMapService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTLabMapServiceImpl.class);
    private static final Pattern PATTERN_FN_NAME = Pattern.compile(HTLabFunction.REGEX_FN_NAME);
    private static final Comparator<Map.Entry<UseFunctionKey, HTLabFunction>> ENTRY_COMPARATOR =
            new Comparator<Map.Entry<UseFunctionKey, HTLabFunction>>() {
                @Override
                public int compare(Map.Entry<UseFunctionKey, HTLabFunction> o1,
                                   Map.Entry<UseFunctionKey, HTLabFunction> o2) {
                    return o1.getKey().getComparable().compareTo(o2.getKey().getComparable());
                }
            };

    @Reference(
            name = "useFunction",
            referenceInterface = HTLabFunction.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private Map<UseFunctionKey, HTLabFunction> useFunctions = new ConcurrentHashMap<UseFunctionKey, HTLabFunction>();

    private Map<String, HTLabFunction> useFunctionMap = Collections.emptyMap();

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context,
                                @Nonnull String fnName,
                                @Nonnull String key,
                                @CheckForNull Object value) {
        final Logger log = getBestLoggerForContext(context);
        log.trace("[service.apply] fnName={}, value={}", fnName, value);
        if (this.useFunctionMap.containsKey(fnName)) {
            HTLabFunction func = this.useFunctionMap.get(fnName);
            try {
                return func.apply(context, key, value).withFnName(fnName);
            } catch (Exception e) {
                log.warn("[service.apply] Error thrown by HTLabFunction.apply(). fnName={}, key={}, value={}, message={}",
                        new Object[] {fnName, key, value, e.getMessage()});
                if (log.isDebugEnabled()) {
                    log.debug("Caused by:", e);
                }

                return HTLabMapResult.failure(e).withFnName(fnName);
            }
        } else {
            log.info("[service.apply] No HTLabFunction found with {} of '{}'", HTLabFunction.OSGI_FN_NAME, fnName);
        }
        return HTLabMapResult.forwardValue();
    }

    private Logger getBestLoggerForContext(HTLabContext context) {
        if (context.getLog() != null) {
            return context.getLog();
        }
        return LOGGER;
    }

    protected final void bindUseFunction(HTLabFunction HTLabFunction, Map<String, ?> props) {
        if (HTLabFunction != null) {
            UseFunctionKey key = getKeyForUseFunction(HTLabFunction, props);
            if (key != null) {
                if (useFunctions.containsKey(key)) {
                    LOGGER.warn("[bindUseFunction] Failed to bind HTLabFunction {}: Service ID already bound.",
                            key.getServiceId());
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("[bindUseFunction] Binding HTLabFunction {} with key {}",
                                new Object[]{HTLabFunction.getClass().getName(), key});
                    }
                    useFunctions.put(key, HTLabFunction);
                    this.rebuildUseFunctionMap();
                }
            }
        }
    }

    protected final void updatedUseFunction(HTLabFunction HTLabFunction, Map<String, ?> props) {
        if (HTLabFunction != null) {
            UseFunctionKey key = getKeyForUseFunction(HTLabFunction, props);
            if (key != null) {
                if (useFunctions.containsKey(key)) {
                    useFunctions.remove(key);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[updatedUseFunction] Updating HTLabFunction {} with key {}",
                            new Object[]{HTLabFunction.getClass().getName(), key});
                }
                useFunctions.put(key, HTLabFunction);
                this.rebuildUseFunctionMap();
            }
        }
    }

    protected final void unbindUseFunction(HTLabFunction HTLabFunction, Map<String, ?> props) {
        if (HTLabFunction != null) {
            UseFunctionKey key = getKeyForUseFunction(HTLabFunction, props);
            if (key != null) {
                if (!useFunctions.containsKey(key)) {
                    LOGGER.warn("[unbindUseFunction] Failed to unbind HTLabFunction {}: Service ID not yet bound.",
                            key.getServiceId());
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("[unbindUseFunction] Unbinding HTLabFunction {} with key {}",
                                new Object[]{HTLabFunction.getClass().getName(), key});
                    }
                    useFunctions.remove(key);
                    this.rebuildUseFunctionMap();
                }
            }
        }
    }

    private synchronized void rebuildUseFunctionMap() {
        Map<String, HTLabFunction> newFunctions = new HashMap<String, HTLabFunction>();
        List<Map.Entry<UseFunctionKey, HTLabFunction>> entries =
                new ArrayList<Map.Entry<UseFunctionKey, HTLabFunction>>(this.useFunctions.entrySet());
        Collections.sort(entries, ENTRY_COMPARATOR);
        for (Map.Entry<UseFunctionKey, HTLabFunction> entry : entries) {
            if (!newFunctions.containsKey(entry.getKey().getFnName())) {
                newFunctions.put(entry.getKey().getFnName(), entry.getValue());
            }
        }
        this.useFunctionMap = Collections.unmodifiableMap(newFunctions);
    }

    private static UseFunctionKey getKeyForUseFunction(HTLabFunction HTLabFunction, Map<String, ?> props) {
        Map<String, Object> concreteProps = new HashMap<String, Object>(props);
        final Long serviceId = PropertiesUtil.toLong(props.get(Constants.SERVICE_ID), -1L);
        final String fnName = PropertiesUtil.toString(props.get(HTLabFunction.OSGI_FN_NAME), null);
        final Comparable<Object> comparable = ServiceUtil.getComparableForServiceRanking(concreteProps, Order.DESCENDING);
        if (serviceId > -1L && fnName != null) {
            if (PATTERN_FN_NAME.matcher(fnName).matches()) {
                return new UseFunctionKey(serviceId, fnName, comparable);
            } else {
                LOGGER.warn("[getKeyForUseFunction] Failed to bind HTLabFunction {}: illegal value '{}' for {}.",
                        new Object[] { serviceId, fnName, HTLabFunction.OSGI_FN_NAME});
            }
        } else if (fnName == null) {
            LOGGER.warn("[getKeyForUseFunction] Failed to bind HTLabFunction {}: missing mandatory property '{}'.",
                    serviceId, HTLabFunction.OSGI_FN_NAME);
        } else {
            LOGGER.warn("[getKeyForUseFunction] Failed to bind HTLabFunction {}: missing mandatory property '{}'.",
                    HTLabFunction.getClass().getName(), Constants.SERVICE_ID);
        }
        return null;
    }

    private static final class UseFunctionKey {
        private Long serviceId;
        private String fnName;
        private Comparable<Object> comparable;

        UseFunctionKey(@Nonnull Long serviceId, @Nonnull String fnName, @Nonnull Comparable<Object> comparable) {
            this.serviceId = serviceId;
            this.fnName = fnName;
            this.comparable = comparable;
        }

        Long getServiceId() {
            return serviceId;
        }

        String getFnName() {
            return fnName;
        }

        Comparable<Object> getComparable() {
            return comparable;
        }

        @Override
        public int hashCode() {
            return serviceId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            UseFunctionKey other = (UseFunctionKey) obj;
            return this.serviceId.equals(other.serviceId);
        }

        @Override
        public String toString() {
            return "UseFunctionKey{" +
                    "serviceId=" + serviceId +
                    ", fnName='" + fnName + '\'' +
                    '}';
        }
    }
}
