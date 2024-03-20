/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.util.impl;

import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ModeUtil;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    private static ServiceTracker<AdapterManager, AdapterManager> adapterManagerServiceTracker;

    /**
     * default logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    @SuppressWarnings("squid:S2696")
    public void start(BundleContext context) throws Exception {
        LOG.info("{} started",context.getBundle().getSymbolicName());
        ServiceReference<SlingSettingsService> ref  = context.getServiceReference(SlingSettingsService.class);
        SlingSettingsService service = context.getService(ref);
        try {
            ModeUtil.configure(service);
        } catch (ConfigurationException ex) {
            LOG.error("Unable to configure ModeUtil with Sling Settings.", ex);
        }
        context.ungetService(ref);
        adapterManagerServiceTracker = new ServiceTracker<>(context, AdapterManager.class, null);
        adapterManagerServiceTracker.open();
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        adapterManagerServiceTracker.close();
        LOG.info("{} stopped",context.getBundle().getSymbolicName());
    }

    public static AdapterManager getAdapterManager() {
        return adapterManagerServiceTracker != null ? adapterManagerServiceTracker.getService() : null;
    }

}