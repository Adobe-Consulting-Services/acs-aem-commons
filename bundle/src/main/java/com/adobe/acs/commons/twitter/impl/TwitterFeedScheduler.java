/*
 * #%L
 * ACS AEM Commons Twitter Support Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.twitter.impl;


import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.discovery.TopologyEventListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.RunnableOnMaster;

import java.util.Collections;
import java.util.Map;

@Component(immediate = true, service= {TopologyEventListener.class, Runnable.class},
configurationPolicy=ConfigurationPolicy.REQUIRE, property= {
 "scheduler.concurrent" + "=" + "false"
})
@Designate(ocd=TwitterFeedScheduler.Config.class)
public final class TwitterFeedScheduler extends RunnableOnMaster {

    private static final Logger log = LoggerFactory.getLogger(TwitterFeedScheduler.class);

    private static final String SERVICE_NAME = "twitter-updater";
    private static final Map<String, Object> AUTH_INFO;
    
    @ObjectClassDefinition(name= "ACS AEM Commons - Twitter Feed Refresh Scheduler",
        description = "Schedule job which refreshes Twitter Feed components on a recurring basis")
    public @interface Config {
        @AttributeDefinition( defaultValue = "0 0/15 * * * ?", name = "Refresh Interval",
                description = "Twitter Feed Refresh interval (Quartz Cron Expression)")
        String scheduler_expression();

    }

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private TwitterFeedUpdater twitterFeedService;

    @Override
    public void runOnMaster() {

        ResourceResolver resourceResolver = null;

        try {
            log.debug("Master Instance, Running ACS AEM Commons Twitter Feed Scheduler");

            resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);

            twitterFeedService.updateTwitterFeedComponents(resourceResolver);

        } catch (Exception e) {
            log.error(
                    "Exception while running TwitterFeedScheduler.", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
                resourceResolver = null;
            }
        }

    }

}
