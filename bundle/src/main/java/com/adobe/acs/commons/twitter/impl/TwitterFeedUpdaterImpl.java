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

import com.adobe.acs.commons.cqsearch.QueryUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.json.DataObjectFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(label = "ACS AEM Commons - Twitter Feed Update Service",
        metatype = true, description = "Service to update Twitter Feed components.")
@Service
public final class TwitterFeedUpdaterImpl implements TwitterFeedUpdater {

    private static final Logger log = LoggerFactory.getLogger(TwitterFeedUpdaterImpl.class);

    @Reference
    private Replicator replicator;

    @Reference
    private QueryBuilder queryBuilder;

    @Property(value = "acs-commons/components/content/twitter-feed", unbounded = PropertyUnbounded.ARRAY,
            label = "Twitter Feed component paths", description = "Component paths for Twitter Feed components.")
    private static final String TWITTER_COMPONENT_PATHS = "twitter.component.paths";

    private String[] twitterComponentPaths = null;

    protected void activate(ComponentContext ctx) {
        final Dictionary<?, ?> props = ctx.getProperties();

        twitterComponentPaths = PropertiesUtil.toStringArray(props.get(TWITTER_COMPONENT_PATHS));

    }

    @Override
    @SuppressWarnings("squid:S3776")
    public void updateTwitterFeedComponents(ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        List<Resource> twitterResources = findTwitterResources(resourceResolver);

        for (Resource twitterResource : twitterResources) {
            Page page = pageManager.getContainingPage(twitterResource);
            if (page != null) {
                Twitter client = page.adaptTo(Twitter.class);
                if (client != null) {
                    try {
                        ValueMap properties = twitterResource.getValueMap();
                        String username = properties.get("username", String.class);

                        if (!StringUtils.isEmpty(username)) {

                            log.info("Loading Twitter timeline for user {} for component {}.", username,
                                    twitterResource.getPath());

                            List<Status> statuses = client.getUserTimeline(username);

                            if (statuses != null) {
                                List<String> tweetsList = new ArrayList<>(statuses.size());
                                List<String> jsonList = new ArrayList<>(statuses.size());

                                for (Status status : statuses) {
                                    tweetsList.add(processTweet(status));
                                    jsonList.add(DataObjectFactory.getRawJSON(status));
                                }

                                if (!tweetsList.isEmpty()) {
                                    ModifiableValueMap map = twitterResource.adaptTo(ModifiableValueMap.class);
                                    map.put("tweets", tweetsList.toArray(new String[tweetsList.size()]));
                                    map.put("tweetsJson", jsonList.toArray(new String[jsonList.size()]));
                                    twitterResource.getResourceResolver().commit();

                                    handleReplication(twitterResource);
                                }
                            }

                        }
                    } catch (PersistenceException e) {
                        log.error("Exception while updating twitter feed on resource:" + twitterResource.getPath(), e);
                    } catch (ReplicationException e) {
                        log.error("Exception while replicating twitter feed on resource:" + twitterResource.getPath(),
                                e);
                    } catch (TwitterException e) {
                        log.error("Exception while loading twitter feed on resource:" + twitterResource.getPath(),
                                e);
                    }
                } else {
                    log.warn("Twitter component found on {}, but page cannot be adapted to Twitter API. Check Cloud SErvice configuration", page.getPath());
                }
            }

        }

    }

    private List<Resource> findTwitterResources(ResourceResolver resourceResolver) {
        List<Resource> twitterResources = new ArrayList<>();

        Map<String, String> predicateMap = new HashMap<>();
        predicateMap.put("path", "/content");
        predicateMap.put("property", "sling:resourceType");

        int counter = 1;
        for (String path : twitterComponentPaths) {
            predicateMap.put("property." + (counter++) + "_value", path);

        }

        predicateMap.put("p.limit", "-1");

        Query query = queryBuilder.createQuery(PredicateGroup.create(predicateMap), resourceResolver.adaptTo(Session.class));
        QueryUtil.setResourceResolverOn(resourceResolver, query);

        SearchResult result = query.getResult();
        Iterator<Resource> resources = result.getResources();
        while (resources.hasNext()) {
            twitterResources.add(resourceResolver.getResource(resources.next().getPath()));
        }
        return twitterResources;
    }

    private String processTweet(Status status) {
        String tweet = status.getText();

        for (URLEntity entity : status.getURLEntities()) {
            String url = String.format("<a target=\"_blank\" href=\"%s\">%s</a>", entity.getURL(), entity.getURL());
            tweet = tweet.replace(entity.getURL(), url);
        }

        return tweet;

    }

    private void handleReplication(Resource twitterResource) throws ReplicationException {
        if (isReplicationEnabled(twitterResource)) {
            Session session = twitterResource.getResourceResolver().adaptTo(Session.class);
            replicator.replicate(session, ReplicationActionType.ACTIVATE, twitterResource.getPath());
        }

    }

    private boolean isReplicationEnabled(Resource twitterResource) {
        ValueMap properties = twitterResource.getValueMap();
        return properties.get("replicate", false);
    }

}
