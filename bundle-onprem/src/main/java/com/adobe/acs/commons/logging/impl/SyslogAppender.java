/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2023 Adobe
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
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import ch.qos.logback.core.Appender;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        "webconsole.configurationFactory.nameHint=Host: {host}, for loggers [{loggers}]"
    })
@Designate(ocd = SyslogAppender.Config.class, factory = true)
public final class SyslogAppender {

    @ObjectClassDefinition(name = "ACS AEM Commons - Syslog Appender",
            description = "Logback appender to send messages using Syslog")
    public @interface Config {

        @AttributeDefinition(name = "Host", description = "Host of Syslog server")
        String host();

        @AttributeDefinition(name = "Logger Names", description = "List of logger categories (ROOT for all)")
        String[] loggers() default {"ROOT"};

        @AttributeDefinition(name = "Port", description = "Port of Syslog server")
        int port() default -1;

        @AttributeDefinition(name = "Suffix Pattern", description = "Logback Pattern defining the message format.")
        String suffix_pattern() default "[%thread] %-5level %logger{36} - %msg%n";

        @AttributeDefinition(name = "Syslog Facility", description = "The Syslog Facility is meant to identify the source of a message, "
                + "separately from any context included in the Suffix Pattern. The facility option must be set to one of the strings "
                + "KERN, USER, MAIL, DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP, NTP, AUDIT, ALERT, CLOCK, LOCAL0, LOCAL1, "
                + "LOCAL2, LOCAL3, LOCAL4, LOCAL5, LOCAL6, LOCAL7. Case is not important.")
        String facility() default "USER";

        @AttributeDefinition(name = "Stack Trace Pattern", description = "Logback Pattern for customizing the string appearing just before each stack "
                + "trace line. The default value for this property is a single tab character.")
        String stack_trace_pattern() default "";

        @AttributeDefinition(name = "Exclude Throwables", description = "Set to true to cause stack trace data associated with a Throwable to be omitted. "
                + "By default, this is set to false so that stack trace data is sent to the syslog server.")
        boolean throwable_excluded() default false;
    }

    private ch.qos.logback.classic.net.SyslogAppender appender;

    private ServiceRegistration<Appender> appenderRegistration;

    @Activate
    @SuppressWarnings("squid:S1149")
    protected void activate(final BundleContext ctx, final Config config) {
        this.appender = constructAppender(config);

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("loggers", config.loggers());
        appenderRegistration = ctx.registerService(Appender.class, appender, props);
    }

    static ch.qos.logback.classic.net.SyslogAppender constructAppender(final Config config) {
        if (StringUtils.isEmpty(config.host()) || config.port() == -1) {
            throw new IllegalArgumentException(
                    "Syslog Appender not configured correctly. Both host and port need to be provided.");
        }

        // throws a descriptive IllegalArgumentException if facility is not valid.
        SyslogAppenderBase.facilityStringToint(config.facility());

        final ch.qos.logback.classic.net.SyslogAppender appender = new ch.qos.logback.classic.net.SyslogAppender();

        appender.setSyslogHost(config.host());
        appender.setPort(config.port());

        appender.setFacility(config.facility());
        appender.setSuffixPattern(config.suffix_pattern());

        if (StringUtils.isNotEmpty(config.stack_trace_pattern())) {
            appender.setStackTracePattern(config.stack_trace_pattern());
        }

        appender.setThrowableExcluded(config.throwable_excluded());
        return appender;
    }


    @Deactivate
    protected void deactivate() {
        if (appender != null) {
            if (appender.isStarted()) {
                appender.stop();
            }
            appender = null;
        }

        if (appenderRegistration != null) {
            try {
                appenderRegistration.unregister();
            } catch (IllegalStateException e) {
                // ignore
            }
            appenderRegistration = null;
        }
    }

}
