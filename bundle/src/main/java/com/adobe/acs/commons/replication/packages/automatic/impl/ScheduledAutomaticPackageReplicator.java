/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 - Adobe
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

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.packages.automatic.AbstractAutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.impl.ScheduledAutomaticPackageReplicator.ScheduledAutomaticPackageReplicatorConfig;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

@Component(service = { Runnable.class })
@Designate(ocd = ScheduledAutomaticPackageReplicatorConfig.class, factory = true)
public class ScheduledAutomaticPackageReplicator extends AbstractAutomaticPackageReplicator implements Runnable {

  @ObjectClassDefinition(name = "Scheduled Automatic Package Replicator")
  public @interface ScheduledAutomaticPackageReplicatorConfig {

    @AttributeDefinition(name = "Package Path")
    String packagePath();

    @AttributeDefinition(name = "Scheduler Expression")
    String scheduler_expression();

  }

  private static final Logger log = LoggerFactory.getLogger(ScheduledAutomaticPackageReplicator.class);

  private ScheduledAutomaticPackageReplicatorConfig config;

  @Reference
  private EventAdmin eventAdmin;

  @Reference
  private Packaging packaging;

  @Reference
  private Replicator replicator;

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  private ServiceRegistration<AutomaticPackageReplicator> serviceReference;

  public ScheduledAutomaticPackageReplicator() throws NotCompliantMBeanException {
    super();
  }

  @Activate
  public void activate(ScheduledAutomaticPackageReplicatorConfig config, ComponentContext context) {
    log.info("activate");
    this.config = config;

    @SuppressWarnings({ "squid:S1149" })
    Dictionary<String, String> mbeanProps = new Hashtable<>();
    mbeanProps.put("jmx.objectname",
        "com.adobe.acs.commons.replication.packages.automatic:type=Scheduled Automatic Package Replicator,schedule="
            + ObjectName.quote(config.scheduler_expression()) + ",package=" + ObjectName.quote(config.packagePath()));
    serviceReference = context.getBundleContext().registerService(AutomaticPackageReplicator.class, this, mbeanProps);
    log.info("Registered service {}", serviceReference);
  }

  @Deactivate
  public void deactivate(ComponentContext context) {
    log.info("activate");
    if (serviceReference != null) {
      log.info("Unregistering service {}", serviceReference);
      serviceReference.unregister();
    }
  }

  @Override
  public EventAdmin getEventAdmin() {
    return eventAdmin;
  }

  @Override
  public String getPackagePath() {
    return config.packagePath();
  }

  @Override
  public Replicator getReplicator() {
    return replicator;
  }

  public ResourceResolver getResourceResolver() {
    try {
      return resourceResolverFactory.getServiceResourceResolver(Collections
          .singletonMap(ResourceResolverFactory.SUBSERVICE, AbstractAutomaticPackageReplicator.SERVICE_USER_NAME));
    } catch (LoginException e) {
      throw new RuntimeException("Failed to get service resolver", e);
    }
  }

  @Override
  public void replicatePackage() throws RepositoryException, PackageException, IOException, ReplicationException {
    try (ResourceResolver resolver = getResourceResolver()) {
      super.doReplicatePackage(packaging.getPackageManager(resolver.adaptTo(Session.class)));
    }
  }

  @Override
  public void run() {
    try {
      this.replicatePackage();
    } catch (RepositoryException | PackageException | IOException | ReplicationException e) {
      log.error("Failed to replicate package {}", config.packagePath(), e);
    }
  }

  @Override
  public String toString() {
    return "ScheduledAutomaticPackageReplicator [schedule=" + config.scheduler_expression() + ",package="
        + config.packagePath() + "]";
  }

}
