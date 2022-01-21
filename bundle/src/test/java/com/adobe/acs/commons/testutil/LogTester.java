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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fake Logger Appender used to execute test assertions
 * on logging activity.
 */
public class LogTester extends AppenderBase<LoggingEvent> {
    private static final String ALL = "overall";
    private static final String OAK_AUDIT_LOGGER = "org.apache.jackrabbit.oak.audit";

    private static ThreadLocal<Map<String, List<LoggingEvent>>> loggingEventsThreadLocal = ThreadLocal.withInitial(() -> {
        Map<String, List<LoggingEvent>> events = new HashMap<>();
        events.put(ALL, new ArrayList<>());
        return events;
    });

    private static Map<String, List<LoggingEvent>> loggingEvents() {
        return loggingEventsThreadLocal.get();
    }

    /**
     * Assert that (any) logger contains the expected string.
     *
     * @param expected Expected string.
     */
    public static void assertLogText(String expected) {
        assertLogText(expected, ALL, null);
    }

    /**
     * Assert that a specified logger contains the expected string.
     *
     * @param expected   Expected string.
     * @param loggerName Logger to check.
     */
    public static void assertLogText(String expected, String loggerName) {
        assertLogText(expected, loggerName, null);
    }

    /**
     * Assert that a specified logger contains the exepcted string at a given line in the log.
     *
     * @param expected   Expected string.
     * @param loggerName Logger to check.
     * @param line       Line to check (any line if null).
     */
    public static void assertLogText(String expected, String loggerName, Integer line) {
        boolean found = foundInLogs(expected, loggerName, line);
        String msg = "Expected " + loggerName + " log to contain '" + expected + "'";
        if (line != null) {
            msg += " at line: " + line;
        }
        if (!found) {
            msg += " - Log Contents:";
            List<LoggingEvent> events = loggingEvents().get(loggerName);
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

    /**
     * Assert that no logger contains the unexpected string.
     *
     * @param unexpected Unexpected string.
     */
    public static void assertNotLogText(String unexpected) {
        assertNotLogText(unexpected, ALL);
    }

    /**
     * Assert that a specified logger does not contain the unexpected string.
     *
     * @param unexpected Unexpected string.
     * @param loggerName Logger to check.
     */
    public static void assertNotLogText(String unexpected, String loggerName) {
        boolean found = foundInLogs(unexpected, loggerName, null);
        String msg = "Expected " + loggerName + " log to not contain '" + unexpected + "'";
        if (found) {
            msg += " - Log Contents:";
            List<LoggingEvent> events = loggingEvents().get(loggerName);
            for (LoggingEvent event : events) {
                msg += "\n" + event.getFormattedMessage();
            }
        }
        assertFalse(msg, found);
    }

    private static boolean foundInLogs(String expected, String loggerName, Integer line) {
        boolean found = false;
        if (loggingEvents().containsKey(loggerName)) {
            List<LoggingEvent> events = loggingEvents().get(loggerName);
            if (line != null) {
                if (events.size() >= line) {
                    found = events.get(line - 1).getFormattedMessage().equals(expected);
                }
            } else {
                for (LoggingEvent event : events) {
                    // the logger name represented by OAK_AUDIT_LOGGER will fill output with stack traces if
                    // event.getFormattedMessage() is checked after closing the original JCR session, and it's deep
                    // enough that this library will likely need to examine that particular logger's output for test
                    // assertions.
                    if (event.getLoggerName().startsWith(OAK_AUDIT_LOGGER)) {
                        continue;
                    }
                    if (event.getFormattedMessage().equals(expected)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    /**
     * Clear the contents of the log.
     * <p>
     * This should generally be called at the end of any @Before test functions
     * so that the log contents are limited to only those created within the
     * body of the test.
     */
    public static void reset() {
        loggingEventsThreadLocal.remove();
    }

    /**
     * Save the logging event to memory so that assertions can be run on log contents.
     */
    @Override
    protected void append(LoggingEvent loggingEvent) {
        loggingEvents().get(ALL).add(loggingEvent);

        String loggerName = loggingEvent.getLoggerName();
        if (!loggingEvents().containsKey(loggerName)) {
            loggingEvents().put(loggerName, new ArrayList<>());
        }
        loggingEvents().get(loggerName).add(loggingEvent);
    }

}
