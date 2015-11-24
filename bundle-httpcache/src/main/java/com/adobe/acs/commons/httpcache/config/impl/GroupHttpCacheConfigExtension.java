package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheReposityAccessException;
import com.adobe.acs.commons.httpcache.util.UserUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;

/**
 * This cache config extension assumes that custom attributes are aem group names. It accepts the http request only if
 * at least one of the configured groups is present in the request user's group membership list.
 */
@Component(immediate = true)
@Service
public class GroupHttpCacheConfigExtension implements HttpCacheConfigExtension {
    private static final Logger log = LoggerFactory.getLogger(GroupHttpCacheConfigExtension.class);

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, List<String>
            customConfigAttributes) throws HttpCacheReposityAccessException {

        // Match groups.
        if (UserUtils.isAnonymous(request.getResourceResolver().getUserID())) {
            // If the user is anonymous, no matching with groups required.
            return true;
        } else {
            // Case of authenticated requests.
            if (customConfigAttributes.isEmpty()) {
                // In case custom attributes list is empty.
                return true;
            }

            try {
                List<String> requestUserGroupNames = UserUtils.getUserGroupMembershipNames(request
                        .getResourceResolver().adaptTo(User.class));

                // At least one of the group in config should match.
                boolean isGroupMatchFound = CollectionUtils.containsAny(customConfigAttributes, requestUserGroupNames);
                if (!isGroupMatchFound) {
                    log.debug("Group didn't match and hence rejecting the cache config.");
                }
                return isGroupMatchFound;
            } catch (RepositoryException e) {
                throw new HttpCacheReposityAccessException("Unable to access group information of request user.", e);
            }
        }
    }
}
