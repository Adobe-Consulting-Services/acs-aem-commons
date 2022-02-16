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
package com.adobe.acs.commons.version.impl;

import com.adobe.acs.commons.version.EvolutionContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.util.Collections;

import static com.adobe.acs.commons.version.impl.CurrentEvolutionImpl.LATEST_VERSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EvolutionContextImplTest {

    private static final String RESOURCE_PATH = "/path/to/resource";
    private static final String FROZEN_NODE_1_PATH = "/path/to/first/jcr:frozenNode";
    private static final String FROZEN_NODE_2_PATH = "/path/to/second/jcr:frozenNode";
    private static final String VERSION_1 = "version-1";
    private static final String VERSION_2 = "version-2";
    private static final String LATEST = "version-2";

    private static final String TESTED_KEY = "jcr:title";
    private static final String TESTED_VALUE_1 = "old";
    private static final String TESTED_VALUE_2 = "new";

    @Mock
    Resource resource;

    @Mock
    ResourceResolver resolver;

    @Mock
    Workspace workspace;

    @Mock
    Session session;

    @Mock
    VersionManager versionManager;

    @Mock
    VersionHistory versionHistory;

    @Mock
    VersionIterator versionIterator;

    @Mock
    Version version1;

    @Mock
    Version version2;

    @Mock
    Node frozenNode1;

    @Mock
    Node frozenNode2;

    @Mock
    Resource frozenResource1;

    @Mock
    Resource frozenResource2;

    ValueMap frozenResource1ValueMap = new ValueMapDecorator(Collections.singletonMap(TESTED_KEY, TESTED_VALUE_1));

    ValueMap frozenResource2ValueMap = new ValueMapDecorator(Collections.singletonMap(TESTED_KEY, TESTED_VALUE_2));

    @Mock
    Property jcrPropertyTitle1;

    @Mock
    Property jcrPropertyTitle2;

    @Mock
    Value value1;

    @Mock
    Value value2;

    EvolutionConfig config = new EvolutionConfig(new String[]{"foo"}, new String[]{"bar"});

    EvolutionContext evolutionContext;

    @Before
    public void setUp() throws Exception {
        when(resource.getPath()).thenReturn(RESOURCE_PATH);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resource.getValueMap()).thenReturn(frozenResource2ValueMap);
        when(resource.adaptTo(Node.class)).thenReturn(frozenNode2);
        when(resource.getChildren()).thenReturn(Collections.emptyList());

        when(resolver.adaptTo(Session.class)).thenReturn(session);
        when(resolver.resolve(eq(FROZEN_NODE_1_PATH))).thenReturn(frozenResource1);
        when(resolver.resolve(eq(FROZEN_NODE_2_PATH))).thenReturn(frozenResource2);

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getVersionManager()).thenReturn(versionManager);
        when(versionManager.getVersionHistory(eq(RESOURCE_PATH))).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionIterator);

        when(versionIterator.hasNext()).thenReturn(true, true, false);
        when(versionIterator.next()).thenReturn(version1, version2);

        when(version1.getName()).thenReturn(VERSION_1);
        when(version2.getName()).thenReturn(VERSION_2);
        when(version1.getFrozenNode()).thenReturn(frozenNode1);
        when(version2.getFrozenNode()).thenReturn(frozenNode2);

        when(frozenNode1.getPath()).thenReturn(FROZEN_NODE_1_PATH);
        when(frozenNode2.getPath()).thenReturn(FROZEN_NODE_2_PATH);
        when(frozenNode1.getProperty(TESTED_KEY)).thenReturn(jcrPropertyTitle1);
        when(frozenNode2.getProperty(TESTED_KEY)).thenReturn(jcrPropertyTitle2);
        when(frozenNode1.getName()).thenReturn(FROZEN_NODE_1_PATH);
        when(frozenNode2.getName()).thenReturn(FROZEN_NODE_2_PATH);

        when(frozenResource1.getValueMap()).thenReturn(frozenResource1ValueMap);
        when(frozenResource2.getValueMap()).thenReturn(frozenResource2ValueMap);
        when(frozenResource1.adaptTo(Node.class)).thenReturn(frozenNode1);
        when(frozenResource2.adaptTo(Node.class)).thenReturn(frozenNode2);
        when(frozenResource1.getChildren()).thenReturn(Collections.emptyList());
        when(frozenResource2.getChildren()).thenReturn(Collections.emptyList());

        when(jcrPropertyTitle1.getPath()).thenReturn(FROZEN_NODE_1_PATH + "/" + TESTED_KEY);
        when(jcrPropertyTitle2.getPath()).thenReturn(FROZEN_NODE_2_PATH + "/" + TESTED_KEY);
        when(jcrPropertyTitle1.getParent()).thenReturn(frozenNode1);
        when(jcrPropertyTitle2.getParent()).thenReturn(frozenNode2);
        when(jcrPropertyTitle1.isMultiple()).thenReturn(false);
        when(jcrPropertyTitle2.isMultiple()).thenReturn(false);
        when(jcrPropertyTitle1.getValue()).thenReturn(value1);
        when(jcrPropertyTitle2.getValue()).thenReturn(value2);

        when(value1.getString()).thenReturn(TESTED_VALUE_1);
        when(value1.getType()).thenReturn(PropertyType.STRING);
        when(value2.getString()).thenReturn(TESTED_VALUE_2);
        when(value2.getType()).thenReturn(PropertyType.STRING);

        evolutionContext = new EvolutionContextImpl(resource, config);
    }

    @Test
    public void getEvolutionItems() {
        assertEquals(3, evolutionContext.getEvolutionItems().size());
        // version 1
        assertEquals(VERSION_1, evolutionContext.getEvolutionItems().get(0).getVersionName());
        assertEquals(1, evolutionContext.getEvolutionItems().get(0).getVersionEntries().size());
        assertEquals(TESTED_VALUE_1, evolutionContext.getEvolutionItems()
                                                     .get(0)
                                                     .getVersionEntries()
                                                     .get(0)
                                                     .getValueString());
        // version 2
        assertEquals(VERSION_2, evolutionContext.getEvolutionItems().get(1).getVersionName());
        assertEquals(1, evolutionContext.getEvolutionItems().get(1).getVersionEntries().size());
        assertEquals(TESTED_VALUE_2, evolutionContext.getEvolutionItems()
                                                     .get(1)
                                                     .getVersionEntries()
                                                     .get(0)
                                                     .getValueString());
        // latest version
        assertEquals(LATEST_VERSION, evolutionContext.getEvolutionItems().get(2).getVersionName());
        assertEquals(TESTED_VALUE_2, evolutionContext.getEvolutionItems()
                                                     .get(2)
                                                     .getVersionEntries()
                                                     .get(0)
                                                     .getValueString());
    }

    @Test
    public void getVersions() {
        assertEquals(2, evolutionContext.getVersions().size());
    }
}
