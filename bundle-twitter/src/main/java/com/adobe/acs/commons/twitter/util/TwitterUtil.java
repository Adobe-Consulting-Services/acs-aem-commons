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
package com.adobe.acs.commons.twitter.util;

import java.util.ArrayList;
import java.util.List;

public class TwitterUtil {

    public static String[] filterTwitterFeeds(String[] tweets,
            String[] filterWords, int noOfTweets) {

        List<String> filteredTweets = new ArrayList<String>();

        if (!isNull(tweets)) {
            for (String tweet : tweets) {

                filterTweets(filterWords, filteredTweets, tweet);
            }

            if (noOfTweets > 0) {
                int size = (noOfTweets > filteredTweets.size()) ? filteredTweets
                        .size() : noOfTweets;
                filteredTweets = filteredTweets.subList(0, size);

            }

            tweets = filteredTweets.toArray(new String[filteredTweets.size()]);

        }

        return tweets;

    }

    private static void filterTweets(String[] filterWords,
            List<String> filteredTweets, String tweet) {
        if (!isNull(filterWords)) {
            for (String word : filterWords) {
                if (tweet.toLowerCase().contains(word.toLowerCase())) {
                    filteredTweets.add(tweet);
                    break;
                }
            }
        } else {
            filteredTweets.add(tweet);
        }
    }

    private static boolean isNull(String[] strArr) {

        if (strArr != null && strArr.length > 0)
            return false;
        else
            return true;

    }

}
