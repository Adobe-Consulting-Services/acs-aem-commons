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


import ch.qos.logback.core.net.SyslogAppenderBase;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import ch.qos.logback.core.Appender;

@Component(configurationPolicy=ConfigurationPolicy.REQUIRE, property= {
      "webconsole.configurationFactory.nameHint" + "=" + "Host: {host}, for loggers [{loggers}]"
})
@Designate(ocd=SyslogAppender.Config.class, factory=true)
public final class SyslogAppender {

    private static final String ROOT = "ROOT";

    private static final int DEFAULT_PORT = -1;

    private static final String DEFAULT_SUFFIX_PATTERN = "[%thread] %-5level %logger{36} - %msg%n";

    private static final String DEFAULT_FACILITY = "USER";

    private static final boolean DEFAULT_THROWABLE_EXCLUDED = false;
    
    @ObjectClassDefinition(name = "ACS AEM Commons - Syslog Appender",
        description = "Logback appender to send messages using Syslog")
    public @interface Config {
       
        @AttributeDefinition(name = "Host", description = "Host of Syslog server")
        String host();

        @AttributeDefinition(name = "Logger Names", description = "List of logger categories (ROOT for all)",
                 defaultValue = ROOT)
        String[] loggers();

        @AttributeDefinition(name = "Port", description = "Port of Syslog server", defaultValue = "-1")
        int port();

        @AttributeDefinition(name = "Suffix Pattern", description = "Logback Pattern defining the message format.",
                defaultValue = DEFAULT_SUFFIX_PATTERN)
        String suffix_pattern();

        @AttributeDefinition(name = "Syslog Facility", defaultValue = DEFAULT_FACILITY,
                description = "The Syslog Facility is meant to identify the source of a message, separately from any context "
                + "included in the Suffix Pattern. The facility option must be set to one of the strings KERN, USER, MAIL, DAEMON, "
                + "AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP, NTP, AUDIT, ALERT, CLOCK, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, "
                + "LOCAL5, LOCAL6, LOCAL7. Case is not important.")
        String facility();

        @AttributeDefinition(name = "Stack Trace Pattern", description = "Logback Pattern for customizing the string appearing just before each stack "
                 + "trace line. The default value for this property is a single tab character.")
        String stack_trace_pattern();

        @AttributeDefinition(name = "Exclude Throwables", description = "Set to true to cause stack trace data associated with a Throwable to be omitted. "
                + "By default, this is set to false so that stack trace data is sent to the syslog server.", defaultValue = ""+DEFAULT_THROWABLE_EXCLUDED)
        boolean throwable_excluded();
    }

    private static final String PROP_HOST = "host";

    private static final String PROP_LOGGERS = "loggers";

    private static final String PROP_PORT = "port";

    private static final String PROP_SUFFIX_PATTERN = "suffix.pattern";

    private static final String PROP_FACILITY = "facility";

    private static final String PROP_STACK_TRACE_PATTERN = "stack.trace.pattern";

    private static final String PROP_THROWABLE_EXCLUDED = "throwable.excluded";

    private ch.qos.logback.classic.net.SyslogAppender appender;

    private ServiceRegistration appenderRegistration;

    @Activate
    @SuppressWarnings("squid:S1149")
    protected void activate(ComponentContext ctx) {
        final Dictionary<?, ?> properties = ctx.getProperties();
        final String[] loggers = PropertiesUtil.toStringArray(properties.get(PROP_LOGGERS), new String[] {ROOT});
        final String suffixPattern = PropertiesUtil
                .toString(properties.get(PROP_SUFFIX_PATTERN), DEFAULT_SUFFIX_PATTERN);
        final int port = PropertiesUtil.toInteger(properties.get(PROP_PORT), DEFAULT_PORT);
        final String host = PropertiesUtil.toString(properties.get(PROP_HOST), null);
        final String facility = PropertiesUtil.toString(properties.get(PROP_FACILITY), DEFAULT_FACILITY);
        final String stackTracePattern = PropertiesUtil.toString(properties.get(PROP_STACK_TRACE_PATTERN), null);
        final boolean throwableExcluded = PropertiesUtil.toBoolean(properties.get(PROP_THROWABLE_EXCLUDED), DEFAULT_THROWABLE_EXCLUDED);

        if (host == null || port == -1) {
            throw new IllegalArgumentException(
                    "Syslog Appender not configured correctly. Both host and port need to be provided.");
        }

        // throws a descriptive IllegalArgumentException if facility is not valid.
        SyslogAppenderBase.facilityStringToint(facility);

        final BundleContext bundleContext = ctx.getBundleContext();

        appender = new ch.qos.logback.classic.net.SyslogAppender();

        appender.setSyslogHost(host);
        appender.setPort(port);

        appender.setFacility(facility);
        appender.setSuffixPattern(suffixPattern);

        if (StringUtils.isNotEmpty(stackTracePattern)) {
            appender.setStackTracePattern(stackTracePattern);
        }

        appender.setThrowableExcluded(throwableExcluded);

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
