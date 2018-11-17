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

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ResourceServiceManager;
import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicatorMBean;
import com.adobe.acs.commons.replication.packages.automatic.model.AutomaticPackageReplicatorModel;
import com.adobe.acs.commons.replication.packages.automatic.model.AutomaticPackageReplicatorModel.TRIGGER;
import com.day.cq.replication.Replicator;

/**
 * Listens to changes under /etc/acs-commons/automatic-package-replication and
 * manages the Automatic Package Replicator jobs based on the updates.
 */
@Component(immediate = true)
@Service(value = { EventHandler.class, AutomaticPackageReplicatorMBean.class })
@Properties({
            // TODO: Register a Resource Change Listener instead as per the deprecation notes
            // https://sling.apache.org/apidocs/sling9/org/apache/sling/api/resource/observation/ResourceChangeListener.html
        @Property(name = EventConstants.EVENT_TOPIC, value = { SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED }),
        @Property(name = "jmx.objectname", value = "com.adobe.acs.commons:type=Automatic Package Replicator"),
        @Property(name = EventConstants.EVENT_FILTER, value = "(path=/etc/acs-commons/automatic-package-replication/*/jcr:content)") })
public class ConfigurationUpdateListener extends ResourceServiceManager
        implements EventHandler, AutomaticPackageReplicatorMBean {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationUpdateListener.class);

    private static final String ROOT_PATH = "/etc/acs-commons/automatic-package-replication";

    private static final String TRIGGER_KEY = "trigger.name";

    private static final String SERVICE_NAME = "automatic-package-replicator";

    private static final Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
            (Object) SERVICE_NAME);

    /**
     * Creating this as a separate method to make migrating to service users
     * easier. Callers of this method must ensure the resource resolver is
     * closed.
     *
     * @param factory
     *            the resource resolver factory to use for getting the resource
     *            resolver
     * @return the resource resolver or null if there is an exception allocating
     *         the resource resolver
     */
    static final ResourceResolver getResourceResolver(ResourceResolverFactory factory) {
        ResourceResolver resolver = null;
        try {
            resolver = factory.getServiceResourceResolver(AUTH_INFO);

        } catch (LoginException e) {
            log.error("Exception allocating resource resolver", e);
        }
        return resolver;
    }

    @Override
    protected ResourceResolver getResourceResolver() {
        return getResourceResolver(resourceResolverFactory);
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private Replicator replicator;

    @Reference
    private EventAdmin eventAdmin;

    public ConfigurationUpdateListener() throws NotCompliantMBeanException {
        super(AutomaticPackageReplicatorMBean.class);
    }

    protected ConfigurationUpdateListener(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    @Override
    public void execute(String id) {
        AutomaticPackageReplicatorJob job = (AutomaticPackageReplicatorJob) getBundleContext()
                .getService(super.getRegisteredServices().get(id).getReference());
        job.run();
    }

    @Override
    @SuppressWarnings("squid:S3923")
    protected boolean isServiceUpdated(Resource config, ServiceReference reference) {
        boolean updated = false;
        AutomaticPackageReplicatorModel model = new AutomaticPackageReplicatorModel(config);
        String triggerStr = (String) reference.getProperty(TRIGGER_KEY);
        if (model.getTrigger() == TRIGGER.cron && model.getTrigger() == TRIGGER.valueOf(triggerStr) && ObjectUtils
                .equals(reference.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION), model.getCronTrigger())) {
            updated = true;
        } else if (model.getTrigger() == TRIGGER.event && model.getTrigger() == TRIGGER.valueOf(triggerStr)
                && ObjectUtils.equals(reference.getProperty(EventConstants.EVENT_TOPIC), model.getEventTopic())
                && ObjectUtils.equals(reference.getProperty(EventConstants.EVENT_FILTER), model.getEventFilter())) {
            updated = true;
        }
        return updated;
    }

    @Override
    public String getRootPath() {
        return ROOT_PATH;
    }

    @Override
    protected ServiceRegistration registerServiceObject(Resource config, Hashtable<String, Object> props) {
        AutomaticPackageReplicatorModel model = new AutomaticPackageReplicatorModel(config);
        AutomaticPackageReplicatorJob job = new AutomaticPackageReplicatorJob(resourceResolverFactory, replicator,
                eventAdmin, model.getPackagePath());
        ServiceRegistration serviceRegistration = null;
        props.put(TRIGGER_KEY, model.getTrigger().name());
        if (AutomaticPackageReplicatorModel.TRIGGER.cron == model.getTrigger()) {
            if(StringUtils.isEmpty(model.getCronTrigger())){
                throw new IllegalArgumentException("No cron trigger specified");
            }
            props.put(Scheduler.PROPERTY_SCHEDULER_EXPRESSION, model.getCronTrigger());
            log.debug("Registering cron runner with: {}", props);
            serviceRegistration = super.getBundleContext().registerService(Runnable.class.getCanonicalName(), job,
                    props);
        } else {
            if(StringUtils.isEmpty(model.getEventTopic())){
                throw new IllegalArgumentException("No event topic specified");
            }
            props.put(EventConstants.EVENT_TOPIC, new String[] { model.getEventTopic() });
            if (StringUtils.isNotEmpty(model.getEventFilter())) {
                props.put(EventConstants.EVENT_FILTER, model.getEventFilter());
            }
            log.debug("Registering event handler runner with: {}", props);
            serviceRegistration = super.getBundleContext().registerService(EventHandler.class.getCanonicalName(), job,
                    props);
        }
        return serviceRegistration;
    }
}
