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
import static org.mockito.Mockito.when;

import com.adobe.acs.commons.wrap.jackrabbit.JackrabbitSessionIWrap;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JackrabbitSessionIWrapTest {

    @Mock
    JackrabbitSession session;

    @Mock
    UserManager userManager;

    @Before
    public void setUp() throws Exception {
        when(session.getUserManager()).thenReturn(userManager);
    }

    @Test
    public void testGetUserManager() throws Exception {
        JackrabbitSession wrapper = new JackrabbitSessionWrapper(session);
        UserManager mockUserMan = wrapper.getUserManager();
        assertSame("user manager should be same as mocked", userManager, mockUserMan);
    }

    static class JackrabbitSessionWrapper implements JackrabbitSessionIWrap {
        final JackrabbitSession session;

        public JackrabbitSessionWrapper(final JackrabbitSession session) {
            this.session = session;
        }

        @Override
        public JackrabbitSession unwrapSession() {
            return session;
        }
    }
}
