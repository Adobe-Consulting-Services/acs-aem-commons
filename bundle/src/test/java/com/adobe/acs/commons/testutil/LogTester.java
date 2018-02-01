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
package com.adobe.acs.commons.testutil;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Fake Logger Appender used to execute test assertions
 * on logging activity.
 */
public class LogTester extends AppenderBase<LoggingEvent> {
    private static final String ALL = "overall";

    private static Map<String, List<LoggingEvent>> loggingEvents;

    static {
        reset();
    }

    public static void assertLogEvents(int expected) {
        assertLogEvents(expected, ALL);
    }

    public static void assertLogEvents(int expected, String loggerName) {
        int count = 0;
        if (loggingEvents.containsKey(loggerName)) {
            count = loggingEvents.get(loggerName).size();
        }
        String msg = "Expected " + loggerName + " log size to be " + expected + " event";
        msg += (expected > 1 ? "s" : "");
        msg += " but found " + count;
        assertEquals(msg, expected, count);
    }

    public static void assertLogText(String expected) {
        assertLogText(expected, ALL, null);
    }

    public static void assertLogText(String expected, String loggerName) {
        assertLogText(expected, loggerName, null);
    }

    public static void assertLogText(String expected, String loggerName, Integer line) {
        boolean found = false;
        if (loggingEvents.containsKey(loggerName)) {
            List<LoggingEvent> events = loggingEvents.get(loggerName);
            if (line != null) {
                if (events.size() >= line) {
                    found = events.get(line - 1).getFormattedMessage().equals(expected);
                }
            } else {
                for (LoggingEvent event : loggingEvents.get(loggerName)) {
                    if (event.getFormattedMessage().equals(expected)) {
                        found = true;
                        break;
                    }
                }
            }
        }

        String msg = "Expected " + loggerName + " log to contain '" + expected + "'";
        if (line != null) {
            msg += " at line: " + line;
        }
        if (!found) {
            msg += " - Log Contents:";
            List<LoggingEvent> events = loggingEvents.get(loggerName);
            if (events != null && events.size() > 0) {
                for (LoggingEvent event : events) {
                    msg += "\n" + event.getFormattedMessage();
                }
            } else {
                msg += " (none)";
            }
        }
        assertTrue(msg, found);
    }

    public static void reset() {
        loggingEvents = new HashMap<String, List<LoggingEvent>>();
        loggingEvents.put(ALL, new ArrayList<LoggingEvent>());
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        loggingEvents.get(ALL).add(loggingEvent);

        String loggerName = loggingEvent.getLoggerName();
        if (!loggingEvents.containsKey(loggerName)) {
            loggingEvents.put(loggerName, new ArrayList<LoggingEvent>());
        }
        loggingEvents.get(loggerName).add(loggingEvent);
    }

}
