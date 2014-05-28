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
package com.adobe.acs.commons.contentfinder.querybuilder.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.result.Hit;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.RepositoryException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ContentFinderHitBuilder {
    private static final Long ONE_MILLION = 1000000L;

    private ContentFinderHitBuilder() {
    }

    private static final int ELLIPSE_LENGTH = 3;

    private static final int MAX_EXCERPT_LENGTH = 32;

    private static final String DAM_THUMBNAIL = "cq5dam.thumbnail.48.48.png";

    /**
     * Builds the result object that will representing a CF view record for the provided hit.
     * <p/>
     * This method will generate the result object data points based on if the hit is:
     * 1) a Page
     * 2) an Asset
     * 3) Other
     *
     * @param hit a hit
     * @return a result object
     * @throws RepositoryException if something goes wrong
     */
    public static Map<String, Object> buildGenericResult(final Hit hit) throws RepositoryException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        final Resource resource = hit.getResource();

        /**
         * Apply custom properties based on the "type"
         */

        // Assets
        final Asset asset = DamUtil.resolveToAsset(resource);
        if (asset != null) {
            return addAssetData(asset, hit, map);
        }

        // Pages
        final Page page = getPage(resource);
        if (page != null) {
            return addPageData(page, hit, map);
        }

        // Other
        return addOtherData(hit, map);
    }

    /**
     * Derives and adds Page related information to the map representing the hit.
     *
     * @param hit
     * @param map
     * @return
     * @throws javax.jcr.RepositoryException
     */
    private static Map<String, Object> addPageData(final Page page, final Hit hit, Map<String, Object> map)
            throws RepositoryException {

        // Title
        String title = page.getName();

        if (StringUtils.isNotBlank(page.getTitle())) {
            title = page.getTitle();
        } else if (StringUtils.isNotBlank(page.getPageTitle())) {
            title = page.getPageTitle();
        } else if (StringUtils.isNotBlank(page.getNavigationTitle())) {
            title = page.getNavigationTitle();
        }

        // Excerpt
        String excerpt = hit.getExcerpt();
        if (StringUtils.isBlank(hit.getExcerpt())) {
            excerpt = StringUtils.stripToEmpty(page.getDescription());
            if (excerpt.length() > MAX_EXCERPT_LENGTH) {
                excerpt = StringUtils.substring(excerpt, 0, (MAX_EXCERPT_LENGTH - ELLIPSE_LENGTH)) + "...";
            }
        }

        map.put("path", page.getPath());
        map.put("name", page.getName());
        map.put("title", title);
        map.put("excerpt", excerpt);
        map.put("ddGroups", "page");
        map.put("type", "Page");
        map.put("lastModified", getLastModified(page));

        return map;
    }

    /**
     * Derives and adds Asset related information to the map representing the hit.
     *
     * @param hit
     * @param map
     * @return
     * @throws javax.jcr.RepositoryException
     */
    private static Map<String, Object> addAssetData(final Asset asset, final Hit hit, Map<String, Object> map)
            throws RepositoryException {

        String title = asset.getName();

        if (StringUtils.isNotBlank(asset.getMetadataValue(DamConstants.DC_TITLE))) {
            title = asset.getMetadataValue(DamConstants.DC_TITLE);
        }

        // Excerpt
        String excerpt = hit.getExcerpt();
        if (StringUtils.isBlank(hit.getExcerpt())) {
            excerpt = StringUtils.stripToEmpty(asset.getMetadataValue(DamConstants.DC_DESCRIPTION));
            if (excerpt.length() > MAX_EXCERPT_LENGTH) {
                excerpt = StringUtils.substring(excerpt, 0, (MAX_EXCERPT_LENGTH - ELLIPSE_LENGTH)) + "...";
            }
        }

        map.put("path", asset.getPath());
        map.put("name", asset.getName());
        map.put("title", title);
        map.put("excerpt", excerpt);
        map.put("mimeType", asset.getMimeType());
        map.put("size", getSize(asset));
        map.put("ck", getCK(asset));
        map.put("type", "Asset");
        map.put("lastModified", getLastModified(asset));

        return map;
    }

    /**
     * Derives and adds Other (non-Page, non-Asset) related information to the map representing the hit.
     *
     * @param hit
     * @param map
     * @return
     * @throws javax.jcr.RepositoryException
     */
    private static Map<String, Object> addOtherData(final Hit hit, Map<String, Object> map)
            throws RepositoryException {
        final Resource resource = hit.getResource();
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        map.put("path", resource.getPath());
        map.put("name", resource.getName());
        map.put("title", properties.get("jcr:title", resource.getName()));
        map.put("excerpt", hit.getExcerpt());
        map.put("lastModified", getLastModified(resource));
        map.put("type", "Data");

        return map;
    }

    /**
     * Get the last modified date for an Asset.
     *
     * @param asset
     * @return
     */
    private static long getLastModified(final Asset asset) {
        if (asset.getLastModified() > 0L) {
            return asset.getLastModified();
        } else {
            final Object obj = asset.getMetadata().get(JcrConstants.JCR_LASTMODIFIED);

            if (obj != null && obj instanceof Date) {
                return ((Date) obj).getTime();
            } else {
                return 0L;
            }
        }
    }

    /**
     * Get the last modified date for a Page.
     *
     * @param page
     * @return
     */
    private static long getLastModified(final Page page) {
        if (page.getLastModified() != null) {
            return page.getLastModified().getTimeInMillis();
        } else {
            final ValueMap properties = page.getProperties();
            Date lastModified = properties.get(NameConstants.PN_PAGE_LAST_MOD, Date.class);
            if (lastModified != null) {
                return lastModified.getTime();
            } else {
                return 0L;
            }
        }
    }

    /**
     * Get the last modified date for a generic resource.
     *
     * @param resource
     * @return
     */
    private static long getLastModified(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        final Date cqLastModified = properties.get(NameConstants.PN_PAGE_LAST_MOD, Date.class);
        if (cqLastModified != null) {
            return cqLastModified.getTime();
        }

        final Date jcrLastModified = properties.get(JcrConstants.JCR_LASTMODIFIED, Date.class);
        if (jcrLastModified != null) {
            return jcrLastModified.getTime();
        }

        return 0L;
    }

    /**
     * Get the size of the Asset (the original rendition).
     *
     * @param asset
     * @return
     */
    private static long getSize(final Asset asset) {
        final Rendition original = asset.getOriginal();
        if (original == null) {
            return 0;
        }
        return original.getSize();
    }

    /**
     * Get the timestamp for the last change to the thumbnail.
     *
     * @param asset
     * @return
     */
    private static long getCK(final Asset asset) {
        try {
            Resource resource = asset.getRendition(DAM_THUMBNAIL);
            Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
            ValueMap properties = contentResource.adaptTo(ValueMap.class);

            return properties.get(JcrConstants.JCR_LASTMODIFIED, 0L) / ONE_MILLION;
        } catch (Exception ex) {
            return 0L;
        }
    }

    /**
     * Gets the Page object corresponding the with the resource.
     * Will resolve to a Page if the result is a cq:Page or a cq:Page's jcr:content node.
     *
     * @param resource The resource to covert to a Page
     * @return a Page if  the resource is Page like (cq:Page or a cq:Page's jcr:content node), else null
     */
    private static Page getPage(final Resource resource) {
        if (resource == null) {
            return null;
        }

        // If resource is a cq:Page node; then return the Page
        if (resource.adaptTo(Page.class) != null) {
            return resource.adaptTo(Page.class);
        }

        // If the resource is a cq:Page/jcr:content node, then return the cq:Page page
        if (StringUtils.equals(resource.getName(), JcrConstants.JCR_CONTENT)) {
            final Resource parent = resource.getParent();
            if (parent != null) {
                if (parent.adaptTo(Page.class) != null) {
                    return parent.adaptTo(Page.class);
                }
            }
        }

        return null;
    }
}
