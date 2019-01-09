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
package com.adobe.acs.commons.logging.impl;


import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Logs OSGi Events for any set of topics to an SLF4j Logger Category, as JSON
 * objects.
 */
@Component( configurationPolicy=ConfigurationPolicy.REQUIRE, property= {
      "webconsole.configurationFactory.nameHint" + "=" + "Logger: {event.logger.category} for events matching '{event.filter}' on '{event.topics}'"
})
@SuppressWarnings("PMD.MoreThanOneLogger")
@Designate(ocd=JsonEventLogger.Config.class, factory=true)
public class JsonEventLogger implements EventHandler {

    /**
     * Use this logger for tracing this service instance's own lifecycle.
     */
    private static final Logger log = LoggerFactory.getLogger(JsonEventLogger.class);

    /**
     * We add this timestamp property to all logged events
     */
    private static final String PROP_TIMESTAMP = "_timestamp";

    private static final String DEFAULT_LEVEL = "INFO";

    /**
     * A simple enum for Slf4j logging levels.
     */
    private enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR;

        public static LogLevel fromProperty(String prop) {
            if (prop != null) {
                for (LogLevel value : values()) {
                    if (value.name().equalsIgnoreCase(prop)) {
                        return value;
                    }
                }
            }
            return null;
        }
    }
    
    @ObjectClassDefinition(name = "ACS AEM Commons - JSON Event Logger", description = "Logs OSGi Events for any set of topics to an SLF4j Logger Category, as JSON objects.")
    public @interface Config {
        @AttributeDefinition(name = "Event Topics",
                description = "This value lists the topics handled by this logger. The value is a list of strings. If the string ends with a star, all topics in this package and all subpackages match. If the string does not end with a star, this is assumed to define an exact topic.")
        String[] event_topics();

        @AttributeDefinition(name = "Event Filter", description = "LDAP-style event filter query. Leave blank to log all events to the configured topic or topics.")
        String event_filter();

        @AttributeDefinition(name = "Logger Name", description = "The Sling SLF4j Logger Name or Category to send the JSON messages to. Leave empty to disable the logger.")
        String event_logger_category();

        @AttributeDefinition(name = "Logger Level", defaultValue = DEFAULT_LEVEL, options = {
                @Option(value = "TRACE", label = "Trace"),
                @Option(value = "DEBUG", label = "Debug"),
                @Option(value = "INFO", label = "Information"),
                @Option(value = "WARN", label = "Warnings"),
                @Option(value = "ERROR", label = "Error")})
        String event_logger_level();
    }

    private static final String OSGI_TOPICS = EventConstants.EVENT_TOPIC;

    private static final String OSGI_FILTER = EventConstants.EVENT_FILTER;

    private static final String OSGI_CATEGORY = "event.logger.category";

    private static final String OSGI_LEVEL = "event.logger.level";

    private String[] topics;
    private String filter;
    private String category;
    private String level;
    private boolean valid;

    /**
     * Suppress the PMD.LoggerIsNotStaticFinal check because the point is to
     * have an SCR-configurable logger separate from the normal class-level log
     * object defined above.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
    private Logger eventLogger;
    private LogLevel logLevel;

    private ServiceRegistration registration;

    /**
     * Writes an event to the configured logger using the configured log level
     *
     * @param event an OSGi Event
     */
    private void logEvent(Event event) {
        log.trace("[logEvent] event={}", event);
        String message = constructMessage(event);
        if (logLevel == LogLevel.ERROR) {
            this.eventLogger.error(message);
        } else if (logLevel == LogLevel.WARN) {
            this.eventLogger.warn(message);
        } else if (logLevel == LogLevel.INFO) {
            this.eventLogger.info(message);
        } else if (logLevel == LogLevel.DEBUG) {
            this.eventLogger.debug(message);
        } else if (logLevel == LogLevel.TRACE) {
            this.eventLogger.trace(message);
        }
    }

    /**
     * Determines if the logger category is enabled at the configured level
     *
     * @return true if the logger is enabled at the configured log level
     */
    private boolean isLoggerEnabled() {
        if (this.eventLogger != null && this.logLevel != null) {
            if (logLevel == LogLevel.ERROR) {
                return this.eventLogger.isErrorEnabled();
            } else if (logLevel == LogLevel.WARN) {
                return this.eventLogger.isWarnEnabled();
            } else if (logLevel == LogLevel.INFO) {
                return this.eventLogger.isInfoEnabled();
            } else if (logLevel == LogLevel.DEBUG) {
                return this.eventLogger.isDebugEnabled();
            } else if (logLevel == LogLevel.TRACE) {
                return this.eventLogger.isTraceEnabled();
            }
        }
        return false;
    }

    /**
     * Serializes an OSGi {@link org.osgi.service.event.Event} into a JSON
     * object string
     *
     * @param event the event to be serialized as
     * @return a serialized JSON object
     */
    protected static String constructMessage(Event event) {
        Map<String, Object> eventProperties = new LinkedHashMap<>();
        for (String prop : event.getPropertyNames()) {
            Object val = event.getProperty(prop);
            Object converted = convertValue(val);
            eventProperties.put(prop, converted == null ? val : converted);
        }
        eventProperties.put(PROP_TIMESTAMP, ISO8601.format(Calendar.getInstance()));
        Gson gson = new Gson();
        return gson.toJson(eventProperties);
    }

    /**
     * Converts individual java objects to JSONObjects using reflection and
     * recursion
     *
     * @param val an untyped Java object to try to convert
     * @return {@code val} if not handled, or return a converted JSONObject,
     * JSONArray, or String
     * @throws JSONException
     */
    @SuppressWarnings({"unchecked", "squid:S3776"})
    protected static Object convertValue(Object val) {
        if (val instanceof Calendar) {
            try {
                return ISO8601.format((Calendar) val);
            } catch (IllegalArgumentException e) {
                log.debug("[constructMessage] failed to convert Calendar to ISO8601 String: {}, {}", e.getMessage(), val);
            }
        } else if (val instanceof Date) {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime((Date) val);
                return ISO8601.format(calendar);
            } catch (IllegalArgumentException e) {
                log.debug("[constructMessage] failed to convert Date to ISO8601 String: {}, {}", e.getMessage(), val);
            }
        }

        return val;
    }

    //
    // ---------------------------------------------------------< EventHandler methods >-----
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getProperty("event.application") == null && this.isLoggerEnabled()) {
            logEvent(event);
        }
    }

    //
    // ---------------------------------------------------------< SCR methods >-------------
    //
    @Activate
    @SuppressWarnings("squid:S1149")
    protected void activate(ComponentContext ctx) {
        log.trace("[activate] entered activate method.");
        Dictionary<?, ?> props = ctx.getProperties();
        this.topics = PropertiesUtil.toStringArray(props.get(OSGI_TOPICS));
        this.filter = PropertiesUtil.toString(props.get(OSGI_FILTER), "").trim();
        this.category = PropertiesUtil.toString(props.get(OSGI_CATEGORY), "").trim();
        this.level = PropertiesUtil.toString(props.get(OSGI_LEVEL), DEFAULT_LEVEL);

        this.logLevel = LogLevel.fromProperty(this.level);

        this.valid = (this.topics != null && this.topics.length > 0 && !this.category.isEmpty());

        if (this.valid) {
            this.eventLogger = LoggerFactory.getLogger(this.category);
            Dictionary<String, Object> registrationProps = new Hashtable<String, Object>();
            registrationProps.put(EventConstants.EVENT_TOPIC, this.topics);
            if (!this.filter.isEmpty()) {
                registrationProps.put(EventConstants.EVENT_FILTER, this.filter);
            }
            this.registration = ctx.getBundleContext().registerService(EventHandler.class.getName(), this, registrationProps);
        } else {
            log.warn("Not registering invalid event handler. Check configuration.");
        }

        log.debug("[activate] logger state: {}", this);
    }

    @Deactivate
    protected void deactivate() {
        log.trace("[deactivate] entered deactivate method.");
        if (this.registration != null) {
            this.registration.unregister();
            this.registration = null;
        }
        this.eventLogger = null;
        this.logLevel = null;
    }

    //
    // ---------------------------------------------------------< Object methods >-------------
    //
    @Override
    public String toString() {
        return "EventLogger{"
                + "valid=" + valid
                + ", topics=" + Arrays.toString(topics)
                + ", filter='" + filter + '\''
                + ", category='" + category + '\''
                + ", level='" + level + '\''
                + ", enabled=" + isLoggerEnabled()
                + '}';
    }

}
