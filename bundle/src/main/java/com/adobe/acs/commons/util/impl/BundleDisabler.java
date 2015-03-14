/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.util.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Component disabler service
 * <p>
 * In the AEM security checklist, there are some bundles which should be disabled in production environents.
 * Whilst these bundles can be manually stopped, this component will do that as part of a deployment.  It will also
 * ensure that if they are manually started once more, then they will be immediately stopped.
 */
@org.apache.felix.scr.annotations.Component(immediate = true, metatype = true,
        label = "ACS AEM Commons - OSGI Bundle Disabler", description = "Disables bundles by configuration",
        policy = ConfigurationPolicy.REQUIRE)
@Service()
@Property(name = "event.topics", value = { "org/osgi/framework/BundleEvent/STARTED" }, propertyPrivate = true)
public class BundleDisabler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(BundleDisabler.class);

    @Property(label = "Disabled bundles", description = "The symbolic names of the bundles you want to disable",
            cardinality = Integer.MAX_VALUE)
    private static final String DISABLED_BUNDLES = "bundles";

    private BundleContext bundleContext;

    private List<String> disabledBundles = Collections.emptyList();

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        this.bundleContext = componentContext.getBundleContext();
        this.disabledBundles = getDisabledBundles(properties);
        disableBundles();
    }

    @Override
    public void handleEvent(Event event) {
        // We don't care about the event, we just need iterate all configured bundles and try to disable them
        disableBundles();
    }

    private void disableBundles() {
        if (disabledBundles.isEmpty()) {
            log.info("No bundles specified. Consider specifying bundles or removing this service config");
            return;
        }

        log.trace("Disabling bundles {}", disabledBundles);
        for (Bundle bundle : bundleContext.getBundles()) {
            if (isOnBundleStopList(bundle)) {
                try {
                    disableBundle(bundle);
                } catch (BundleException be) {
                    log.error("Unable to stop bundle {}", bundle.getSymbolicName(), be);
                }
            }
        }
    }

    private void disableBundle(final Bundle bundle) throws BundleException {
        if (isBundleStoppable(bundle) && isNotOwnBundle(bundle)) {
            log.info("Bundle {} disabled by configuration (name={}) ",
                    bundle.getSymbolicName(), bundle.getBundleId());
            bundle.stop();
        }
    }

    private List<String> getDisabledBundles(final Map<String, Object> properties) {
        final String[] bundlesProperty = PropertiesUtil.toStringArray(properties.get(DISABLED_BUNDLES), new String[0]);
        return Arrays.asList(PropertiesUtil.toStringArray(bundlesProperty, new String[0]));
    }

    private boolean isOnBundleStopList(final Bundle bundle) {
        return disabledBundles.contains(bundle.getSymbolicName());
    }

    private boolean isBundleStoppable(final Bundle bundle) {
        return bundle.getState() != Bundle.UNINSTALLED;
    }

    private boolean isNotOwnBundle(final Bundle bundle) {
        return !bundle.equals(bundleContext.getBundle());
    }
}
