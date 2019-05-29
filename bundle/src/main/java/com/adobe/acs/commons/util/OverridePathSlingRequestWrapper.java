/*
 * ***********************************************************************
 * HS2 SOLUTIONS CONFIDENTIAL
 * ___________________
 *
 * Copyright 2019 Bounteous
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property
 * of HS2 Solutions and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to HS2 Solutions
 * and its suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from HS2 Solutions.
 * ***********************************************************************
 */
package com.adobe.acs.commons.util;

import com.adobe.cq.sightly.WCMBindings;
import com.day.text.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

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
    private final String ATTR_SLING_BINDINGS = SlingBindings.class.getName();

    private final SlingBindings myBindings = new SlingBindings();
    private final Resource resource;
    private final String relPath;

    private final AdapterManager adapterManager;
    private final Map<Class<?>, Object> adaptersCache = new HashMap<>();

    /**
     * Constructor.
     *
     * @param wrappedRequest SlingHttpServletRequest
     * @param relPath        String
     */
    public OverridePathSlingRequestWrapper(final SlingHttpServletRequest wrappedRequest, final String relPath) {
        super(wrappedRequest);

        SlingBindings slingBindings = (SlingBindings) getSlingRequest().getAttribute(ATTR_SLING_BINDINGS);

        this.relPath = relPath;
        this.adapterManager = slingBindings.getSling().getService(AdapterManager.class);
        this.resource = getSlingRequest().getResourceResolver().resolve(getSlingRequest(),
                Text.fullFilePath(getSlingRequest().getRequestPathInfo().getResourcePath(), relPath));
        this.myBindings.putAll(slingBindings);
        this.myBindings.put(WCMBindings.PROPERTIES, this.resource.getValueMap());
        this.myBindings.put(SlingBindings.RESOURCE, this.resource);
        this.myBindings.put(SlingBindings.REQUEST, this);
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
        AdapterType result = null;
        synchronized(this) {
            result = (AdapterType) this.adaptersCache.get(type);

            if (result == null) {
                AdapterManager mgr = this.adapterManager;
                result = mgr == null ? null : mgr.getAdapter(this, type);
                if (result != null) {
                    this.adaptersCache.put(type, result);
                }
            }

            return result;
        }
    }
}
