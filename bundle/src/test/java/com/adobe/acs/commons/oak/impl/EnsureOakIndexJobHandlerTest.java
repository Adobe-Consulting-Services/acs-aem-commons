/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.oak.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
import org.apache.commons.collections.IteratorUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.junit.Assert;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.resourceresolver.MockValueMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.oak.impl.EnsureOakIndex.OakIndexDefinitionException;

/**
 * 
 * Test the algorithm within the JobHandler, but do not validate the inner working of the
 * performing methods
 * 
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class EnsureOakIndexJobHandlerTest {

    // Create a job handler, where we mock the execution of the index actions
    @Spy
    private EnsureOakIndexJobHandler handler = new EnsureOakIndexJobHandler(null, OAK_INDEX, DEFINITION_PATH);

    private static final String OAK_INDEX = "/oak:index";
    private static final String INDEX_NAME = "testIndex";
    private static final String DEFINITION_PATH = "/apps/mydefinitions/index/" + INDEX_NAME;

    private Resource oakIndexResource;
    

    @Before
    public void init() throws RepositoryException, IOException, OakIndexDefinitionException {
        oakIndexResource = mock(Resource.class);

        doNothing().when(handler).disableIndex(any(Resource.class));
        doNothing().when(handler).delete(any(Resource.class));
        doNothing().when(handler).forceRefresh(any(Resource.class));
        doNothing().when(handler).validateEnsureDefinition(any(Resource.class));

        doReturn(null).when(handler).update(any(Resource.class), any(Resource.class), anyBoolean());
        doReturn(null).when(handler).create(any(Resource.class), any(Resource.class));

    }

    Resource getEnsureOakDefinition(Map<String, Object> properties) {
        Resource r = mock(Resource.class);
        ValueMap vm = new MockValueMap(r, properties);
        when(r.getValueMap()).thenReturn(vm);
        when(r.adaptTo(ModifiableValueMap.class)).thenReturn(new ModifiableValueMapDecorator(properties));
        when(r.getName()).thenReturn(INDEX_NAME);
        when(r.getPath()).thenReturn(DEFINITION_PATH);
        when(r.listChildren()).thenReturn(IteratorUtils.EMPTY_LIST_ITERATOR);

        return r;
    }

    Resource getOakDefinition(Map<String, Object> properties) {
        Resource r = mock(Resource.class);
        ValueMap vm = new MockValueMap(r, properties);
        when(r.adaptTo(ModifiableValueMap.class)).thenReturn(new ModifiableValueMapDecorator(properties));
        when(r.getValueMap()).thenReturn(vm);
        when(r.getName()).thenReturn(INDEX_NAME);
        when(r.getPath()).thenReturn(OAK_INDEX + "/" + INDEX_NAME);
        when(r.listChildren()).thenReturn(IteratorUtils.EMPTY_LIST_ITERATOR);

        return r;
    }


    @Test
    public void testIgnoreProperty() throws PersistenceException, RepositoryException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_IGNORE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        Resource def = getEnsureOakDefinition(props);

        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, null));
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));

    }


    @Test
    public void testDisableProperty() throws PersistenceException, RepositoryException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition(props);

        Resource r = mock(Resource.class);

        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify(handler, times(1)).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));
    }

    @Test
    public void testDisablePropertyWithoutExistingIndex() throws PersistenceException, RepositoryException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition(props);

        Resource r = null;

        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));
    }

    @Test
    public void testDeleteProperty() throws PersistenceException, RepositoryException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition(props);

        Resource r = mock(Resource.class);

        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, times(1)).delete(any(Resource.class));
    }

    @Test
    public void testDeletePropertyWithoutExistingIndex() throws PersistenceException, RepositoryException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition(props);

        Resource r = null;

        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));
    }

    @Test
    public void testCreate() throws RepositoryException, IOException {

        Map<String, Object> props = new HashMap<String, Object>();
        Resource def = getEnsureOakDefinition(props);

        Resource index = null;

        Assert.assertFalse(handler.handleLightWeightIndexOperations(def, index));
        handler.handleHeavyWeightIndexOperations(oakIndexResource, def, index);
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));

        verify(handler, times(1)).create(eq(def), eq(oakIndexResource));
        verify(handler, never()).forceRefresh(any(Resource.class));
    }

    @Test
    public void testCreateWithForcedReindex() throws RepositoryException, IOException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_FORCE_REINDEX, "true");
        Resource def = getEnsureOakDefinition(props);

        Resource index = null;

        Assert.assertFalse(handler.handleLightWeightIndexOperations(def, index));
        handler.handleHeavyWeightIndexOperations(oakIndexResource, def, index);
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));

        verify(handler, times(1)).create(eq(def), eq(oakIndexResource));
        verify(handler, times(1)).forceRefresh(any(Resource.class));
    }

    @Test
    public void testUpdate() throws RepositoryException, IOException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EnsureOakIndexJobHandler.PN_FORCE_REINDEX, "true");
        Resource def = getEnsureOakDefinition(props);

        Resource index = mock(Resource.class);

        Assert.assertFalse(handler.handleLightWeightIndexOperations(def, index));
        handler.handleHeavyWeightIndexOperations(oakIndexResource, def, index);
        verify(handler, never()).disableIndex(any(Resource.class));
        verify(handler, never()).delete(any(Resource.class));

        verify(handler, never()).create(any(Resource.class), any(Resource.class));
        verify(handler, times(1)).update(eq(def), eq(oakIndexResource), eq(true));
        verify(handler, never()).forceRefresh(any(Resource.class));
    }

    @Ignore("This test passes locally but fails on TravisCI; Figure out how to make this test pass on TravisCI")
    public void testUpdateOperation() throws RepositoryException, IOException {
        String pnIgnoreMe = "ignoreMe";

        ChecksumGenerator checksumGenerator = mock(ChecksumGenerator.class);

        List<String> ignoreProperties = new ArrayList<String>();
        ignoreProperties.add(pnIgnoreMe);
        EnsureOakIndex eoi = mock(EnsureOakIndex.class);
        when(eoi.getIgnoreProperties()).thenReturn(ignoreProperties);
        when(eoi.getChecksumGenerator()).thenReturn(checksumGenerator);

        Map<String, Object> ensureProps = spy(new HashMap<String, Object>());
        ensureProps.put(EnsureOakIndexJobHandler.PN_RECREATE_ON_UPDATE, true);
        Resource def = getEnsureOakDefinition(ensureProps);

        Map<String, Object> oakProps = spy(new HashMap<String, Object>());
        oakProps.put(EnsureOakIndexJobHandler.PN_RECREATE_ON_UPDATE, true);
        oakProps.put(pnIgnoreMe, "true");
        Resource oak = getOakDefinition(oakProps);

        Map<String,String> defChecksum = new HashMap<String, String>();
        defChecksum.put(def.getPath(), "123");
        when(checksumGenerator.generateChecksums(any(Session.class), any(String.class), any(CustomChecksumGeneratorOptions.class))).thenReturn(defChecksum);

        Map<String,String> oakChecksum = new HashMap<String, String>();
        oakChecksum.put(oak.getPath(), "456");
        when(checksumGenerator.generateChecksums(any(Session.class), any(String.class), any(CustomChecksumGeneratorOptions.class))).thenReturn(oakChecksum);

        when(oakIndexResource.getChild(INDEX_NAME)).thenReturn(oak);

        handler.update(def, oakIndexResource, false);

        verify(oakProps, never()).remove(pnIgnoreMe);
    }
}
