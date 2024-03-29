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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle disabler service
 * <p>
 * In the AEM security checklist, there are some bundles which should be disabled in production environments.
 * Whilst these bundles can be manually stopped, this component will do that as part of a deployment.  It will also
 * ensure that if they are manually started once more, then they will be immediately stopped.
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics(value = { "org/osgi/framework/BundleEvent/STARTED"} )
@Designate(ocd = BundleDisabler.Config.class)
public class BundleDisabler implements EventHandler {

    @ObjectClassDefinition(name = "ACS AEM Commons - OSGI Bundle Disabler", description = "Disables OSGi bundles by configuration")
    static @interface Config {
        @AttributeDefinition(name = "Disabled bundles", description = "The symbolic names of the bundles you want to disable", cardinality = Integer.MAX_VALUE)
        String[] bundles();
    }

    @Component(service = ConfigAmendment.class, property = "webconsole.configurationFactory.nameHint={bundles}")
    @Designate(factory = true, ocd = BundleDisabler.Config.class)
    public static final class ConfigAmendment {
        private final Config config;

        @Activate
        public ConfigAmendment(Config config) {
            this.config = config;
        }

        public Config getConfig() {
            return config;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(BundleDisabler.class);

    private final BundleContext bundleContext;

    private final Set<String> disabledBundles;

    @Activate
    public BundleDisabler(BundleContext bundleContext, Config config, @Reference(policyOption = ReferencePolicyOption.GREEDY) List<ConfigAmendment> configAmendments) {
        this.bundleContext = bundleContext;
        this.disabledBundles = new HashSet<>();
        this.disabledBundles.addAll(Arrays.asList(config.bundles()));
        configAmendments.stream().forEach(amendment -> disabledBundles.addAll(Arrays.asList(amendment.getConfig().bundles())));
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
            log.info("Bundle {} disabled by configuration (id={}) ",
                    bundle.getSymbolicName(), bundle.getBundleId());
            bundle.stop();
        }
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
