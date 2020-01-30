/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.wcm.comparisons.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.acs.commons.wcm.comparisons.VersionService;
import com.google.common.collect.ImmutableMap;

public final class VersionServiceImplTest {

    private static final String MANY = "manyVersions";
    private static final String ONE = "oneVersion";

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private final VersionService underTest = new VersionServiceImpl();
    
    Version oneVersionVersion;
    Version manyVersionVersion;
    
    @Before
    public void setup() throws Exception {
        Map<String, Object> props = ImmutableMap.of("jcr:primaryType", "nt:unstructured");
        ResourceResolver rr = context.resourceResolver();
        Resource root = rr.getResource("/");
        Resource manyVersions = rr.create(root, "manyVersions", props);
        Resource oneVersion = rr.create(root, "oneVersion", props);
        Resource noVersion = rr.create(root, "noVersion", props);

        manyVersions.adaptTo(Node.class).addMixin("mix:versionable");
        oneVersion.adaptTo(Node.class).addMixin("mix:versionable");

        rr.commit();

        Session session = context.resourceResolver().adaptTo(Session.class);
        VersionManager vmgr = session.getWorkspace().getVersionManager();
        oneVersionVersion = vmgr.checkin(oneVersion.getPath());
        vmgr.checkout(oneVersion.getPath());

        for (int i = 0; i < 10; i++) {
            manyVersionVersion = vmgr.checkin(manyVersions.getPath());
            vmgr.checkout(manyVersions.getPath());
        }

    }
    
    @Test
    public void lastVersion_oneVersion_returnVersion() throws Exception {
        Version lastVersion = underTest.lastVersion(context.resourceResolver().getResource("/oneVersion"));
        assertEquals(lastVersion.getUUID(), oneVersionVersion.getUUID());
    }

    @Test
//    @Ignore
    public void lastVersion_noElement_returnNull() throws Exception {
        Version lastVersion = underTest.lastVersion(context.resourceResolver().getResource("/noVersion"));
        assertNull(lastVersion);
    }

    @Test
//    @Ignore
    public void lastVersion_listOfVersion_returnLast() throws Exception {
        Version lastVersion = underTest.lastVersion(context.resourceResolver().getResource("/manyVersions"));
        assertEquals(lastVersion.getUUID(), manyVersionVersion.getUUID());
    }

}