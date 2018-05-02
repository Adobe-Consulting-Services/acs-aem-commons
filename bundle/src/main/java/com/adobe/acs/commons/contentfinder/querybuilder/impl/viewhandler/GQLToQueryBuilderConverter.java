/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.Predicate;
import com.day.cq.search.eval.FulltextPredicateEvaluator;
import com.day.cq.search.eval.JcrPropertyPredicateEvaluator;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler.ContentFinderConstants.*;

import java.util.Map;

public final class GQLToQueryBuilderConverter {

    private static final String SUFFIX_ORDERBY = "_orderby";
    private static final String SUFFIX_ORDERBY_SORT = "_orderby.sort";
    private static final String AT = "@";
    private static final String SUFFIX_GROUP = "_group";
    private static final String SUFFIX_P_OR = ".p.or";

    private GQLToQueryBuilderConverter() {
    }

    private static final Logger log = LoggerFactory.getLogger(GQLToQueryBuilderConverter.class);

    /**
     * Checks if request forces QueryBuilder mode
     *
     * @param request
     * @return
     */
    public static boolean convertToQueryBuilder(final SlingHttpServletRequest request) {
        return (has(request, ContentFinderConstants.CONVERT_TO_QUERYBUILDER_KEY) && ContentFinderConstants.CONVERT_TO_QUERYBUILDER_VALUE
                .equals(get(request, ContentFinderConstants.CONVERT_TO_QUERYBUILDER_KEY)));
    }

    public static Map<String, String> addPath(final SlingHttpServletRequest request, Map<String, String> map) {
        if (has(request, CF_PATH)) {
            map = put(request, map, CF_PATH, GROUP_PATH, true);
        } else {
            map.put(CF_PATH, request.getRequestPathInfo().getSuffix());
        }

        return map;
    }

    public static Map<String, String> addType(final SlingHttpServletRequest request, Map<String, String> map) {
        if (has(request, CF_TYPE)) {
            map = put(request, map, CF_TYPE, GROUP_TYPE, true);
        }

        return map;
    }

    public static Map<String, String> addName(final SlingHttpServletRequest request, Map<String, String> map) {
        if (has(request, CF_NAME)) {
            map = put(request, map, CF_NAME, "nodename", GROUP_NAME, true);
        }

        return map;
    }

    @SuppressWarnings("squid:S3776")
    public static Map<String, String> addOrder(final SlingHttpServletRequest request, Map<String, String> map,
            final String queryString) {
        if (has(request, CF_ORDER)) {

            int count = 1;
            for (String value : getAll(request, CF_ORDER)) {
                value = StringUtils.trim(value);
                final String orderGroupId = String.valueOf(GROUP_ORDERBY_USERDEFINED + count) + SUFFIX_ORDERBY;
                boolean sortAsc = false;

                if (StringUtils.startsWith(value, "-")) {
                    value = StringUtils.removeStart(value, "-");
                } else if (StringUtils.startsWith(value, "+")) {
                    sortAsc = true;
                    value = StringUtils.removeStart(value, "+");
                }

                map.put(orderGroupId, StringUtils.trim(value));
                map.put(orderGroupId + ".sort", sortAsc ? Predicate.SORT_ASCENDING : Predicate.SORT_DESCENDING);

                count++;
            }

        } else {

            final boolean isPage = isPage(request);
            final boolean isAsset = isAsset(request);
            final String prefix = getPropertyPrefix(request);

            if (StringUtils.isNotBlank(queryString)) {
                map.put(GROUP_ORDERBY_SCORE + SUFFIX_ORDERBY, AT + JcrConstants.JCR_SCORE);
                map.put(GROUP_ORDERBY_SCORE + SUFFIX_ORDERBY_SORT, Predicate.SORT_DESCENDING);
            }

            String modifiedOrderProperty = AT + JcrConstants.JCR_LASTMODIFIED;
            if (isPage) {
                modifiedOrderProperty = AT + prefix + NameConstants.PN_PAGE_LAST_MOD;
            } else if (isAsset) {
                modifiedOrderProperty = AT + prefix + JcrConstants.JCR_LASTMODIFIED;
            }

            map.put(GROUP_ORDERBY_MODIFIED + SUFFIX_ORDERBY, modifiedOrderProperty);
            map.put(GROUP_ORDERBY_MODIFIED + SUFFIX_ORDERBY_SORT, Predicate.SORT_DESCENDING);
        }

        return map;
    }

    public static Map<String, String> addMimeType(final SlingHttpServletRequest request, Map<String, String> map) {
        final boolean isAsset = isAsset(request);
        final String prefix = getPropertyPrefix(request);

        if (isAsset && has(request, CF_MIMETYPE)) {
            map.put(GROUP_MIMETYPE + "_group.1_property.operation", "like");
            map.put(GROUP_MIMETYPE + "_group.1_property", prefix + DamConstants.DC_FORMAT);
            map.put(GROUP_MIMETYPE + "_group.1_property.value", "%" + get(request, CF_MIMETYPE) + "%");
        }

        return map;
    }

    public static Map<String, String> addTags(final SlingHttpServletRequest request, Map<String, String> map) {
        if (has(request, CF_TAGS)) {
            final String prefix = getPropertyPrefix(request);

            final String groupId = GROUP_TAGS + SUFFIX_GROUP;
            final String tagProperty = prefix + NameConstants.PN_TAGS;

            map.put(groupId + SUFFIX_P_OR, "true");

            if (hasMany(request, CF_TAGS)) {
                final String[] tags = getAll(request, CF_TAGS);

                int counter = 1;
                for (final String tag : tags) {
                    map.put(groupId + "." + counter + "_tagid.property", tagProperty);
                    map.put(groupId + "." + counter + "_tagid", tag);

                    counter++;
                }
            } else {
                map.put(groupId + ".1_tagid.property", tagProperty);
                map.put(groupId + ".1_tagid", get(request, CF_TAGS));
            }
        }

        return map;
    }

    @SuppressWarnings("squid:S1172")
    public static Map<String, String> addFulltext(final SlingHttpServletRequest request, Map<String, String> map,
            final String queryString) {
        if (StringUtils.isNotBlank(queryString)) {
            final String groupId = GROUP_FULLTEXT + SUFFIX_GROUP;

            map.put(groupId + "." + FulltextPredicateEvaluator.FULLTEXT, queryString);
            map.put(groupId + SUFFIX_P_OR, "true");
        }
        return map;
    }

    public static Map<String, String> addLimitAndOffset(final SlingHttpServletRequest request,
            Map<String, String> map) {
        if (has(request, CF_LIMIT)) {
            // Both limits and offsets are computed from CF's limit field X..Y
            final String offset = String.valueOf(getOffset(request));
            final String limit = String.valueOf(getLimit(request));

            map.put("p.offset", String.valueOf(offset));
            map.put("p.limit", limit);
        } else {
            map.put("p.limit", String.valueOf(DEFAULT_LIMIT));
        }

        return map;
    }

    public static Map<String, String> addProperty(final SlingHttpServletRequest request, Map<String, String> map,
            final String requestKey, final int count) {
        if (!ArrayUtils.contains(ContentFinderConstants.PROPERTY_BLACKLIST, requestKey)) {
            map = putProperty(request, map, requestKey, JcrPropertyPredicateEvaluator.PROPERTY,
                    (GROUP_PROPERTY_USERDEFINED + count), true);
        } else {
            log.debug("Rejecting property [ {} ] due to blacklist match", requestKey);
        }
        return map;
    }

    public static boolean isValidProperty(final String key) {
        return (!ArrayUtils.contains(ContentFinderConstants.PROPERTY_BLACKLIST, key));
    }

    /**
     * Checks if the provided key has more than 1 values (comma delimited)
     *
     * @param request
     * @param key
     * @return
     */
    public static boolean hasMany(SlingHttpServletRequest request, String key) {
        final RequestParameter rp = request.getRequestParameter(key);
        if (rp == null) {
            return false;
        }
        return getAll(request, key).length > 1;
    }

    /**
     * Checks if the provided key has ANY values (1 or more)
     *
     * @param request
     * @param key
     * @return
     */
    public static boolean has(SlingHttpServletRequest request, String key) {
        return request.getParameterValues(key) != null;
    }

    /**
     * Returns a single value for a query parameter key
     *
     * @param request
     * @param key
     * @return
     */
    public static String get(SlingHttpServletRequest request, String key) {
        return StringUtils.trim(request.getRequestParameter(key).toString());
    }

    /**
     * Returns a String array from a comma delimited list of values
     *
     * @param request
     * @param key
     * @return
     */
    public static String[] getAll(SlingHttpServletRequest request, String key) {
        final RequestParameter rp = request.getRequestParameter(key);
        if (rp == null) {
            return new String[0];
        }
        return StringUtils.split(rp.getString(), DELIMITER);
    }

    /**
     * Convenience wrapper
     *
     * @param request
     * @param map
     * @param predicate
     * @param group
     * @param or
     * @return
     */
    public static Map<String, String> put(SlingHttpServletRequest request, Map<String, String> map,
            String predicate, int group, boolean or) {
        return putAll(map, predicate, getAll(request, predicate), group, or);
    }

    public static Map<String, String> put(SlingHttpServletRequest request, Map<String, String> map,
            String requestKey, String predicate, int group, boolean or) {
        return putAll(map, predicate, getAll(request, requestKey), group, or);
    }

    /**
     * Used when the request key is different from the Predicate
     *
     * @param request
     * @param map
     * @param requestKey
     * @param predicate
     * @param group
     * @param or
     * @return
     */
    public static Map<String, String> putProperty(SlingHttpServletRequest request, Map<String, String> map,
            String requestKey, String predicate, int group, boolean or) {
        // putAll(map, "property", "jcr:titke", "value", [x,y,z], 10, true)
        return putAll(map, predicate, requestKey, JcrPropertyPredicateEvaluator.VALUE,
                getAll(request, requestKey), group, or);
    }

    /**
     * Helper method for adding comma delimited values into a Query Builder predicate
     *
     * @param map
     * @param predicate
     * @param values
     * @param group
     * @param or
     * @return
     */
    public static Map<String, String> putAll(Map<String, String> map, String predicate, String[] values,
            int group, boolean or) {
        final String groupId = String.valueOf(group) + SUFFIX_GROUP;
        int count = 1;

        for (final String value : values) {
            final String predicateId = count + "_" + predicate;

            map.put(groupId + "." + predicateId, StringUtils.trim(value));
            count++;
        }

        map.put(groupId + SUFFIX_P_OR, String.valueOf(or));

        return map;
    }

    /**
     * @param map
     * @param predicateValue  => jcr:title
     * @param predicate       => property
     * @param predicateSuffix => value
     * @param values          => [Square, Triangle]
     * @param group           => ID
     * @param or              => true/false
     * @return
     */
    public static Map<String, String> putAll(Map<String, String> map, String predicate, String predicateValue,
            String predicateSuffix, String[] values, int group, boolean or) {
        final String groupId = String.valueOf(group) + SUFFIX_GROUP;

        map.put(groupId + "." + predicate, predicateValue);

        int count = 1;
        for (final String value : values) {
            final String predicateId = predicate;
            final String predicateSuffixId = count + "_" + predicateSuffix;
            map.put(groupId + "." + predicateId + "." + predicateSuffixId, StringUtils.trim(value));
            count++;
        }

        map.put(groupId + SUFFIX_P_OR, String.valueOf(or));

        return map;
    }

    /**
     * Checks of the query param node type is that of a CQ Page
     *
     * @param request
     * @return
     */
    public static boolean isPage(final SlingHttpServletRequest request) {
        if (has(request, CF_TYPE)) {
            String nodeType = get(request, CF_TYPE);
            return StringUtils.equals(nodeType, NameConstants.NT_PAGE);
        }
        return false;
    }

    /**
     * Checks of the query param node type is that of a DAM Asset
     *
     * @param request
     * @return
     */
    public static boolean isAsset(final SlingHttpServletRequest request) {
        if (has(request, CF_TYPE)) {
            String nodeType = get(request, CF_TYPE);
            return StringUtils.equals(nodeType, DamConstants.NT_DAM_ASSET);
        }
        return false;
    }

    public static String getPropertyPrefix(final SlingHttpServletRequest request) {
        final boolean isPage = isPage(request);
        final boolean isAsset = isAsset(request);

        String prefix = "";
        if (isPage) {
            prefix = JcrConstants.JCR_CONTENT + "/";
        } else if (isAsset) {
            prefix = JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER + "/";
        }

        return prefix;
    }

    /**
     * Extract the query limit from the ContentFinder Query Parameter notation
     *
     * @param request
     * @return
     */
    public static int getLimit(final SlingHttpServletRequest request) {
        if (has(request, CF_LIMIT)) {
            final String value = get(request, CF_LIMIT);
            final String[] limits = StringUtils.split(value, "..");

            if (value.matches("^(\\d)+\\.\\.(\\d)+$")) {
                // 10..20
                return Integer.parseInt(limits[1]) - Integer.parseInt(limits[0]);
            } else if (value.matches("^\\.\\.(\\d)+$")) {
                // ..20
                return Integer.parseInt(limits[0]);
            } else if (value.matches("^(\\d)+\\.\\.$")) {
                // 20..
                // Not upper limit
                return DEFAULT_LIMIT;
            }
            log.info("Could not find valid LIMIT for QueryBuilder-based ContentFinder: {}", value);
        } else {
            log.info("Could not find any LIMIT for QueryBuilder-based ContentFinder");
        }

        return DEFAULT_LIMIT;
    }

    /**
     * Extract the query offset from the ContentFinder Query Parameter notation
     *
     * @param request
     * @return
     */
    public static int getOffset(final SlingHttpServletRequest request) {
        if (has(request, CF_LIMIT)) {
            final String value = get(request, CF_LIMIT);
            final String[] offsets = StringUtils.split(value, "..");

            if (value.matches("^(\\d)+\\.\\.(\\d)+$") // 10..20
                    || value.matches("^\\.\\.(\\d)+$") // ..20
                    || value.matches("^(\\d)+\\.\\.$") ) { // 20..
                return Integer.parseInt(offsets[0]);
            }
            log.info("Could not find valid OFFSET for QueryBuilder-based ContentFinder: {}", value);
        } else {
            log.info("Could not find any OFFSET for QueryBuilder-based ContentFinder");
        }

        return DEFAULT_OFFSET;
    }
}
