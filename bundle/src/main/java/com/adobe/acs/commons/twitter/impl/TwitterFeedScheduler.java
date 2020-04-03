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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.RunnableOnMaster;

import java.util.Collections;
import java.util.Map;

@Component(immediate = true, metatype = true,
    label = "ACS AEM Commons - Twitter Feed Refresh Scheduler",
    description = "Schedule job which refreshes Twitter Feed components on a recurring basis",
    policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties(value = {
        @Property(name = "scheduler.expression", value = "0 0/15 * * * ?", label = "Refresh Interval",
                description = "Twitter Feed Refresh interval (Quartz Cron Expression)"),
        @Property(name = "scheduler.concurrent", boolValue = false, propertyPrivate = true) })
public final class TwitterFeedScheduler extends RunnableOnMaster {

    private static final Logger log = LoggerFactory.getLogger(TwitterFeedScheduler.class);

    private static final String SERVICE_NAME = "twitter-updater";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private TwitterFeedUpdater twitterFeedService;

    @Override
    public void runOnMaster() {

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO) ){
            log.debug("Master Instance, Running ACS AEM Commons Twitter Feed Scheduler");

            twitterFeedService.updateTwitterFeedComponents(resourceResolver);

        } catch (Exception e) {
            log.error(
                    "Exception while running TwitterFeedScheduler.", e);
        } 

    }

}
