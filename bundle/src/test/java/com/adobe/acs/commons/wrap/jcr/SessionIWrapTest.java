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
package com.adobe.acs.commons.wrap.jcr;

import static org.junit.Assert.assertSame;

import javax.jcr.Session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionIWrapTest {

    @Mock
    Session mockSession;

    @Test
    public void testConstructImpl() {
        SessionWrapper wrapper = new SessionWrapper(mockSession);
        assertSame("unwrapSession to same as mock", mockSession, wrapper.unwrapSession());
    }

    static class SessionWrapper implements SessionIWrap {
        final Session wrapped;

        public SessionWrapper(final Session wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public Session unwrapSession() {
            return wrapped;
        }
    }
}
