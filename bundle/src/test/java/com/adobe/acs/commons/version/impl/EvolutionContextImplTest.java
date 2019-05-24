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

import static com.adobe.acs.commons.version.impl.CurrentEvolutionImpl.LATEST_VERSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.version.EvolutionContext;

@RunWith(MockitoJUnitRunner.class)
public final class EvolutionContextImplTest {

    private static final String RESOURCE_PATH = "/path/to/resource";

    private static final String FROZEN_NODE_1_PATH = "/path/to/first/jcr:frozenNode";
    private static final String FROZEN_NODE_2_PATH = "/path/to/second/jcr:frozenNode";

    private static final String VERSION_1 = "version-1";
    private static final String VERSION_2 = "version-2";

    private static final String TESTED_VALUE_1 = "old";
    private static final String TESTED_VALUE_2 = "new";

    @Mock
    private Resource resource;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Workspace workspace;

    @Mock
    private Session session;

    @Mock
    private VersionManager versionManager;

    @Mock
    private VersionHistory versionHistory;

    @Mock
    private VersionIterator versionIterator;

    private final EvolutionConfig config = new EvolutionConfig(new String[] { "foo" }, new String[] { "bar" });

    private EvolutionContext evolutionContext;

    @Before
    public void setUp() throws RepositoryException {
    	final FrozenResourceMock frozenResource1 = new FrozenResourceMock(FROZEN_NODE_1_PATH, VERSION_1, TESTED_VALUE_1);
    	final FrozenResourceMock frozenResource2 = new FrozenResourceMock(FROZEN_NODE_2_PATH, VERSION_2, TESTED_VALUE_2);
        when(resource.getPath()).thenReturn(RESOURCE_PATH);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resource.getValueMap()).thenReturn(frozenResource2.getValueMap());
        when(resource.adaptTo(Node.class)).thenReturn(frozenResource2.getNode());
        when(resource.getChildren()).thenReturn(Collections.emptyList());

        when(resolver.adaptTo(Session.class)).thenReturn(session);
        when(resolver.resolve(eq(FROZEN_NODE_1_PATH))).thenReturn(frozenResource1.getResource());
        when(resolver.resolve(eq(FROZEN_NODE_2_PATH))).thenReturn(frozenResource2.getResource());

        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getVersionManager()).thenReturn(versionManager);
        when(versionManager.getVersionHistory(eq(RESOURCE_PATH))).thenReturn(versionHistory);
        when(versionHistory.getAllVersions()).thenReturn(versionIterator);

        when(versionIterator.hasNext()).thenReturn(true, true, false);
        when(versionIterator.next()).thenReturn(frozenResource1.getVersion(), frozenResource2.getVersion());

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
