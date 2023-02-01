/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.logging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockBundleContext;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class SyslogAppenderTest {

    class UnregMockServiceRegistration implements ServiceRegistration {
        final ServiceRegistration wrapped;
        boolean unregistered = false;

        public UnregMockServiceRegistration(final ServiceRegistration wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public ServiceReference getReference() {
            return wrapped.getReference();
        }

        @Override
        public void setProperties(final Dictionary dictionary) {
            this.wrapped.setProperties(dictionary);
        }

        @Override
        public void unregister() {
            if (this.unregistered) {
                throw new IllegalStateException("already unregistered");
            }
            this.unregistered = true;
        }
    }

    class UnregMockBundleContext extends MockBundleContext {
        public UnregMockBundleContext(final MockBundle bundle) {
            super(bundle);
        }

        @Override
        public ServiceRegistration registerService(final String s, final Object o, final Dictionary dictionary) {
            return new UnregMockServiceRegistration(super.registerService(s, o, dictionary));
        }
    }

    public static Map<String, Object> createConfig(final String host,
                                                   final int port,
                                                   final String suffixPattern,
                                                   final String facility,
                                                   final String stackTracePattern,
                                                   final boolean throwableExcluded, final String... loggers) {
        Map<String, Object> map = new HashMap<>();
        map.put(SyslogAppender.PROP_HOST, host);
        map.put(SyslogAppender.PROP_PORT, port);
        map.put(SyslogAppender.PROP_SUFFIX_PATTERN, suffixPattern);
        map.put(SyslogAppender.PROP_FACILITY, facility);
        map.put(SyslogAppender.PROP_STACK_TRACE_PATTERN, stackTracePattern);
        map.put(SyslogAppender.PROP_THROWABLE_EXCLUDED, throwableExcluded);
        map.put(SyslogAppender.PROP_LOGGERS, loggers);
        return map;
    }


    @Test
    public void testActivate() {
        SyslogAppender appender = new SyslogAppender();
        MockBundleContext ctx = new UnregMockBundleContext(null);
        appender.activate(ctx, createConfig("localhost", 42, "test",
                "USER", null, false, "my.logger"));
        appender.deactivate();
    }

    @Test
    public void testConstructSyslogAppender() {
        boolean nullHostThrown = false;
        try {
            ch.qos.logback.classic.net.SyslogAppender logAppender =
                    SyslogAppender.constructAppender(createConfig(null, 465, "test",
                            "USER", null, false, "my.logger"));
        } catch (final IllegalArgumentException e) {
            nullHostThrown = true;
        }

        assertTrue("a null host should throw an IllegalArgumentException", nullHostThrown);
        boolean emptyHostThrown = false;
        try {
            ch.qos.logback.classic.net.SyslogAppender logAppender =
                    SyslogAppender.constructAppender(createConfig("", 465, "test",
                            "USER", null, false, "my.logger"));
        } catch (final IllegalArgumentException e) {
            emptyHostThrown = true;
        }

        assertTrue("an empty host should throw an IllegalArgumentException", emptyHostThrown);

        boolean negativePortThrown = false;
        try {
            ch.qos.logback.classic.net.SyslogAppender logAppender =
                    SyslogAppender.constructAppender(createConfig("localhost", -1, "test",
                            "USER", null, false, "my.logger"));
        } catch (final IllegalArgumentException e) {
            negativePortThrown = true;
        }

        assertTrue("a negative port should throw an IllegalArgumentException", negativePortThrown);

        ch.qos.logback.classic.net.SyslogAppender logAppender =
                SyslogAppender.constructAppender(createConfig("localhost", 465, "test",
                        "USER", "fff", false, "my.logger"));
        assertEquals("stack trace pattern should match", "fff", logAppender.getStackTracePattern());


    }
}
