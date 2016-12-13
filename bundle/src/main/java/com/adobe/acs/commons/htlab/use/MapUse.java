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
package com.adobe.acs.commons.htlab.use;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.script.Bindings;

import aQute.bnd.annotation.ConsumerType;
import com.adobe.acs.commons.htlab.HTLabFunction;
import com.day.cq.wcm.api.Page;
import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabMapService;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "MapUse" is is the entry point for the HTLab DSL within an HTL script. It provides a more concise means of extending
 * HTL functionality than typing out a fully-qualified class name for every instance of a custom {@link Use} class.
 *
 * To initialize the DSL for the request resource, create an instance of this class using the data-sly-use attribute:
 *
 * {@code data-sly-use.resource_="com.adobe.acs.commons.htlab.use.MapUse"}
 *
 * This allows one to access values from an associated underlying property map and then dollar-pipe them to
 * {@link HTLabFunction}s, which can either be registered as OSGi services, or
 * implemented as {@link Use} classes. For example:
 *
 * "${resource_['jcr:created $ jsonDate']}" evaluates to an ISO8601-formatted string representing the Calendar value of
 * the jcr:created property.
 *
 * (see {@link com.adobe.acs.commons.htlab.impl.func.JsonDateFunction})
 *
 * If a function name is not mapped to an active {@link HTLabFunction}, the input value
 * is passed through and the function name logged, rather than throwing an exception.
 *
 *
 * INITIALIZER OPTIONS
 * -------------------
 *
 * WRAP OBJECT ("wrap=")
 *
 * To wrap an HTL variable, just specify {@code wrap=varName} in the {@code data-sly-use} options. For example,
 *
 * {@code data-sly-use.currentPage_="${'com.adobe.acs.commons.htlab.use.MapUse' @ wrap=currentPage}"}
 *
 * By convention, the MapUse variable should be named "var_" (i.e. "var" followed by underscore), where "var" is the name of
 * the variable that is wrapped.
 *
 * RESOURCE PATH ("path=")
 *
 * It is also possible to wrap a resource resolved from a path. Simply specify {@code path='./path/to/resource'}. The
 * path will be resolved relative to the request resource.
 *
 * PIPE OPERATOR ("pipe=")
 *
 * If the default dollar ("$") pipe operator conflicts with an existing property name in the wrapped map, you can
 * specify a different operator using {@code pipe='[op]'}.
 *
 * For example, initializing with
 * {@code data-sly-use.resI18n_="${'com.adobe.acs.commons.htlab.use.MapUse' @ pipe='%', wrap=resI18n}"} allows for property
 * access like so: "${resI18n_['Big Dishwasher Sale $ $ $ Marquee % truncateTo100']}", where "Big Dishwasher Sale $ $ $
 * Marquee" is interpreted as the property name.
 *
 *
 * USE FUNCTIONS
 * -------------
 *
 * HTLab functions can also be bound in the {@link MapUse} initializer so long as the assigned function names do not collide
 * with the argument names specified above (i.e., Use Functions cannot be bound as "wrap", "path", or "pipe").
 *
 * Specifically, the Use Function must be initialized before the {@link MapUse} instance, with a valid HTL use variable name:
 *
 * {@code data-sly-use.myUseFunc="com.adobe.acs.commons.htlab.use.ToStringUseFn"}
 *
 * Then, the use variable must be bound to the {@link MapUse} initializer as an option name=value pair:
 *
 * {@code data-sly-use.currentPage_="${'com.adobe.acs.commons.htlab.use.MapUse' @ toString=myUseFunc}"}
 *
 * The function can then be applied using the option name assigned in the {@link MapUse} initializer:
 *
 * "${currentPage_['cq:lastModified $ toString']}" evaluates to "java.util.GregorianCalendar[time=..."
 *
 * An Initializer-bound Use Function will always override an OSGi-registered function of the same name, regardless of
 * the latter's {@code service.ranking} value.
 *
 *
 * THE SELF SELECTOR
 * -----------------
 *
 * The target object of an {@link MapUse} instance can be selected instead of one of its properties by using the
 * pipe operator by itself (the "Self" selector) or followed by a function.
 *
 * "${resource_[' $ ']}" evaluates to the underlying Resource, whereas "{resource_[' $ pageLink']}" passes the Resource to
 * the {@link com.adobe.acs.commons.htlab.impl.func.PageLinkFunction} service which generates a link to the resourcePage
 * by mapping the containing-Page-path using the resolver and request context objects, and then appending ".html".
 *
 * HTLAB_USE SHORTCUT
 * ------------------
 *
 * The {@link com.adobe.acs.commons.htlab.impl.HTLabBindingsValuesProvider} adds the HTLAB_USE binding which is a
 * mapping between simple class names and fully-qualified class names for the classes under the .htlab.use package.
 *
 * For example,
 * 1. {@code data-sly-use.resource_="com.adobe.acs.commons.htlab.use.MapUse"} becomes
 * {@code data-sly-use.resource_="${HTLAB_USE.MapUse}"}
 * 2. {@code data-sly-use.myUseFunc="${'com.adobe.acs.commons.htlab.use.ToStringUseFn' @ onNull='n/a'}"} becomes
 * {@code data-sly-use.myUseFunc="${HTLAB_USE.ToStringUseFn @ onNull='n/a'}"}
 * 3. {@code data-sly-use.fnAdaptToRepStatus="${'com.adobe.acs.commons.htlab.use.AdaptToUseFn' @ type='com.day.cq.replication.ReplicationStatus'}"} becomes
 * {@code data-sly-use.myUseFunc="${HTLAB_USE.AdaptToUseFn @ type='com.day.cq.replication.ReplicationStatus'}"}
 *
 */
@ConsumerType
public final class MapUse implements Use, Map<String, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(MapUse.class);

    public static final String B_PATH = "path";
    public static final String B_WRAP = "wrap";
    public static final String B_PIPE = "pipe";

    private static final Set<String> RESERVED_OPTIONS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(B_PATH, B_WRAP, B_PIPE)));

    public static final String DEFAULT_PIPE = "$";

    private HTLabContext context;
    private String pipe;
    private String pipeLiteral;
    private Pattern pipePattern;

    private Object target;
    private Map<?, ?> originalMap;

    private Map<String, HTLabFunction> useFunctions = new HashMap<String, HTLabFunction>();

    private HTLabMapService mapService;
    private Map<String, Object> memoized = new HashMap<String, Object>();

    static class FunctionKey {
        private String normalizedKey;
        private String property;
        private String[] functions;

        FunctionKey(String normalizedKey, String property, String[] functions) {
            this.normalizedKey = normalizedKey;
            this.property = property;
            this.functions = functions;
        }

        String getNormalizedKey() {
            return normalizedKey;
        }

        String getProperty() {
            return property;
        }

        String[] getFunctions() {
            return functions;
        }
    }

    /**
     * Get configured pipe operator.
     * @return configured pipe operator
     */
    public String getPipe() {
        return pipe;
    }

    @Override
    public void init(Bindings bindings) {
        this.context = HTLabContext.fromBindings(bindings);
        this.configurePipeOperator();
        this.collectBoundUseFunctions();

        Object wrap = this.context.get(B_WRAP, Object.class);
        this.target = wrap;
        if (wrap instanceof Map) {
            getLog().debug("[MapUse.init] wrap= object is a Map. using it as '{}' and as map.", this.pipe);
            this.originalMap = (Map) wrap;
        } else if (wrap instanceof Page) {
            getLog().debug("[MapUse.init] wrap= object is Page. using it as '{}' and it.getProperties() as map.",
                    this.pipe);
            this.originalMap = ((Page) wrap).getProperties();
        } else if (wrap instanceof Adaptable) {
            getLog().debug("[MapUse.init] wrap= object is Adaptable. using it as '{}' and it.adaptTo(ValueMap.class) as map.",
                    pipe);
            ValueMap adapted = ((Adaptable) wrap).adaptTo(ValueMap.class);
            this.originalMap = adapted != null ? adapted : ValueMap.EMPTY;
        } else if (wrap == null) {
            getLog().debug("[MapUse.init] wrap= is null. will get Resource from request unless path= is defined.");
            Resource resource = this.context.getResource();
            String path = this.context.get(B_PATH, String.class);
            if (path != null) {
                getLog().debug("[MapUse.init] path= is defined. resolving resource from {}.", path);
                ResourceResolver resolver = this.context.getResolver();
                if (resolver != null) {
                    resource = resolver.getResource(resource, path);
                    if (resource == null) {
                        getLog().warn("[MapUse.init] Failed to get resource at path: {}. '{}' is null and map is empty.",
                                path, this.pipe);
                    }
                } else {
                    getLog().warn("[MapUse.init] Failed to get resource resolver.");
                }
            }

            if (resource != null) {
                this.target = resource;
                this.originalMap = resource.getValueMap();
            } else {
                getLog().warn("[MapUse.init] Failed to get resource. '{}' is null and map is empty.", this.pipe);
                this.originalMap = ValueMap.EMPTY;
            }
        } else {
            getLog().debug("[MapUse.init] wrap= object does not adapt to ValueMap. using it as '{}' but map is empty.",
                    this.pipe);
            this.originalMap = ValueMap.EMPTY;
        }


        SlingScriptHelper sling = this.context.getSling();
        if (sling != null) {
            this.mapService = sling.getService(HTLabMapService.class);
        } else {
            getLog().warn("[MapUse.init] Failed to get SlingScriptHelper ('sling') from bindings.");
        }

        if (this.mapService == null) {
            getLog().warn("[MapUse.init] Failed to get {} from sling. function names will be ignored.",
                    HTLabMapService.class.getSimpleName());
        }

        HashMap<String, Object> copyOfOriginal = new HashMap<String, Object>();
        for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
            copyOfOriginal.put(entry.getKey().toString(), entry.getValue());
        }
        this.memoized = new ValueMapDecorator(copyOfOriginal);

        // Memoize the non-null target this at the end of the init method to ensure that this.size() > 0 for
        // data-sly-test, because MapUse implements Map, and data-sly-test expects Map.size() > 0 for success.
        if (this.target != null) {
            this.memoized.put(this.pipeLiteral, this.target);
        }
    }

    private void configurePipeOperator() {
        this.pipe = this.context.get(B_PIPE, DEFAULT_PIPE);
        this.pipeLiteral = String.format(" %s ", this.pipe);
        this.pipePattern =
                Pattern.compile(String.format("\\s*%s(\\s*%s)*\\s*",
                        Pattern.quote(this.pipe), Pattern.quote(this.pipe)));
        getLog().debug("[MapUse.configurePipeOperator] pipe operator: {}", this.pipe);
    }

    private void collectBoundUseFunctions() {
        for (Map.Entry<String, Object> entry : this.context.entrySet()) {
            if (!RESERVED_OPTIONS.contains(entry.getKey())
                    && entry.getValue() instanceof HTLabFunction) {
                getLog().info("[MapUse.collectBoundUseFunctions] found key {} mapped to function class {} in Bindings.",
                        entry.getKey(), entry.getValue().getClass().getName());
                this.useFunctions.put(entry.getKey(), (HTLabFunction) entry.getValue());
            }
        }
    }

    private Logger getLog() {
        return LOG;
    }

    /**
     * Applies any map functions and returns the appropriate key to access the memoized value.
     * @param key original key
     * @return normalized key
     */
    private String applyMapFunctions(@Nonnull String key) {
        getLog().trace("[MapUse.applyMapFunctions] begin; key={}", key);
        if (hasFunctions(key)) {
            FunctionKey functionKey = parseFunctionKey(key);
            String normalizedKey = functionKey.getNormalizedKey();
            if (!this.memoized.containsKey(normalizedKey)) {
                Object value = functionKey.getProperty().isEmpty()
                        ? this.target
                        : this.originalMap.get(functionKey.getProperty());

                if (this.mapService != null) {
                    getLog().trace("[MapUse.applyMapFunctions] begin mapping; property={}, value={}",
                            functionKey.getProperty(), value);

                    HTLabMapResult result = HTLabMapResult.success(value);

                    for (String fnName : functionKey.getFunctions()) {
                        HTLabMapResult nextResult;

                        HTLabFunction useFunction = this.useFunctions.get(fnName);
                        if (useFunction != null) {
                            nextResult = useFunction.apply(this.context,
                                    functionKey.getProperty(), result.getValue()).withFnName(fnName);
                        } else {
                            nextResult = this.mapService.apply(this.context, fnName,
                                    functionKey.getProperty(), result.getValue());
                        }

                        result = result.combine(nextResult);

                        if (getLog().isTraceEnabled()) {
                            getLog().trace("[MapUse.applyMapFunctions] map result {}", result);
                        }

                        if (result.isFailure()) {
                            break;
                        }
                    }

                    if (result.isSuccess()) {
                        value = result.getValue();
                    } else {
                        getLog().error("[MapUse.applyMapFunctions] function application failed; map result {}", result);
                        if (result.getCause() != null) {
                            getLog().error("[MapUse.applyMapFunctions] cause:", result.getCause());
                        }
                        value = null;
                    }
                    getLog().trace("[MapUse.applyMapFunctions] end mapping; property={}, value={}",
                            functionKey.getProperty(), value);
                }
                this.put(normalizedKey, value);
            }
            getLog().trace("[MapUse.applyMapFunctions] end; normalizedKey={}", key);
            return normalizedKey;
        } else if (!this.memoized.containsKey(key)) {
            Object value = this.originalMap.get(key);
            this.memoized.put(key, value);
        }
        getLog().trace("[MapUse.applyMapFunctions] end; key={}", key);
        return key;
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String) {
            String keyString = (String) key;
            String normalizedKey = this.applyMapFunctions(keyString);
            if (this.getLog().isDebugEnabled()) {
                this.getLog().debug("[MapUse.get] key={}, normalizedKey={}, result={}",
                        new Object[] {key, normalizedKey, memoized.get(normalizedKey)});
            }
            return memoized.get(normalizedKey);
        } else {
            return this.originalMap.get(key);
        }
    }

    private boolean hasFunctions(String key) {
        return key.contains(this.pipe);
    }

    FunctionKey parseFunctionKey(@Nonnull String key) {
        Matcher dFuncMatcher = this.pipePattern.matcher(key);
        if (!dFuncMatcher.find()) {
            // key is not a function expression. return simple property accessor.
            return new FunctionKey(key, key, new String[0]);
        }

        // if the D_FUNC delim is the first
        final boolean isSelfSelected = dFuncMatcher.start() == 0;
        dFuncMatcher.reset();
        String[] parts = this.pipePattern.split(key);


        List<String> filtered = new ArrayList<String>();
        if (isSelfSelected) {
            filtered.add("");
        }

        for (String part : Arrays.asList(parts)) {
            if (!part.trim().isEmpty()) {
                filtered.add(part.trim());
            }
        }

        final String property = filtered.get(0);
        String[] functions = filtered.subList(1, filtered.size())
                .toArray(new String[filtered.size() - 1]);

        final String normalizedKey = property + this.pipeLiteral
                + StringUtils.join(functions, this.pipeLiteral);

        if (LOG.isDebugEnabled()) {
            LOG.debug("[MapUse.parseFunctionKey] key={} isSelfSelected={} filtered={}, normKey={}, property={}, functions={}",
                    new Object[]{key, isSelfSelected, filtered, normalizedKey, property, Arrays.asList(functions)});
        }

        return new FunctionKey(normalizedKey, property, functions);
    }

    @Override
    public int size() {
        return memoized.size();
    }

    @Override
    public boolean isEmpty() {
        return memoized.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return memoized.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return memoized.containsValue(value);
    }

    @Override
    public Object put(String key, Object value) {
        Map<String, Object> updated = new HashMap<String, Object>(this.memoized);
        Object ret = updated.put(key, value);
        this.memoized = new ValueMapDecorator(updated);
        return ret;
    }

    @Override
    public Object remove(Object key) {
        Map<String, Object> updated = new HashMap<String, Object>(this.memoized);
        Object removed = updated.remove(key);
        this.memoized = new ValueMapDecorator(updated);
        return removed;
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        Map<String, Object> updated = new HashMap<String, Object>(this.memoized);
        updated.putAll(map);
        this.memoized = new ValueMapDecorator(updated);
    }

    @Override
    public void clear() {
        Map<String, Object> updated = new HashMap<String, Object>();
        this.memoized = new ValueMapDecorator(updated);
    }

    @Override
    public Set<String> keySet() {
        return memoized.keySet();
    }

    @Override
    public Collection<Object> values() {
        return memoized.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return memoized.entrySet();
    }

    @Override
    public String toString() {
        return "MapUse{" +
                "context=" + context +
                ", pipe='" + pipe + '\'' +
                ", target=" + String.valueOf(target)+
                ", originalMap=" + originalMap.keySet() +
                ", useFunctions=" + useFunctions.keySet() +
                '}';
    }
}
