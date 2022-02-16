/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.wrap.impl;

import javax.annotation.Nonnull;
import javax.jcr.Session;

import com.adobe.acs.commons.wrap.jackrabbit.JackrabbitSessionIWrap;
import com.adobe.acs.commons.wrap.jcr.SessionIWrap;
import org.apache.jackrabbit.api.JackrabbitSession;

/**
 * It's a factory, for session logout guards.
 */
public final class SessionLogoutGuardFactory {

    private SessionLogoutGuardFactory() {
        /* no construction */
    }

    /**
     * Wraps a {@link JackrabbitSession} and implements {@link JackrabbitSession}.
     */
    static class JackrabbitWrapper implements JackrabbitSessionIWrap {
        private final JackrabbitSession wrapped;

        JackrabbitWrapper(@Nonnull final JackrabbitSession wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void logout() {
            /* prevent logout by doing nothing */
        }

        @Nonnull
        @Override
        public JackrabbitSession unwrapSession() {
            return wrapped;
        }
    }

    /**
     * Wraps a generic {@link Session} when it's not also a {@link JackrabbitSession}.
     */
    static class JcrWrapper implements SessionIWrap {
        private final Session wrapped;

        JcrWrapper(@Nonnull final Session wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void logout() {
            /* prevent logout by doing nothing */
        }

        @Nonnull
        @Override
        public Session unwrapSession() {
            return wrapped;
        }
    }

    /**
     * Return the best wrapped version of the provided session.
     *
     * @param session the session to wrap
     * @return a wrapped session
     */
    public static Session useBestWrapper(final Session session) {
        if (session instanceof JackrabbitWrapper || session instanceof JcrWrapper) {
            return session;
        } else if (session instanceof JackrabbitSession) {
            return new JackrabbitWrapper((JackrabbitSession) session);
        } else if (session != null) {
            return new JcrWrapper(session);
        }
        return null;
    }
}
