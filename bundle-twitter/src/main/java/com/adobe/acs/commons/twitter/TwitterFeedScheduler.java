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
package com.adobe.acs.commons.twitter;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.RunnableOnMaster;

@Component(immediate = true, metatype = true, label = "ACS AEM Commons - Twitter Feed Refresh Scheduler", policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties(value = {
        @Property(name = "scheduler.expression", value = "0 0/15 * * * ?", label = "Twitter Feed Refresh interval (Quartz Cron Expression)"),
        @Property(name = "scheduler.concurrent", boolValue = false, propertyPrivate = true) })
public class TwitterFeedScheduler extends RunnableOnMaster {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TwitterFeedScheduler.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private TwitterFeedService twitterFeedService;

    @Property(value = "acs-commons/components/content/twitter-feed", label = "Twitter-feed component paths", unbounded = PropertyUnbounded.ARRAY)
    private static final String TWITTER_COMPONENT_PATHS = "twitter.component.paths";

    private String[] twitterComponentPaths = null;

    protected void activate(ComponentContext ctx) {
        final Dictionary<?, ?> props = ctx.getProperties();
        LOGGER.info("activate {}", props);

        twitterComponentPaths = PropertiesUtil.toStringArray(props
                .get(TWITTER_COMPONENT_PATHS));

    }

    @Override
    public void runOnMaster() {

        ResourceResolver resourceResolver = null;

        try {
            LOGGER.info("Master Instance, Running ACS AEM Commons Twitter Feed Scheduler");

            resourceResolver = resourceResolverFactory
                    .getAdministrativeResourceResolver(null);

            twitterFeedService.refreshTwitterFeed(resourceResolver,
                    twitterComponentPaths);

        } catch (Exception e) {
            LOGGER.error(
                    "Exception while running TwitterFeedScheduler, details", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
                resourceResolver = null;
            }
        }

    }

}
