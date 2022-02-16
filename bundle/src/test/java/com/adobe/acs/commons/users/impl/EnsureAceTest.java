/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.users.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnsureAceTest {
    private static final Logger LOG = LoggerFactory.getLogger(EnsureAceTest.class);
    private static final String USER_HOME = "/rep:security/rep:authorizables/rep:users";
    private static final String GROUP_HOME = "/rep:security/rep:authorizables/rep:groups";

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Mock
    QueryBuilder queryBuilder;

    @Mock
    Query query;

    @Mock
    SearchResult result;

    @Before
    public void setUp() throws Exception {
        context.registerService(QueryBuilder.class, queryBuilder);

        when(result.getHits()).thenReturn(Collections.emptyList());
        when(query.getResult()).thenReturn(result);
        when(queryBuilder.createQuery(any(PredicateGroup.class), any(Session.class))).thenReturn(query);
    }

    @Test
    public void testEnsureAces() throws Exception {
        final EnsureAce ensureAce = context.registerInjectActivateService(new EnsureAce());

        context.build().resource("/content", "jcr:primaryType", "nt:unstructured").commit();

        JackrabbitSession session = (JackrabbitSession) context.resourceResolver().adaptTo(Session.class);

        UserManager userManager = session.getUserManager();

        final User testUser = userManager.createSystemUser("testuser", USER_HOME + "/system");
        final User testUser2 = userManager.createSystemUser("testuser2", USER_HOME + "/system");
        final org.apache.jackrabbit.api.security.user.Group testGroup = userManager.createGroup("testgroup");

        Map<String, Object> initConfig = new HashMap<>();
        initConfig.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "testuser");
        initConfig.put(EnsureServiceUser.PROP_ACES, new String[]{"type=allow;privileges=jcr:read;path=/content;rep:ntNames=nt:unstructured"});
        ServiceUser initServiceUser = new ServiceUser(initConfig);
        assertEquals("no failures on init", 0, ensureAce.ensureAces(context.resourceResolver(), testUser, initServiceUser));

        Resource repPolicy = context.resourceResolver().getResource("/content/rep:policy");
        assertNotNull("new rep:policy node should exist", repPolicy);
        Resource newAce = repPolicy.hasChildren() ? repPolicy.listChildren().next() : null;
        assertNotNull("new allow node should exist", newAce);

        Hit mockHit = mock(Hit.class);
        when(mockHit.getPath()).thenThrow(new RepositoryException("no more storage on cloud!"));

        final List<Hit> hits = Collections.singletonList(mockHit);
        when(result.getHits()).thenReturn(hits);

        Map<String, Object> init2Config = new HashMap<>();
        init2Config.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "testuser2");
        init2Config.put(EnsureServiceUser.PROP_ACES, new String[]{"type=allow;privileges=jcr:read;path=/nocontent;"});
        ServiceUser init2ServiceUser = new ServiceUser(init2Config);
        assertEquals("no failures on init, but trigger exceptions", 1, ensureAce.ensureAces(context.resourceResolver(), testUser2, init2ServiceUser));

        doReturn(newAce.getPath()).when(mockHit).getPath();
        init2Config.put(EnsureServiceUser.PROP_ACES, new String[]{"type=allow;privileges=jcr:read;path=/content;rep:glob=*;"});
        init2ServiceUser = new ServiceUser(init2Config);
        assertEquals("no failures on init", 0, ensureAce.ensureAces(context.resourceResolver(), testUser2, init2ServiceUser));


        Iterator<Resource> nextAces = repPolicy.hasChildren() ? repPolicy.listChildren() : Collections.emptyIterator();
        assertTrue("new allow nodes should exist", nextAces.hasNext());

        List<Hit> nextHits = new ArrayList<>();
        while (nextAces.hasNext()) {
            Hit nextHit = mock(Hit.class);
            Resource nextAce = nextAces.next();;
        }

        when(result.getHits()).thenReturn(nextHits);

        Map<String, Object> nextConfig = new HashMap<>();
        nextConfig.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "testuser");
        nextConfig.put(EnsureServiceUser.PROP_ACES, new String[]{"type=allow;privileges=jcr:all;path=/content;rep:itemNames=config;rep:prefixes=foo;"});
        ServiceUser nextServiceUser = new ServiceUser(nextConfig);
        assertEquals("no failures on next",
                0, ensureAce.ensureAces(context.resourceResolver(), testUser, nextServiceUser));

        Map<String, Object> groupConfig = new HashMap<>();
        groupConfig.put(EnsureServiceUser.PROP_PRINCIPAL_NAME, "testgroup");
        groupConfig.put(EnsureServiceUser.PROP_ACES, new String[]{"type=allow;privileges=jcr:read;path=/content;rep:glob=;"});
        Group ensureGroup = new Group(groupConfig);
        assertEquals("no failures on group",
                0, ensureAce.ensureAces(context.resourceResolver(), testGroup, ensureGroup));

    }
}
