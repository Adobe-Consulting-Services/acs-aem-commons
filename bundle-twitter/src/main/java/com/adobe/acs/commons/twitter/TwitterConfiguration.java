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

import org.apache.commons.lang.StringUtils;

public class TwitterConfiguration {

    private String username;

    private String consumerKey;

    private String consumerSecret;

    public TwitterConfiguration() {

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String accessToken) {
        this.consumerKey = accessToken;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String accessTokenSecret) {
        this.consumerSecret = accessTokenSecret;
    }

    public boolean isValid() {
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(consumerKey)
                && !StringUtils.isEmpty(consumerSecret))
            return true;

        return false;

    }

}
