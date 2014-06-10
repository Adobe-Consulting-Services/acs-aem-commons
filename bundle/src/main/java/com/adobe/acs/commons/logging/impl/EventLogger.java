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

import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.EventUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Logs OSGi Events for any set of topics to an SLF4j Logger Category, as JSON objects.
 */
@Component(metatype = true, configurationFactory = true, label = "ACS AEM Commons - Event Logger", description = "Logs OSGi Events for any set of topics to an SLF4j Logger Category, as JSON objects.")
@Service
public class EventLogger implements EventHandler {

    /**
     * Use this logger for tracing this service instance's own lifecycle.
     */
    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

    private static final String[] DEFAULT_TOPICS = new String[0];
    private static final String DEFAULT_FILTER = "(event.topics=*)";
    private static final String DEFAULT_CATEGORY = "";
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

    @Property(label = "Event Topics", cardinality = Integer.MAX_VALUE, value = {},
            description = "This value lists the topics handled by this logger. The value is a list of strings. If the string ends with a star, all topics in this package and all subpackages match. If the string does not end with a star, this is assumed to define an exact topic.")
    private static final String OSGI_TOPICS = EventConstants.EVENT_TOPIC;

    @Property(label = "Event Filter", value = DEFAULT_FILTER, description = "LDAP-style event filter query. Do not leave this empty. A safe default value is (event.topics=*) which matches all valid events.")
    private static final String OSGI_FILTER = EventConstants.EVENT_FILTER;

    @Property(label = "Logger Name", value = DEFAULT_CATEGORY, description = "The Sling SLF4j Logger Name or Category to send the JSON messages to. Leave empty to disable the logger.")
    private static final String OSGI_CATEGORY = "event.logger.category";

    @Property(label = "Logger Level", value = DEFAULT_LEVEL, options = {
            @PropertyOption(name = "TRACE", value = "Trace"),
            @PropertyOption(name = "DEBUG", value = "Debug"),
            @PropertyOption(name = "INFO", value = "Information"),
            @PropertyOption(name = "WARN", value = "Warnings"),
            @PropertyOption(name = "ERROR", value = "Error")
    }, description = "Select the logging level the messages should be sent with.")
    private static final String OSGI_LEVEL = "event.logger.level";

    private String[] topics = DEFAULT_TOPICS;
    private String filter;
    private String category;
    private String level;

    /**
     * Suppress the PMD.LoggerIsNotStaticFinal check because the point is to have an
     * SCR-configurable logger separate from the normal class-level log object defined
     * above.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
    private Logger eventLogger;
    private LogLevel logLevel;

    /**
     * Writes an event to the configured logger using the configured log level
     * @param event an OSGi Event
     */
    private void logEvent(Event event) {
        log.trace("[logEvent] event={}", event);
        try {
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
        } catch (JSONException e) {
            log.error("[logEvent] failed to construct log message from event: " + event.toString(), e);
        }
    }

    /**
     * Determines if the logger category is enabled at the configured level
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
     * Serializes an OSGi {@link org.osgi.service.event.Event} into a JSON object string
     *
     * @param event the event to be serialized as
     * @return a serialized JSON object
     * @throws org.apache.sling.commons.json.JSONException
     */
    @SuppressWarnings("unchecked")
    public static String constructMessage(Event event) throws JSONException {
        JSONObject obj = new JSONObject();
        for (String prop : event.getPropertyNames()) {
            Object val = event.getProperty(prop);
            if (val.getClass().isArray()) {
                Object[] vals = (Object[]) val;
                obj.put(prop, Arrays.asList(vals));
            } else if (val instanceof Map) {
                Map<?, ?> valMap = (Map<?, ?>) val;
                if (valMap.isEmpty()) {
                    obj.put(prop, Collections.<String, String>emptyMap());
                } else if (valMap.keySet().iterator().next() instanceof String) {
                    obj.put(prop, (Map<String, ?>) valMap);
                } else {
                    obj.put(prop, val);
                }
            } else if (val instanceof Collection) {
                obj.put(prop, (Collection<?>) val);
            } else if (val instanceof Calendar) {
                try {
                    obj.put(prop, ISO8601.format((Calendar) val));
                } catch (IllegalArgumentException e) {
                    log.debug("[constructMessage] failed to convert Calendar to ISO8601 String: {}, {}", e.getMessage(), val);
                    obj.put(prop, val);
                }
            } else {
                obj.put(prop, val);
            }
        }
        return obj.toString();
    }

    //
    // ---------------------------------------------------------< EventHandler methods >-----
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(Event event) {
        if (EventUtil.isLocal(event) && this.isLoggerEnabled()) {
            logEvent(event);
        }
    }

    //
    // ---------------------------------------------------------< SCR methods >-------------
    //

    @Activate
    protected void activate(Map<String, Object> props) {
        log.trace("[activate] entered activate method.");
        this.topics = PropertiesUtil.toStringArray(props.get(OSGI_TOPICS), DEFAULT_TOPICS);
        this.filter = PropertiesUtil.toString(props.get(OSGI_FILTER), DEFAULT_FILTER);
        this.category = PropertiesUtil.toString(props.get(OSGI_CATEGORY), DEFAULT_CATEGORY).trim();
        this.level = PropertiesUtil.toString(props.get(OSGI_LEVEL), DEFAULT_LEVEL);

        this.logLevel = LogLevel.fromProperty(this.level);
        if (!this.category.isEmpty()) {
            this.eventLogger = LoggerFactory.getLogger(this.category);
        } else {
            this.eventLogger = null;
        }
        log.debug("[activate] logger state: {}", this);
    }

    @Deactivate
    protected void deactivate() {
        log.trace("[deactivate] entered deactivate method.");
        this.eventLogger = null;
        this.logLevel = null;
        this.topics = DEFAULT_TOPICS;
        this.filter = DEFAULT_FILTER;
        this.category = DEFAULT_CATEGORY;
        this.level = DEFAULT_LEVEL;
    }

    //
    // ---------------------------------------------------------< Object methods >-------------
    //

    @Override
    public String toString() {
        return "EventLogger{" +
                "topics=" + Arrays.toString(topics) +
                ", filter='" + filter + '\'' +
                ", category='" + category + '\'' +
                ", level='" + level + '\'' +
                ", enabled=" + isLoggerEnabled() +
                '}';
    }


}
