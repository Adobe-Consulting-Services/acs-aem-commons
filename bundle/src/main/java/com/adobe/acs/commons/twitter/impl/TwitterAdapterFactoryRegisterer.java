/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

@Component(configurationPid = "com.adobe.acs.commons.twitter.impl.TwitterAdapterFactory")
@Designate(ocd=TwitterAdapterFactoryRegisterer.Config.class)
public class TwitterAdapterFactoryRegisterer {

    private static final Logger log = LoggerFactory.getLogger(TwitterAdapterFactory.class);

    private static final boolean DEFAULT_USE_SSL = true;
    
    @ObjectClassDefinition(name = "ACS AEM Commons - Twitter Client Adapter Factory",
        description = "Adapter Factory to generate TwitterClient objects.")
    public @interface Config {
 
        @AttributeDefinition(name = "HTTP Proxy Host", description = "HTTP Proxy Host, leave blank for none")
        String http_proxy_host();

        @AttributeDefinition(name = "HTTP Proxy Port", description = "HTTP Proxy Port, leave 0 for none", defaultValue = "0")
        int http_proxy_port();

        @AttributeDefinition(name = "Use SSL", description = "Use SSL Connections", defaultValue = ""+DEFAULT_USE_SSL)
        boolean use_ssl();

    }

    private static final String PROP_HTTP_PROXY_HOST = "http.proxy.host";

    private static final String PROP_HTTP_PROXY_PORT = "http.proxy.port";

    private static final String PROP_USE_SSL = "use.ssl";

    private ServiceRegistration<AdapterFactory> adapterFactoryServiceRegistration;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        String httpProxyHost = PropertiesUtil.toString(properties.get(PROP_HTTP_PROXY_HOST), null);
        int httpProxyPort = PropertiesUtil.toInteger(properties.get(PROP_HTTP_PROXY_PORT), 0);
        boolean useSsl = PropertiesUtil.toBoolean(properties.get(PROP_USE_SSL), DEFAULT_USE_SSL);

        try {
            TwitterAdapterFactory adapterFactory = new TwitterAdapterFactory(httpProxyHost, httpProxyPort, useSsl);

            @SuppressWarnings("squid:S1149")
            Dictionary<String, Object> serviceProps = new Hashtable<>();
            serviceProps.put(AdapterFactory.ADAPTABLE_CLASSES, new String[] { "com.day.cq.wcm.api.Page", "com.day.cq.wcm.webservicesupport.Configuration" });
            serviceProps.put(AdapterFactory.ADAPTER_CLASSES, new String[] { "twitter4j.Twitter", "com.adobe.acs.commons.twitter.TwitterClient" });

            this.adapterFactoryServiceRegistration = bundleContext.registerService(AdapterFactory.class, adapterFactory, serviceProps);

        } catch (NoClassDefFoundError e) {
            log.info("Twitter4J Library not found. Not registering TwitterAdapterFactory.");
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.adapterFactoryServiceRegistration != null) {
            this.adapterFactoryServiceRegistration.unregister();
            this.adapterFactoryServiceRegistration = null;
        }
    }
}
