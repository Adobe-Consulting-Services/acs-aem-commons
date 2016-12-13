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
package com.adobe.acs.commons.htlab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;

/**
 * Wraps a {@link Bindings} instance with accessor methods that are analogous to those of
 * {@link SlingBindings}
 */
public final class HTLabContext extends ValueMapDecorator {

    private final Map<String, Object> bindings;

    private HTLabContext(Map<String, Object> base) {
        super(base);
        this.bindings = Collections.unmodifiableMap(
                new HashMap<String, Object>(base));
    }

    /**
     * Create a new {@link Bindings} instance from the underlying map.
     * @return clone of original bindings
     */
    public Bindings cloneBindings() {
        Map<String, Object> cloned = new HashMap<String, Object>(bindings);
        return new SimpleBindings(cloned);
    }

    /**
     * Wrap a {@link Bindings} instance with a new {@link HTLabContext}.
     * @param bindings the input bindings provided to a {@link org.apache.sling.scripting.sightly.pojo.Use} class.
     * @return the new context object
     */
    public static HTLabContext fromBindings(Bindings bindings) {
        return new HTLabContext(bindings);
    }

    /**
     * Get the HTTP Request object.
     * @return the HTTP Request object
     */
    @CheckForNull public SlingHttpServletRequest getRequest() {
        return this.get(SlingBindings.REQUEST, SlingHttpServletRequest.class);
    }

    /**
     * Get the HTTP Response object.
     * @return the HTTP Response object
     */
    @CheckForNull public SlingHttpServletResponse getResponse() {
        return this.get(SlingBindings.RESPONSE, SlingHttpServletResponse.class);
    }

    /**
     * Get the Sling Script Helper.
     * @return the Sling Script Helper
     */
    @CheckForNull public SlingScriptHelper getSling() {
        return this.get(SlingBindings.SLING, SlingScriptHelper.class);
    }

    /**
     * Get the request {@link Resource}.
     * @return the request resource
     */
    @CheckForNull public Resource getResource() {
        return this.get(SlingBindings.RESOURCE, Resource.class);
    }

    /**
     * Get the request {@link ResourceResolver}.
     * @return the request resource resolver.
     */
    @CheckForNull public ResourceResolver getResolver() {
        return this.get(SlingBindings.RESOLVER, ResourceResolver.class);
    }

    /**
     * Get the request logger.
     * @return the request logger
     */
    @CheckForNull public Logger getLog() {
        return this.get(SlingBindings.LOG, Logger.class);
    }

    @Override
    public String toString() {
        return "HTLabContext{" +
                "bindings=" + bindings.keySet() +
                '}';
    }
}
