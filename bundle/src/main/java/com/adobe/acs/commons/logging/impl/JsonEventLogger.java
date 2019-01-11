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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs OSGi Events for any set of topics to an SLF4j Logger Category, as JSON
 * objects.
 */
@Component( configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
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

        @AttributeDefinition(name = "Event Filter", defaultValue = "(event.topics=*)", description = "LDAP-style event filter query. Leave the default to log all events to the configured topic or topics.")
        String event_filter();

        @AttributeDefinition(name = "Logger Name", description = "The Sling SLF4j Logger Name or Category to send the JSON messages to. Leave empty to disable the logger.")
        String event_logger_category() default "";

        @AttributeDefinition(name = "Logger Level", defaultValue = DEFAULT_LEVEL, options = {
                @Option(value = "TRACE", label = "Trace"),
                @Option(value = "DEBUG", label = "Debug"),
                @Option(value = "INFO", label = "Information"),
                @Option(value = "WARN", label = "Warnings"),
                @Option(value = "ERROR", label = "Error")})
        String event_logger_level() default DEFAULT_LEVEL;
    }

    private String[] topics;
    private String filter;
    private String category;
    private String level;

    /**
     * Suppress the PMD.LoggerIsNotStaticFinal check because the point is to
     * have an SCR-configurable logger separate from the normal class-level log
     * object defined above.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
    private Logger eventLogger;
    private Consumer<String> logMapper = logMapperForLevel(null, null);
    private Supplier<Boolean> logEnabler = logEnablerForLevel(null, null);

    /**
     * Return a logging function appropriate for the specified loglevel.
     *
     * @param logLevel the specified loglegel
     * @param logger the logger to map to
     * @return a string comsuming logger function
     */
    static Consumer<String> logMapperForLevel(final LogLevel logLevel, final Logger logger) {
        if (logLevel != null && logger != null) {
            switch (logLevel) {
                case ERROR: return logger::error;
                case WARN: return logger::warn;
                case INFO: return logger::info;
                case DEBUG: return logger::debug;
                case TRACE: return logger::trace;
                default: return (message) -> { /* do nothing */ };
            }
        }
        return (message) -> { /* do nothing */ };
    }

    /**
     * Return a logging function appropriate for the specified log level.
     *
     * @param logLevel the specified log level
     * @return a boolean-supplying function
     */
    static Supplier<Boolean> logEnablerForLevel(final LogLevel logLevel, final Logger logger) {
        if (logLevel != null && logger != null) {
            switch (logLevel) {
                case ERROR: return logger::isErrorEnabled;
                case WARN: return logger::isWarnEnabled;
                case INFO: return logger::isInfoEnabled;
                case DEBUG: return logger::isDebugEnabled;
                case TRACE: return logger::isTraceEnabled;
                default: return () -> false;
            }
        }
        return () -> false;
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
        if (event.getProperty("event.application") == null && this.logEnabler.get()) {
            logMapper.accept(constructMessage(event));
        }
    }

    //
    // ---------------------------------------------------------< SCR methods >-------------
    //
    @Activate
    @SuppressWarnings("squid:S1149")
    protected void activate(final Config config) {
        log.trace("[activate] entered activate method.");
        this.topics = config.event_topics();
        this.filter = config.event_filter();
        this.category = config.event_logger_category();
        this.level = config.event_logger_level();


        if (StringUtils.isNotEmpty(this.category)) {
            this.eventLogger = LoggerFactory.getLogger(this.category);
        } else {
            log.warn("No event.logger.category specified. No events will be logged.");
        }

        final LogLevel logLevel = LogLevel.fromProperty(this.level);
        this.logEnabler = logEnablerForLevel(logLevel, this.eventLogger);
        this.logMapper = logMapperForLevel(logLevel, this.eventLogger);
        log.trace("[activate] logger state: {}", toString());
    }

    @Deactivate
    protected void deactivate() {
        log.trace("[deactivate] entered deactivate method.");
        this.logEnabler = logEnablerForLevel(null, this.eventLogger);
        this.logMapper = logMapperForLevel(null, this.eventLogger);
        this.eventLogger = null;
    }

    //
    // ---------------------------------------------------------< Object methods >-------------
    //
    @Override
    public String toString() {
        return "JsonEventLogger{"
                + "topics=" + Arrays.toString(topics)
                + ", filter='" + filter + '\''
                + ", category='" + category + '\''
                + ", level='" + level + '\''
                + ", enabled=" + logEnabler.get()
                + '}';
    }

}
