/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.replication.packages.automatic.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.management.NotCompliantMBeanException;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.hc.api.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class AutomaticPackageReplicatorTest {

  private AutomaticPackageReplicatorImpl apr;
  private EventAdmin eventAdmin;
  private Replicator replicator;
  public static final String PACKAGE_PATH = "/etc/packages/test";
  public static final String PACKAGE_ID = "test";

  @Before
  public void init() throws NotCompliantMBeanException, LoginException, RepositoryException {
    apr = new AutomaticPackageReplicatorImpl();
    eventAdmin = Mockito.mock(EventAdmin.class);
    apr.setEventAdmin(eventAdmin);

    final ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
    Mockito.when(resolver.adaptTo(Session.class)).thenReturn(Mockito.mock(Session.class));
    final ResourceResolverFactory factory = Mockito.mock(ResourceResolverFactory.class);
    Mockito.when(factory.getServiceResourceResolver(Mockito.anyMap())).thenReturn(resolver);
    apr.setResourceResolverFactory(factory);

    final JcrPackageManager packageMgr = Mockito.mock(JcrPackageManager.class);
    Mockito.when(packageMgr.open(Mockito.any(PackageId.class))).thenAnswer((in) -> {
      final PackageId id = (PackageId) in.getArguments()[0];
      if (id.getInstallationPath().equals(PACKAGE_PATH)) {
        JcrPackage pkg = Mockito.mock(JcrPackage.class);
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getPath()).thenReturn(PACKAGE_PATH);
        Mockito.when(pkg.getNode()).thenReturn(node);
        return pkg;
      }
      return null;
    });
    final Packaging packaging = Mockito.mock(Packaging.class);
    Mockito.when(packaging.getPackageManager(Mockito.any(Session.class))).thenReturn(packageMgr);
    apr.setPackaging(packaging);

    replicator = Mockito.mock(Replicator.class);
    apr.setReplicator(replicator);
  }

  @Test
  public void testReplicate()
      throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {
    apr.replicatePackage(PACKAGE_ID);

    Mockito.verify(eventAdmin).postEvent(Mockito.any(Event.class));
    Mockito.verify(replicator).replicate(Mockito.any(Session.class), Mockito.eq(ReplicationActionType.ACTIVATE),
        Mockito.eq(PACKAGE_PATH));

  }

  @Test
  public void testInvalid()
      throws PackageException, IOException, ReplicationException, LoginException, RepositoryException {
    try {
      apr.replicatePackage("test2");
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Mockito.verify(eventAdmin).postEvent(Mockito.any(Event.class));
    }
  }

  @Test
  public void testRecent()
      throws PackageException, IOException, ReplicationException, LoginException, RepositoryException {
    apr.replicatePackage(PACKAGE_ID);
    assertEquals(1, apr.getRecentReplications().length);
    assertTrue(apr.getResultLog().getAggregateStatus() == Result.Status.OK);
  }
}