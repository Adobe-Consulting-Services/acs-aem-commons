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
package com.adobe.acs.commons.twitter.impl;

import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.adapter.AdapterFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.webservicesupport.ConfigurationManager;

@Component
public class TwitterAdapterFactoryBootstrap {

    private static final Logger log = LoggerFactory.getLogger(TwitterAdapterFactoryBootstrap.class);

    @Reference
    private ConfigurationManager configurationManager;

    private ServiceRegistration registration;

    @Activate
    public void activate(ComponentContext ctx) throws Throwable {
        try {
            TwitterAdapterFactory factory = new TwitterAdapterFactory(configurationManager);
            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.put(AdapterFactory.ADAPTABLE_CLASSES, new String[] { "com.day.cq.wcm.api.Page",
                    "com.day.cq.wcm.webservicesupport.Configuration" });
            properties.put(AdapterFactory.ADAPTER_CLASSES, new String[] { "twitter4j.Twitter",
                    "com.adobe.acs.commons.twitter.TwitterClient" });
            registration = ctx.getBundleContext().registerService(AdapterFactory.SERVICE_NAME, factory, properties);
        } catch (Throwable t) {
            // specifically catching Throwable; would rather catch ClassNotFoundException, but the compiler won't let us
            if (t instanceof ClassNotFoundException || t instanceof NoClassDefFoundError) {
                log.warn("Twitter4J not available. Twitter features will be disabled.");
            } else {
                throw t;
            }
        }
    }

    @Deactivate
    public void deactivate() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
