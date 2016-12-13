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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.script.Bindings;

import aQute.bnd.annotation.ConsumerType;
import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabFunction;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts input values to the specified adapter type.
 * Initializer fails if adapterType is unspecified or on class loader exception.
 */
@ConsumerType
public class AdaptToUseFn implements Use, HTLabFunction {
    private static final Logger LOG = LoggerFactory.getLogger(AdaptToUseFn.class);

    private static final String P_ADAPTER_TYPE = "type";

    private HTLabContext context;
    private Class<?> adapterType;

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull String key, @CheckForNull Object value) {
        if (this.adapterType == null) {
            return HTLabMapResult.failure();
        }

        if (value instanceof Adaptable) {
            Object adapted = ((Adaptable) value).adaptTo(this.adapterType);
            if (getLog().isDebugEnabled()) {
                getLog().debug("[AdaptToUseFn.apply] key={}, value={}, adapted={}",
                        new Object[]{key, value, adapted});
            }
            return HTLabMapResult.success(adapted);
        }

        return HTLabMapResult.forwardValue();
    }

    @Override
    public void init(Bindings bindings) {
        this.context = HTLabContext.fromBindings(bindings);

        String adapterTypeName = this.context.get(P_ADAPTER_TYPE, String.class);
        if (adapterTypeName == null || adapterTypeName.isEmpty()) {
            throw new IllegalArgumentException("Illegal value for " + P_ADAPTER_TYPE);
        }
        SlingScriptHelper sling = context.getSling();
        if (sling != null) {
            DynamicClassLoaderManager dclm = sling.getService(DynamicClassLoaderManager.class);
            if (dclm != null) {
                try {
                    this.adapterType = dclm.getDynamicClassLoader().loadClass(adapterTypeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Unable to retrieve adapter type class " + adapterTypeName, e);
                }
            } else {
                throw new IllegalStateException("Unable to get DynamicClassLoader service.");
            }
        } else {
            throw new IllegalStateException("Unable to retrieve SlingScriptHelper from bindings.");
        }
    }

    private Logger getLog() {
        return this.context.getLog() != null ? this.context.getLog() : LOG;
    }
}
