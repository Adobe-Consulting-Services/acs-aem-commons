/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.util;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utilties tied to user groups and authentication.
 */
public class UserUtils {
    private UserUtils() {}

    /** User id for anonymous requests */
    public static final String USER_ID_ANONYMOUS = "anonymous";

    /**
     * Check if the given user id is anonymous.
     *
     * @param userId
     * @return
     */
    public static boolean isAnonymous(String userId) {
        if (USER_ID_ANONYMOUS.equals(userId)) {
            return true;
        }
        return false;
    }

    /**
     * Get the list of names of groups for which this user has a membership.
     *
     * @param user
     * @return
     */
    public static List<String> getUserGroupMembershipNames(User user) throws RepositoryException {
        List<String> groupNames = new ArrayList<String>();

        Iterator<Group> groupIterator = user.memberOf();
        while (groupIterator.hasNext()) {
            groupNames.add(groupIterator.next().getID());
        }

        return groupNames;
    }
}
