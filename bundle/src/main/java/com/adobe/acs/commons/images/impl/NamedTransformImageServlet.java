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
package com.adobe.acs.commons.images.impl;

import com.adobe.acs.commons.dam.RenditionPatternPicker;
import com.adobe.acs.commons.util.OsgiPropertyUtil;
import com.adobe.acs.commons.util.PathInfoUtil;
import com.adobe.acs.commons.wcm.ComponentHelper;
import com.day.cq.commons.ImageResource;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.foundation.Image;
import com.day.image.Layer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component(
        label = "ACS AEM Commons - Named Transform Image Servlet",
        description = "Transform images programatically by applying a named traansform to the Request.",
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
    @Property(
        name = "sling.servlet.resourceTypes",
        value = { "nt/file", "dam/Asset", "nt/unstructured", "foundation/components/image" },
        propertyPrivate = true
    ),
    @Property(
            name = "sling.servlet.extensions",
            value = { "transform" },
            propertyPrivate = true
    ),
    @Property(
            name = "sling.servlet.methods",
            value = { "GET" },
            propertyPrivate = true
    )
})
@Service(Servlet.class)
public class NamedTransformImageServlet extends SlingSafeMethodsServlet implements OptingServlet {
    private final Logger log = LoggerFactory.getLogger(NamedTransformImageServlet.class);

    private static final String MIME_TYPE_GIF = "image/gif";

    private static final RenditionPatternPicker RENDITION_PICKER =
            new RenditionPatternPicker(Pattern.compile("cq5dam\\.web\\.(.*)"));

    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_ROTATE = "rotate";
    private static final String KEY_CROP = "crop";


    private static final int SYSTEM_MAX_DIMENSION = 50000;

    @Reference
    private ComponentHelper componentHelper;

    /**
     * OSGi Properties *
     */

    @Property(label = "Named Transforms",
            description = "<name>:<transform>",
            cardinality = Integer.MAX_VALUE,
            value = {})
    private static final String PROP_NAMED_TRANSFORMS = "prop.named-transforms";

    private Map<String, String> namedTransformsMap = new HashMap<String, String>();



    /**
     * OptingServlet Acceptance Method *
     */
    @Override
    public boolean accepts(SlingHttpServletRequest request) {
        if (request == null) {
            return false;
        }

        final String suffix = request.getRequestPathInfo().getSuffix();
        log.debug("suffix: {}", suffix);
        if (StringUtils.isBlank(suffix)) {
            return false;
        }

        final String transformName = PathInfoUtil.getSuffixSegment(request, 0);
        log.debug("transformName: {}", transformName);
        if (!this.namedTransformsMap.keySet().contains(transformName)) {
            return false;
        }

        log.debug("Candidate for Named Transform Servlet");
        return true;
    }


    /**
     * Add overrides for other SlingSafeMethodsServlet here (doGeneric, doHead, doOptions, doTrace) *
     */

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        final String transformName = PathInfoUtil.getSuffixSegment(request, 0);
        final String transform = this.namedTransformsMap.get(transformName);

        log.debug("{} ~> {}", transformName, transform);

        final String width = parseTransform(transform, KEY_WIDTH);
        final String height = parseTransform(transform, KEY_HEIGHT);
        final String rotate = scrubRotate(parseTransform(transform, KEY_ROTATE));
        final String crop = parseTransform(transform, KEY_CROP, 4, ",");

        log.debug("width: {}", width);
        log.debug("height: {}", height);
        log.debug("rotate: {}", rotate);
        log.debug("crop: {}", crop);

        final Image image = this.applyImageProperties(this.resolveImage(request), width, height, crop, rotate);

        final Layer layer = this.getLayer(image);
        final boolean modified = this.getModified(image, layer);
        final String mimeType = this.getMimeType(image);

        // don't cache images on authoring instances
        // Cache-Control: no-cache allows caching (e.g. in the browser cache) but
        // will force re-validation using If-Modified-Since or If-None-Match every time,
        // avoiding aggressive browser caching
        if (!componentHelper.isDisabledMode(request)) {
            response.setHeader("Cache-Control", "no-cache");
        }

        response.setContentType(mimeType);
        layer.write(mimeType, mimeType.equals(MIME_TYPE_GIF) ? 255 : 1.0, response.getOutputStream());

        response.flushBuffer();
    }


    private Image resolveImage(final SlingHttpServletRequest request) {
        final Resource resource = request.getResource();

        log.debug("Trying to find image for: {}", resource.getPath());
        final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(resource);

        // Handle Pages and Page sub-resources
        if(page != null) {
            if(ResourceUtil.isA(resource, "cq/Page")) {
                // Is a Page, use the Page's image resource
                return new Image(page.getContentResource(), "image");
            } else if(ResourceUtil.isA(resource, "foundation/components/image")) {
                // Is a resource type whose supertype is the foundation image component
                return new Image(resource);
            }
        } else if(DamUtil.isAsset(resource)) {
            log.debug("Is a DAM Asset");
            // For assets, pick the configured rendition if it exists
            // If rendition does not exist, use original
            final Asset asset = DamUtil.resolveToAsset(resource);
            Rendition rendition = asset.getRendition(RENDITION_PICKER);
            if(rendition == null) {
                log.debug("Selected rendition is null, use original");
                rendition = asset.getOriginal();
            }

            final Resource renditionResource = request.getResourceResolver().getResource(rendition.getPath());
            log.debug("Using rendition: {}", renditionResource.getPath());

            final Image image = new Image(resource);
            image.set(Image.PN_REFERENCE, renditionResource.getPath());
            return image;
        } else if(DamUtil.isRendition(resource)) {
            log.debug("Using specific rendition: {}", resource.getPath());

            final Image image = new Image(resource);
            image.set(Image.PN_REFERENCE, resource.getPath());
            return image;
        }

        log.debug("Could not find image");
        return new Image(resource);
    }

    private String getMimeType(final Image image) {
        try {
            return image.getMimeType();
        } catch (RepositoryException e) {
            return "image/png";
        }
    }

    private Image applyImageProperties(final Image image, final String width, final String height,
                                       final String crop, final String rotate)  {
        if (StringUtils.isNotBlank(width)) {
            image.set(ImageResource.PN_WIDTH, width);
        }

        if (StringUtils.isNotBlank(height)) {
            image.set(ImageResource.PN_HEIGHT, height);
        }

        if (StringUtils.isNotBlank(rotate)) {
            image.set(ImageResource.PN_IMAGE_ROTATE, rotate);
        }

        if (StringUtils.isNotBlank(crop)) {
            image.set(ImageResource.PN_IMAGE_CROP, crop);
        }

        // Cowardly refusing to resize images past an already enormous size
        image.set(Image.PN_MAX_WIDTH, String.valueOf(SYSTEM_MAX_DIMENSION));
        image.set(Image.PN_MAX_HEIGHT, String.valueOf(SYSTEM_MAX_DIMENSION));

        return image;
    }


    private Layer getLayer(final Image image) throws IOException {
        Layer layer = null;

        try {
            layer = image.getLayer(false, false, false);
        } catch (RepositoryException ex) {
            log.error("Could not create layer");
        }

        if (layer == null) {
            log.error("Could not create layer - layer is null;");
        }

        return layer;
    }

    private boolean getModified(final Image image, final Layer layer) {
        boolean modified = image.crop(layer) != null;
        modified |= image.rotate(layer) != null;
        modified |= image.resize(layer) != null;

        return modified;
    }



    /**
     * Wrapper method for parseTransform(String suffix, String key, int size, String delimiter)
     * <p/>
     * Passes
     * size: 0
     * delimiter: null
     *
     * @param suffix
     * @param key
     * @return
     */
    private String parseTransform(String suffix, String key) {
        return parseTransform(suffix, key, 1, null);
    }

    /**
     * Get key/values from the suffix string
     *
     * @param suffix    Suffix string (foo/bar/pets/cat/dog/nuts.bolts)
     * @param key       Suffix segment to treat as the key
     * @param size      Number of suffix value segments after the key segment to return
     * @param delimiter Delimiter used to join the value segments
     * @return "true" if key exists and size = 0, segment value is key exists and size = 1, joined segment values using delimiter if key exists and size > 1
     */
    private String parseTransform(String suffix, String key, int size, String delimiter) {
        final String[] suffixes = StringUtils.split(suffix, "/");

        final int index = ArrayUtils.indexOf(suffixes, key);
        if (index < 0 || (index + size) >= suffixes.length) {
            return null;
        }

        if (size < 1) {
            return String.valueOf(true);
        }

        final List<String> result = new ArrayList<String>();
        for (int i = index + 1; i <= index + size; i++) {
            result.add(suffixes[i]);
        }

        delimiter = StringUtils.isBlank(delimiter) ? "" : delimiter;
        return StringUtils.join(result, delimiter);
    }


    /**
     * Validates and normalizes rotation parameter.
     * <p/>
     * This normalized values to exist between -360 and 360.
     *
     * @param rotate
     * @return
     */
    private String scrubRotate(final String rotate) {
        final String DEFAULT_ROTATE = "0";

        try {
            if (StringUtils.isBlank(rotate)) {
                return DEFAULT_ROTATE;
            }

            final long r = Long.parseLong(rotate) % 360;
            return String.valueOf(r);
          } catch (Exception ex) {
            // Error occurred parsing rotate value, use the DEFAULT_ROTATE
            // which forces the image to render using 0 rotation
            return DEFAULT_ROTATE;
        }
    }



    @Activate
    protected void activate(Map<String, String> properties) throws Exception {

        this.namedTransformsMap = OsgiPropertyUtil.toMap(PropertiesUtil.toStringArray(properties.get
                (PROP_NAMED_TRANSFORMS), new String[]{ }), ":");

        for(final Map.Entry<String, String> entry : this.namedTransformsMap.entrySet()) {
            log.info("{} ~> {}", entry.getKey(), entry.getValue());
        }
    }
}