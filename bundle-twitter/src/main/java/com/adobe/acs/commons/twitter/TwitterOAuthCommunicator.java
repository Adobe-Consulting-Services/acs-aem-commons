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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterOAuthCommunicator {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TwitterOAuthCommunicator.class);

    private TwitterFactory twitterFactory;

    private Twitter twitter;

    public TwitterOAuthCommunicator() {
        init();
    }

    private void init() {
        ConfigurationBuilder configurationBuilder = getConfigurationBuilder();

        twitterFactory = new TwitterFactory(configurationBuilder.build());
    }

    private ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setUseSSL(true);
        configurationBuilder.setApplicationOnlyAuthEnabled(true);
        return configurationBuilder;
    }

    public List<String> getTweetsAsList(
            TwitterConfiguration twitterConfiguration) {

        String userName = twitterConfiguration.getUsername();

        List<String> tweetsList = new ArrayList<String>();

        twitter = twitterFactory.getInstance();

        twitter.setOAuthConsumer(twitterConfiguration.getConsumerKey(),
                twitterConfiguration.getConsumerSecret());

        try {

            LOGGER.info("Loading Twitter time line for user {}", userName);

            twitter.getOAuth2Token();
            List<Status> statuses = twitter.getUserTimeline(userName);

            if (statuses != null && statuses.size() > 0) {
                for (Status status : statuses) {
                    tweetsList.add(processTweet(status));
                }

            }

        } catch (TwitterException te) {
            LOGGER.error("Error occured while fetching tweets for user:"
                    + userName, te);
        }

        return tweetsList;
    }

    private String processTweet(Status status) {
        String tweet = status.getText();

        for (URLEntity entity : status.getURLEntities()) {
            String url = "<a target=\"_blank\" href=\"" + entity.getURL()
                    + "\">" + entity.getURL() + "</a>";
            tweet = tweet.replace(entity.getURL(), url);
        }

        return tweet;

    }

}
