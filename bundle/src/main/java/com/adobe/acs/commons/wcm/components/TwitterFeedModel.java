package com.adobe.acs.commons.wcm.components;

import org.osgi.annotation.versioning.ProviderType;

import java.util.List;

@ProviderType
public interface TwitterFeedModel {
    String getUsername();

    int getLimit();

    List<String> getTweets();

    boolean isReady();

}
