package com.adobe.acs.commons.logging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Dictionary;

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

    public static SyslogAppender.Config createConfig(final String host,
                                                     final int port,
                                                     final String suffixPattern,
                                                     final String facility,
                                                     final String stackTracePattern,
                                                     final boolean throwableExcluded, final String... loggers) {
        return new SyslogAppender.Config() {
            @Override public String host() { return host; }

            @Override public String[] loggers() { return loggers; }

            @Override public int port() { return port; }

            @Override public String suffix_pattern() { return suffixPattern; }

            @Override public String facility() { return facility; }

            @Override public String stack_trace_pattern() { return stackTracePattern; }

            @Override public boolean throwable_excluded() { return throwableExcluded; }

            @Override public Class<? extends Annotation> annotationType() { return SyslogAppender.Config.class; }
        };
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
