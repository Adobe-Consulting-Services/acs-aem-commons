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
package com.adobe.acs.commons.logging.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import ch.qos.logback.core.Appender;

@Component(metatype = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE,
        label = "ACS AEM Commons - Syslog Appender",
        description = "Logback appender to send messages using Syslog")
public final class SyslogAppender {

    private static final String ROOT = "ROOT";

    private static final int DEFAULT_PORT = -1;

    private static final String DEFAULT_SUFFIX_PATTERN = "[%thread] %-5level %logger{36} - %msg%n";

    @Property(label = "Host", description = "Host of Syslog server")
    private static final String PROP_HOST = "host";

    @Property(label = "Logger Names", description = "List of logger categories (ROOT for all)",
            unbounded = PropertyUnbounded.ARRAY, value = ROOT)
    private static final String PROP_LOGGERS = "loggers";

    @Property(label = "Port", description = "Port of Syslog server", intValue = -1)
    private static final String PROP_PORT = "port";

    @Property(label = "Suffix Pattern", description = "Logback Pattern defining the message format.",
            value = DEFAULT_SUFFIX_PATTERN)
    private static final String PROP_SUFFIX_PATTERN = "suffix.pattern";

    private ch.qos.logback.classic.net.SyslogAppender appender;

    private ServiceRegistration appenderRegistration;

    @Activate
    protected void activate(ComponentContext ctx) {
        final Dictionary<?, ?> properties = ctx.getProperties();
        String[] loggers = PropertiesUtil.toStringArray(properties.get(PROP_LOGGERS), new String[] {ROOT});
        String suffixPattern = PropertiesUtil
                .toString(properties.get(PROP_SUFFIX_PATTERN), DEFAULT_SUFFIX_PATTERN);
        int port = PropertiesUtil.toInteger(properties.get(PROP_PORT), DEFAULT_PORT);
        String host = PropertiesUtil.toString(properties.get(PROP_HOST), null);

        if (host == null || port == -1) {
            throw new IllegalArgumentException(
                    "Syslog Appender not configured correctly. Both host and port need to be provided.");
        }

        BundleContext bundleContext = ctx.getBundleContext();

        appender = new ch.qos.logback.classic.net.SyslogAppender();

        appender.setSyslogHost(host);
        appender.setPort(port);

        appender.setFacility("USER");
        appender.setSuffixPattern(suffixPattern);

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("loggers", loggers);
        appenderRegistration = bundleContext.registerService(Appender.class.getName(), appender, props);
    }

    @Deactivate
    protected void deactivate() {
        if (appender != null) {
            appender.stop();
            appender = null;
        }

        if (appenderRegistration != null) {
            appenderRegistration.unregister();
            appenderRegistration = null;
        }
    }

}
