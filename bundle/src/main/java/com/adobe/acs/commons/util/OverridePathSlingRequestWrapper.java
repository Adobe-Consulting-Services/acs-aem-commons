/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.util;

import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Request wrapper to get the Resource and SlingBindings objects for a request scoped to a specified path.
 *
 * Among other things, this enables injection of sling models from a SlingHttpServletRequest
 * object rather than simply a Resource, which is required by some sling models for full
 * functionality.
 */
public class OverridePathSlingRequestWrapper extends SlingHttpServletRequestWrapper {
    private static final String ATTR_SLING_BINDINGS = SlingBindings.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger(OverridePathSlingRequestWrapper.class);

    private final SlingBindings myBindings = new SlingBindings();
    private final Resource resource;

    private final AdapterManager adapterManager;
    private final Map<Class<?>, Object> adaptersCache = new HashMap<>();

    /**
     * Constructor.
     *
     * @param request A valid sling request.
     * @param path    Path to represent by this request wrapper.
     */
    public OverridePathSlingRequestWrapper(final SlingHttpServletRequest request, final String path) {
        super(request);

        SlingBindings slingBindings = (SlingBindings) getSlingRequest().getAttribute(ATTR_SLING_BINDINGS);

        this.adapterManager = getAdapterManager(slingBindings);

        // Using `resolve` instead of `getResource` in order to support requests to non-existent resources
        this.resource = getSlingRequest().getResourceResolver().resolve(getSlingRequest(), path);
        if(slingBindings != null){
            this.myBindings.putAll(slingBindings);
        }
        this.myBindings.put(WCMBindings.PROPERTIES, this.resource.getValueMap());
        this.myBindings.put(SlingBindings.RESOURCE, this.resource);
        this.myBindings.put(SlingBindings.REQUEST, this);

        Page currentPage = null;
        PageManager pageManager = getSlingRequest().getResourceResolver().adaptTo(PageManager.class);
        if (pageManager != null) {
            currentPage = pageManager.getContainingPage(this.resource);
        }
        this.myBindings.put(WCMBindings.CURRENT_PAGE, currentPage);
    }

    /**
     * Gets the getAdapterManager from slingBindings.getSling() or the current bundle context.
     * Return null if something goes wrong.
     *
     * @param slingBindings the slingBindings to get Sling Services
     * @return        the AdapterManager from slingBindings.getSling() or the current bundle context
     */
    public static AdapterManager getAdapterManager(SlingBindings slingBindings) {
        if (slingBindings != null && slingBindings.getSling() != null) {
            return slingBindings.getSling().getService(AdapterManager.class);
        } else {
            BundleContext bundleContext = FrameworkUtil.getBundle(AdapterManager.class).getBundleContext();
            return (AdapterManager) bundleContext.getService(bundleContext.getServiceReference(AdapterManager.class.getName()));
        }
    }

    @Override
    public Object getAttribute(final String name) {
        if (ATTR_SLING_BINDINGS.equals(name)) {
            return this.myBindings;
        } else {
            return super.getAttribute(name);
        }
    }

    @Override
    public Resource getResource() {
        return this.resource;
    }

    /**
     * Overriding `adaptTo` to avoid using the original request as the adaptable.
     */
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType result;
        synchronized(this) {
            result = (AdapterType) this.adaptersCache.get(type);

            if (result == null) {
                AdapterManager mgr = this.adapterManager;
                if (mgr == null) {
                    LOG.warn("Unable to adapt request for path {} to {} because AdapterManager is null", this.resource.getPath(), type);
                } else {
                    result = mgr.getAdapter(this, type);
                }
                if (result != null) {
                    this.adaptersCache.put(type, result);
                }
            }

            return result;
        }
    }
}
