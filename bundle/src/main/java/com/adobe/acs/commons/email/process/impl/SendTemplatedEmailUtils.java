/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.email.process.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;

public class SendTemplatedEmailUtils {

    private static final Logger log = LoggerFactory.getLogger(SendTemplatedEmailUtils.class);

    private static final String PN_USER_EMAIL = "profile/email";

    private SendTemplatedEmailUtils() {}

    /***
     * Tests whether the payload is a DAM asset or a cq:Page for DAM asset
     * returns all properties at the metadata node for DAM assets for cq:Page
     * returns all properties at the jcr:content node The Map<String, String>
     * that is returned contains string representations of each of the
     * respective properties
     *
     * @param payloadRes
     *            the payload as a resource
     * @param sdf
     *            used by the method to transform Date properties into Strings
     * @return Map<String, String> String representation of jcr properties
     */
    protected static final Map<String, String> getPayloadProperties(Resource payloadRes, SimpleDateFormat sdf) {

        Map<String, String> emailParams = new HashMap<String, String>();

        if (payloadRes == null) {
            return emailParams;
        }

        // Check if the payload is an asset
        if (DamUtil.isAsset(payloadRes)) {

            // get metadata resource
            Resource mdRes = payloadRes.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER);

            Map<String, String> assetMetadata = getJcrKeyValuePairs(mdRes, sdf);
            emailParams.putAll(assetMetadata);

        } else {
            // check if the payload is a page
            Page payloadPage = payloadRes.adaptTo(Page.class);

            if (payloadPage != null) {
                Map<String, String> pageContent = getJcrKeyValuePairs(payloadPage.getContentResource(), sdf);
                emailParams.putAll(pageContent);
            }
        }

        return emailParams;
    }

    /**
     * Gets email(s) based on the path to a principal or principle name.
     * If it points to a user an array with a single email is returned,
     * else an array of emails for each individual in the group
     *
     * @param resourceResolver
     * @param principleOrPath name of a user or group or the path to such
     * @return String[] of email(s) associated with account
     */
    protected static final String[] getEmailAddrsFromPathOrName(ResourceResolver resourceResolver, String principleOrPath) {
        if (StringUtils.startsWith(principleOrPath, "/")) {
            return getEmailAddrsFromUserPath(resourceResolver, principleOrPath);
        }

        try {
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            Authorizable auth = userManager.getAuthorizable(principleOrPath);
            return getEmailAddrsFromUserPath(resourceResolver, auth.getPath());
        } catch (RepositoryException e) {
            log.warn("Could not load repository paths for users. {}", e);
        }
        return new String[]{};
    }


    /**
     * Gets email(s) based on the path to a principal If the path is a user it
     * returns an array with a single email if the path is a group returns an
     * array emails for each individual in the group
     *
     * @param resourceResolver
     * @param principlePath
     *            path to a CQ user or group
     * @return String[] of email(s) associated with account
     */
    @SuppressWarnings({"squid:S3776"})
    protected static final String[] getEmailAddrsFromUserPath(ResourceResolver resourceResolver, String principlePath) {
        List<String> emailList = new LinkedList<String>();

        try {
            Resource authRes = resourceResolver.getResource(principlePath);

            if (authRes != null) {
                Authorizable authorizable = authRes.adaptTo(Authorizable.class);
                if (authorizable != null) {
                    // check if it is a group
                    if (authorizable.isGroup()) {
                        Group authGroup = authRes.adaptTo(Group.class);

                        // iterate over members of the group and add emails
                        Iterator<Authorizable> memberIt = authGroup.getMembers();
                        while (memberIt.hasNext()) {
                            String currEmail = getAuthorizableEmail(memberIt.next());
                            if (currEmail != null) {
                                emailList.add(currEmail);
                            }
                        }
                    } else {
                        // otherwise is an individual user
                        String authEmail = getAuthorizableEmail(authorizable);
                        if (authEmail != null) {
                            emailList.add(authEmail);
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.warn("Could not get list of email(s) for users. {}", e);
        }
        String[] emailReturn = new String[emailList.size()];
        return emailList.toArray(emailReturn);
    }

    /***
     * Method to add all properties of a resource to Key/Value map of strings
     * only converts dates to string format based on simple date format
     * concatenates String[] into a string of comma separated items all other
     * values uses toString
     *
     * @param resource
     * @return a string map where the key is the jcr property and the value is
     *
     */
    private static Map<String, String> getJcrKeyValuePairs(Resource resource, SimpleDateFormat sdf) {

        Map<String, String> returnMap = new HashMap<String, String>();

        if (resource == null) {
            return returnMap;
        }

        ValueMap resMap = resource.getValueMap();

        for (Map.Entry<String, Object> entry : resMap.entrySet()) {

            Object value = entry.getValue();

            if (value instanceof Calendar) {
                // Date property
                String fmtDate = formatDate((Calendar) value, sdf);
                returnMap.put(entry.getKey(), fmtDate);
            } else if (value instanceof String[]) {
                // concatenate string array
                String strValue = StringUtils.join((String[]) value, ", ");
                returnMap.put(entry.getKey(), strValue);

            } else {
                // all other properties just use default to string
                returnMap.put(entry.getKey(), value.toString());
            }
        }

        return returnMap;
    }

    private static String getAuthorizableEmail(Authorizable authorizable) throws RepositoryException {
        if (authorizable.hasProperty(PN_USER_EMAIL)) {
            Value[] emailVal = authorizable.getProperty(PN_USER_EMAIL);
            return emailVal[0].getString();
        }

        return null;
    }

    /***
     * Format date as a string using global variable sdf
     *
     * @param calendar
     * @return
     */
    private static String formatDate(Calendar calendar, SimpleDateFormat sdf) {

        return sdf.format(calendar.getTime());
    }

}
