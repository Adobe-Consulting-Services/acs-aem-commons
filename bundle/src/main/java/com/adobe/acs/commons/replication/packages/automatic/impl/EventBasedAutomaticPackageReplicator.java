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

import javax.jcr.RepositoryException;

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.impl.EventBasedAutomaticPackageReplicator.EventBasedAutomaticPackageReplicatorConfig;
import com.day.cq.replication.ReplicationException;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.LoginException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Automatic Package Replicator to be triggered by an OSGi
 * Event.
 */
@Component(service = { EventHandler.class }, immediate = true)
@Designate(ocd = EventBasedAutomaticPackageReplicatorConfig.class, factory = true)
public class EventBasedAutomaticPackageReplicator implements EventHandler {

  @ObjectClassDefinition(name = "ACS AEM Commons - Automatic Package Replicator - Event-Based")
  public @interface EventBasedAutomaticPackageReplicatorConfig {

    @SuppressWarnings({ "squid:S00100" })
    @AttributeDefinition(name = "Event Topic", required = true)
    String event_topics();

    @AttributeDefinition(name = "Package Path", required = true)
    String packagePath();

    String webconsole_configurationFactory_nameHint() default "Event-Based Automatic Package Replicator <b>{event.topics}->{packagePath}</b>";
  }

  private static final Logger log = LoggerFactory.getLogger(EventBasedAutomaticPackageReplicator.class);

  private EventBasedAutomaticPackageReplicatorConfig config;

  private AutomaticPackageReplicator automaticPackageReplicator;

  @Activate
  public void activate(EventBasedAutomaticPackageReplicatorConfig cfg) {
    this.config = cfg;
  }

  @Override
  public void handleEvent(Event event) {
    try {
      automaticPackageReplicator.replicatePackage(config.packagePath());
    } catch (RepositoryException | PackageException | IOException | ReplicationException | LoginException e) {
      log.error("Failed to replicate package {}", config.packagePath(), e);
    }
  }

  @Reference
  public void setAutomaticPackageReplicator(AutomaticPackageReplicator automaticPackageReplicator) {
    this.automaticPackageReplicator = automaticPackageReplicator;
  }
}
