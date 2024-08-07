/*
 * ACS AEM Commons Bundle
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Dictionary;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ch.qos.logback.core.Appender;

public class SyslogAppenderTest {

    public static SyslogAppender.Config createConfig(final String host,
                                                   final int port,
                                                   final String suffixPattern,
                                                   final String facility,
                                                   final String stackTracePattern,
                                                   final boolean throwableExcluded, final String... loggers) {
        final SyslogAppender.Config config = Mockito.mock(SyslogAppender.Config.class);
        Mockito.when(config.host()).thenReturn(host);
        Mockito.when(config.port()).thenReturn(port);
        Mockito.when(config.suffix_pattern()).thenReturn(suffixPattern);
        Mockito.when(config.facility()).thenReturn(facility);
        Mockito.when(config.stack_trace_pattern()).thenReturn(stackTracePattern);
        Mockito.when(config.throwable_excluded()).thenReturn(throwableExcluded);
        Mockito.when(config.loggers()).thenReturn(loggers);
        return config;
    }


    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testActivate() {
        BundleContext ctx = Mockito.mock(BundleContext.class);
        ServiceRegistration<Appender> reg = Mockito.mock(ServiceRegistration.class);
        Mockito.when(ctx.registerService(Mockito.eq(Appender.class), Mockito.any(ch.qos.logback.classic.net.SyslogAppender.class), Mockito.any()))
            .thenReturn(reg);

        SyslogAppender appender = new SyslogAppender();
        appender.activate(ctx, createConfig("localhost", 42, "test",
                "USER", null, false, "my.logger"));

        ArgumentCaptor<Dictionary<String, ?>> propCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(ctx, Mockito.times(1))
            .registerService(
                Mockito.eq(Appender.class),
                Mockito.any(ch.qos.logback.classic.net.SyslogAppender.class),
                propCaptor.capture());
        Mockito.verifyNoMoreInteractions(ctx);
        assertArrayEquals(new String[] {"my.logger"}, (String[])propCaptor.getValue().get("loggers"));

        appender.deactivate();
        Mockito.verify(reg, Mockito.times(1)).unregister();
        Mockito.verifyNoMoreInteractions(ctx);
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

        assertTrue(nullHostThrown, "a null host should throw an IllegalArgumentException");
        boolean emptyHostThrown = false;
        try {
            ch.qos.logback.classic.net.SyslogAppender logAppender =
                    SyslogAppender.constructAppender(createConfig("", 465, "test",
                            "USER", null, false, "my.logger"));
        } catch (final IllegalArgumentException e) {
            emptyHostThrown = true;
        }

        assertTrue(emptyHostThrown, "an empty host should throw an IllegalArgumentException");

        boolean negativePortThrown = false;
        try {
            ch.qos.logback.classic.net.SyslogAppender logAppender =
                    SyslogAppender.constructAppender(createConfig("localhost", -1, "test",
                            "USER", null, false, "my.logger"));
        } catch (final IllegalArgumentException e) {
            negativePortThrown = true;
        }

        assertTrue(negativePortThrown, "a negative port should throw an IllegalArgumentException");

        ch.qos.logback.classic.net.SyslogAppender logAppender =
                SyslogAppender.constructAppender(createConfig("localhost", 465, "test",
                        "USER", "fff", false, "my.logger"));
        assertEquals("fff", logAppender.getStackTracePattern(), "stack trace pattern should match");


    }
}
