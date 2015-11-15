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
    private UserUtils() {
        throw new Error(UserUtils.class.getName() + " is not meant to be instantiated.");
    }

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
        List<String> groupNames = new ArrayList<>();

        Iterator<Group> groupIterator = user.memberOf();
        while (groupIterator.hasNext()) {
            groupNames.add(groupIterator.next().getID());
        }

        return groupNames;
    }
}
