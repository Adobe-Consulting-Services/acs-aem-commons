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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.packages.automatic.AbstractAutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.impl.EventBasedAutomaticPackageReplicator.EventBasedAutomaticPackageReplicatorConfig;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

/**
 * Implementation of the Automatic Package Replicator to be triggered by an OSGi
 * Event.
 */
@Component(service = { EventHandler.class }, immediate = true)
@Designate(ocd = EventBasedAutomaticPackageReplicatorConfig.class, factory = true)
public class EventBasedAutomaticPackageReplicator extends AbstractAutomaticPackageReplicator implements EventHandler {

  @ObjectClassDefinition(name = "Event-Based Automatic Package Replicator")
  public @interface EventBasedAutomaticPackageReplicatorConfig {

    @SuppressWarnings({ "squid:S00100" })
    @AttributeDefinition(name = "Event Topic")
    String event_topics();

    @AttributeDefinition(name = "Package Path")
    String packagePath();
  }

  private static final Logger log = LoggerFactory.getLogger(EventBasedAutomaticPackageReplicator.class);

  private EventBasedAutomaticPackageReplicatorConfig config;

  @Reference
  private EventAdmin eventAdmin;

  @Reference
  private Packaging packaging;

  @Reference
  private Replicator replicator;

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  private ServiceRegistration<AutomaticPackageReplicator> serviceReference;

  public EventBasedAutomaticPackageReplicator() throws NotCompliantMBeanException {
    super();
  }

  @Activate
  public void activate(EventBasedAutomaticPackageReplicatorConfig config, ComponentContext context) {
    log.info("activate");
    this.config = config;

    @SuppressWarnings({ "squid:S1149" })
    Dictionary<String, String> mbeanProps = new Hashtable<>();
    mbeanProps.put("jmx.objectname",
        "com.adobe.acs.commons.replication.packages.automatic:type=Event-Based Automatic Package Replicator,event="
            + ObjectName.quote(config.event_topics()) + ",package=" + ObjectName.quote(config.packagePath()));
    serviceReference = context.getBundleContext().registerService(AutomaticPackageReplicator.class, this, mbeanProps);
    log.info("Registered service {}", serviceReference);
  }

  @Deactivate
  public void deactivate(ComponentContext context) {
    log.info("deactivate");
    if (serviceReference != null) {
      log.info("Unregistering service {}", serviceReference);
      serviceReference.unregister();
    } else {
      log.info("No service to unregister");
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
  public void handleEvent(Event event) {
    try {
      this.replicatePackage();
    } catch (RepositoryException | PackageException | IOException | ReplicationException e) {
      log.error("Failed to replicate package {}", config.packagePath(), e);
    }
  }

  @Override
  public void replicatePackage() throws RepositoryException, PackageException, IOException, ReplicationException {
    try (ResourceResolver resolver = getResourceResolver()) {
      super.doReplicatePackage(packaging.getPackageManager(resolver.adaptTo(Session.class)));
    }
  }
  

  @Override
  public String toString() {
    return "EventBasedAutomaticPackageReplicator [event=" + config.event_topics() + ",package="
        + config.packagePath() + "]";
  }

}
