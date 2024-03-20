/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.wcm.components.impl;

import com.adobe.acs.commons.wcm.components.TwitterFeedModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {TwitterFeedModel.class},
        resourceType = {"acs-commons/components/content/twitter-feed"},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class TwitterFeedModelImpl implements TwitterFeedModel {

    @ValueMapValue
    private String username;

    @ValueMapValue
    @Default(intValues = 0)
    private int limit;

    @ValueMapValue
    private List<String> tweets;

    @PostConstruct
    protected void init() {
        if (tweets == null) {
            tweets = new ArrayList<>();
        }
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public List<String> getTweets() {
        List<String> limitedTweets = new ArrayList<>(tweets);

        if (getLimit() > 0 && getLimit() < tweets.size()) {
            limitedTweets = tweets.subList(0, getLimit());
        }

        return limitedTweets;
    }

    @Override
    public boolean isReady() {
        return StringUtils.isNotEmpty(getUsername()) && !getTweets().isEmpty();
    }
}
