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

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionIWrapTest {

    @Mock
    Session session;

    @Mock
    Node rootNode;

    @Before
    public void setUp() throws Exception {
        when(session.getRootNode()).thenReturn(rootNode);
    }

    @Test
    public void testGetRootNode() throws Exception {
        SessionWrapper wrapper = new SessionWrapper(session);
        Node wrappedRootNode = wrapper.getRootNode();
        assertSame("root node should be same as mock", rootNode, wrappedRootNode);
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
