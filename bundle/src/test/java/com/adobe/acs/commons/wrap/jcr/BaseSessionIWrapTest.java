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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.ContentHandler;

@RunWith(MockitoJUnitRunner.class)
public class BaseSessionIWrapTest {

    @Mock
    Session session;

    @Mock
    Session otherSession;

    @Mock
    Workspace workspace;

    @Mock
    Repository repository;

    @Mock
    Node rootNode;

    @Mock
    Property someProperty;

    @Mock
    ValueFactory valueFactory;

    @Mock
    ContentHandler contentHandler;

    @Mock
    AccessControlManager accessControlManager;

    @Mock
    RetentionManager retentionManager;

    String[] attributeNames = new String[]{"attrOne", "attrTwo"};
    String[] namespacePrefixes = new String[]{"jcr", "nt", "sling"};
    String[] lockTokens = new String[]{"lockOne", "lockTwo"};

    @Before
    public void setUp() throws Exception {
        when(session.getRootNode()).thenReturn(rootNode);
        when(session.getWorkspace()).thenReturn(workspace);
        when(session.getRepository()).thenReturn(repository);
        when(session.getUserID()).thenReturn("hal9000");
        when(session.getAttributeNames()).thenReturn(attributeNames);
        when(session.getAttribute("attrOne")).thenReturn("valueOne");
        when(session.impersonate(null)).thenReturn(otherSession);
        when(session.getNodeByUUID(anyString())).thenReturn(rootNode);
        when(session.getNodeByIdentifier(anyString())).thenReturn(rootNode);
        when(session.getItem("/item/node")).thenReturn(rootNode);
        when(session.getNode("/item/node")).thenReturn(rootNode);
        when(session.getProperty("/item/property")).thenReturn(someProperty);
        when(session.itemExists(anyString())).thenReturn(true);
        when(session.nodeExists(anyString())).thenReturn(true);
        when(session.propertyExists(anyString())).thenReturn(true);

        when(session.hasPendingChanges()).thenReturn(true);
        when(session.getValueFactory()).thenReturn(valueFactory);
        when(session.hasPermission(anyString(), anyString())).thenReturn(true);
        when(session.hasCapability(anyString(), any(), any(Object[].class))).thenReturn(true);
        when(session.getImportContentHandler(anyString(), anyInt())).thenReturn(contentHandler);
        when(session.getNamespacePrefixes()).thenReturn(namespacePrefixes);
        when(session.getNamespaceURI(anyString())).thenReturn("http://someuri/");
        when(session.getNamespacePrefix(anyString())).thenReturn("someprefix");
        when(session.isLive()).thenReturn(true);
        when(session.getLockTokens()).thenReturn(lockTokens);
        when(session.getAccessControlManager()).thenReturn(accessControlManager);
        when(session.getRetentionManager()).thenReturn(retentionManager);
    }

    @Test
    public void testGets() throws Exception {
        SessionWrapper wrapper = new SessionWrapper(session);
        assertSame("wrapItem(rootNode) should be same as mock", rootNode, wrapper.wrapItem(rootNode));
        assertSame("wrapWorkspace(workspace) should be same as mock", workspace, wrapper.wrapWorkspace(workspace));
        assertSame("wrapSession(session) should be same as mock", otherSession, wrapper.wrapSession(otherSession));
        assertSame("root node should be same as mock", rootNode, wrapper.getRootNode());
        assertSame("repository should be same as mock", repository, wrapper.getRepository());
        assertEquals("expect correct userId", "hal9000", wrapper.getUserID());
        assertArrayEquals("expect correct attributeNames", attributeNames, wrapper.getAttributeNames());
        assertEquals("expect correct attribute", "valueOne", wrapper.getAttribute("attrOne"));
        assertSame("expect same workspace", workspace, wrapper.getWorkspace());
        assertSame("expect other session on impersonate", otherSession, wrapper.impersonate(null));
        assertSame("get root node by uuid", rootNode, wrapper.getNodeByUUID("some-uuid"));
        assertSame("get root node by identifier", rootNode, wrapper.getNodeByIdentifier("some-uuid"));
        assertSame("getItem()", rootNode, wrapper.getItem("/item/node"));
        assertSame("getNode()", rootNode, wrapper.getNode("/item/node"));
        assertSame("getProperty()", someProperty, wrapper.getProperty("/item/property"));
        assertTrue("itemExists", wrapper.itemExists("/item/node"));
        assertTrue("nodeExists", wrapper.nodeExists("/item/node"));
        assertTrue("propertyExists", wrapper.propertyExists("/item/property"));
        assertTrue("hasPendingChanges", wrapper.hasPendingChanges());
        assertSame("getValueFactory", valueFactory, wrapper.getValueFactory());
        assertTrue("hasPermission", wrapper.hasPermission("path", "action"));
        assertTrue("hasCapability", wrapper.hasCapability("path", "action", new Object[0]));
        assertSame("getImportContentHandler", contentHandler, wrapper.getImportContentHandler("path", 0));
        assertArrayEquals("expect correct namespacePrefixes", namespacePrefixes, wrapper.getNamespacePrefixes());
        assertEquals("getNamespaceURI", "http://someuri/", wrapper.getNamespaceURI("someprefix"));
        assertEquals("getNamespacePrefix", "someprefix", wrapper.getNamespacePrefix("http://someuri/"));
        assertTrue("isLive", wrapper.isLive());
        assertArrayEquals("getLockTokens", lockTokens, wrapper.getLockTokens());
        assertSame("getAccessControlManager", accessControlManager, wrapper.getAccessControlManager());
        assertSame("getRetentionManager", retentionManager, wrapper.getRetentionManager());
    }

    @Test
    public void testVoids() throws Exception {
        SessionWrapper wrapper = new SessionWrapper(session);
        wrapper.move("/from", "/to");
        verify(session, times(1)).move("/from", "/to");
        wrapper.removeItem("/item");
        verify(session, times(1)).removeItem("/item");
        wrapper.save();
        verify(session, times(1)).save();
        wrapper.refresh(true);
        verify(session, times(1)).refresh(true);
        wrapper.refresh(false);
        verify(session, times(1)).refresh(false);
        wrapper.checkPermission("/path", "actions");
        verify(session, times(1)).checkPermission("/path", "actions");
        wrapper.importXML("/path", null, 42);

        final OutputStream mockOs = mock(OutputStream.class);
        verify(session, times(1)).importXML("/path", null, 42);
        wrapper.exportSystemView("/path", contentHandler, true, false);
        verify(session, times(1)).exportSystemView("/path", contentHandler, true, false);
        wrapper.exportSystemView("/path", contentHandler, false, true);
        verify(session, times(1)).exportSystemView("/path", contentHandler, false, true);
        wrapper.exportSystemView("/pathos", mockOs, true, false);
        verify(session, times(1)).exportSystemView("/pathos", mockOs, true, false);
        wrapper.exportSystemView("/pathos", mockOs, false, true);
        verify(session, times(1)).exportSystemView("/pathos", mockOs, false, true);
        wrapper.exportDocumentView("/path", contentHandler, true, false);
        verify(session, times(1)).exportDocumentView("/path", contentHandler, true, false);
        wrapper.exportDocumentView("/path", contentHandler, false, true);
        verify(session, times(1)).exportDocumentView("/path", contentHandler, false, true);
        wrapper.exportDocumentView("/pathos", mockOs, true, false);
        verify(session, times(1)).exportDocumentView("/pathos", mockOs, true, false);
        wrapper.exportDocumentView("/pathos", mockOs, false, true);
        verify(session, times(1)).exportDocumentView("/pathos", mockOs, false, true);

        wrapper.setNamespacePrefix("someprefix", "http://someuri/");
        verify(session, times(1)).setNamespacePrefix("someprefix", "http://someuri/");

        wrapper.logout();
        verify(session, times(1)).logout();

        wrapper.addLockToken("lockToken");
        verify(session, times(1)).addLockToken("lockToken");
        wrapper.removeLockToken("lockToken");
        verify(session, times(1)).removeLockToken("lockToken");

    }

    static class SessionWrapper implements BaseSessionIWrap<Session> {
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
