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
import com.day.cq.wcm.api.NameConstants;
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
        description = "Transform images programatically by applying a named transform to the requested Image.",
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
    @Property(
        name = "sling.servlet.resourceTypes",
        value = { "nt/file", "nt/resource", "dam/Asset", "cq/Page", "cq/PageContent", "nt/unstructured",
                "foundation/components/image", "foundation/components/parbase", "foundation/components/page" },
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

    private static final int SYSTEM_MAX_DIMENSION = 50000;
    private static final String DEFAULT_ROTATION = "0";

    private static final String MIME_TYPE_GIF = "image/gif";
    private static final String MIME_TYPE_PNG = "image/png";

    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_ROTATE = "rotate";
    private static final String KEY_CROP = "crop";

    @Reference
    private ComponentHelper componentHelper;

    /* Named Transforms */

    @Property(label = "Named Transforms",
            description = "my-name:/width/X/height/Y/rotate/Z/crop/A/B/C/D",
            cardinality = Integer.MAX_VALUE,
            value = { })
    private static final String PROP_NAMED_TRANSFORMS = "prop.named-transforms";
    private Map<String, String> namedTransformsMap = new HashMap<String, String>();

    /* Asset Rendition Pattern Picker */

    private static final String DEFAULT_ASSET_RENDITION_PICKER_REGEX = "cq5dam\\.web\\.(.*)";
    @Property(label = "Asset Rendition Picker Regex",
            description = "Regex to select the Rendition to transform when directly transforming a DAM Asset.",
            value = DEFAULT_ASSET_RENDITION_PICKER_REGEX)
    private static final String PROP_ASSET_RENDITION_PICKER_REGEX = "prop.asset-rendition-picker-regex";
    private static RenditionPatternPicker renditionPatternPicker =
            new RenditionPatternPicker(Pattern.compile(DEFAULT_ASSET_RENDITION_PICKER_REGEX));

    /**
     * Only accept requests that.
     * - Are not null
     * - Have a suffix
     * - Whose first suffix segment is a registered transform name
     *
     * @param request SlingRequest object
     * @return true if the Servlet should handle the request
     */
    @Override
    public final boolean accepts(SlingHttpServletRequest request) {
        if (request == null) {
            return false;
        }

        final String suffix = request.getRequestPathInfo().getSuffix();
        if (StringUtils.isBlank(suffix)) {
            return false;
        }

        final String transformName = PathInfoUtil.getSuffixSegment(request, 0);
        if (!this.namedTransformsMap.keySet().contains(transformName)) {
            return false;
        }

        return true;
    }


    /**
     * Writes the transformed image to the response.
     *
     * @param request SlingRequest object
     * @param response SlingResponse object
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected final void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws
            ServletException, IOException {

        final String transformName = PathInfoUtil.getSuffixSegment(request, 0);
        final String transform = this.namedTransformsMap.get(transformName);

        log.debug("Named image transform of: {}", request.getResource().getPath());
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
        final String mimeType = this.getMimeType(image);

        // Transform the image
        this.applyTransforms(image, layer);

        response.setContentType(mimeType);
        layer.write(mimeType, mimeType.equals(MIME_TYPE_GIF) ? 255 : 1.0, response.getOutputStream());

        response.flushBuffer();
    }

    private Image resolveImage(final SlingHttpServletRequest request) {
        final Resource resource = request.getResource();

        final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(resource);

        if (DamUtil.isAsset(resource)) {
            // For assets, pick the configured rendition if it exists
            // If rendition does not exist, use original

            final Asset asset = DamUtil.resolveToAsset(resource);
            Rendition rendition = asset.getRendition(renditionPatternPicker);

            if (rendition == null) {
                log.warn("Could not find rendition [ {} ] for [ {} ]", renditionPatternPicker.toString(),
                        resource.getPath());
                rendition = asset.getOriginal();
            }

            final Resource renditionResource = request.getResourceResolver().getResource(rendition.getPath());

            final Image image = new Image(resource);
            image.set(Image.PN_REFERENCE, renditionResource.getPath());
            return image;

        } else if (DamUtil.isRendition(resource)) {
            // For renditions; use the requested rendition
            final Image image = new Image(resource);
            image.set(Image.PN_REFERENCE, resource.getPath());
            return image;

        } else if (page != null) {
            if (ResourceUtil.isA(resource, NameConstants.NT_PAGE)
                    || StringUtils.equals(resource.getPath(), page.getContentResource().getPath())) {
                // Is a Page or Page's Content Resource; use the Page's image resource
                return new Image(page.getContentResource(), "image");
            } else {
                return new Image(resource);
            }
        }

        return new Image(resource);
    }

    /**
     * Apply the transforms initialized on the image.
     *
     * @param image the image w the transformations
     * @param layer the image's layer
     * @return true if any transformations were processed
     */
    private boolean applyTransforms(final Image image, final Layer layer) {
        boolean modified = image.crop(layer) != null;
        modified |= image.rotate(layer) != null;
        modified |= image.resize(layer) != null;

        return modified;
    }

    /**
     * Gets the mimeType of the image.
     *
     * @param image the image to get the mimeType for
     * @return the string representation of the image's mimeType
     */
    private String getMimeType(final Image image) {
        try {
            return image.getMimeType();
        } catch (final RepositoryException e) {
            return MIME_TYPE_PNG;
        }
    }

    /**
     * Set the transformation properties on the Image.
     *
     * @param image The Image to manipulate
     * @param width The width to transform the image to; or null to ignore
     * @param height The height to transform the image to; or null to ignore
     * @param crop The crop coordinates to apply to the image; or null to ignore
     * @param rotate The rotation to apply to the image; or null to ignore
     * @return
     */
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

    /**
     * Gets the Image layer allowing or manipulations.
     *
     * @param image The Image to get the layer from
     * @return the image's Layer
     * @throws IOException
     */
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

    /**
     * Wrapper method for parseTransform(String suffix, String key, int size, String delimiter).
     * <p/>
     * Passes
     * size: 0
     * delimiter: null
     *
     * @param suffix
     * @param key
     * @return
     */
    private String parseTransform(final String suffix, final String key) {
        return parseTransform(suffix, key, 1, null);
    }

    /**
     * Get key/values from the suffix string.
     *
     * @param suffix    Suffix string (foo/bar/pets/cat/dog/nuts.bolts)
     * @param key       Suffix segment to treat as the key
     * @param size      Number of suffix value segments after the key segment to return
     * @param delimiter Delimiter used to join the value segments
     * @return "true" if key exists and size = 0, segment value is key exists and size = 1,
     * joined segment values using delimiter if key exists and size > 1
     */
    private String parseTransform(final String suffix, final String key, final int size, String delimiter) {
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
        try {
            if (StringUtils.isBlank(rotate)) {
                return DEFAULT_ROTATION;
            }

            final long r = Long.parseLong(rotate) % 360;

            return String.valueOf(r);

          } catch (Exception ex) {
            // Error occurred parsing rotate value, use the DEFAULT_ROTATION
            // which forces the image to render using 0 rotation
            return DEFAULT_ROTATION;
        }
    }


    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        final String regex = PropertiesUtil.toString(properties.get(PROP_ASSET_RENDITION_PICKER_REGEX),
                DEFAULT_ASSET_RENDITION_PICKER_REGEX);
        try {
            renditionPatternPicker = new RenditionPatternPicker(regex);
            log.info("Asset Rendition Pattern Picker: {}", regex);
        } catch (Exception ex) {
            log.error("Error creating RenditionPatternPicker with regex [ {} ], defaultin to [ {} ]", regex,
                    DEFAULT_ASSET_RENDITION_PICKER_REGEX);
            renditionPatternPicker = new RenditionPatternPicker(DEFAULT_ASSET_RENDITION_PICKER_REGEX);
        }

        this.namedTransformsMap = OsgiPropertyUtil.toMap(PropertiesUtil.toStringArray(
                properties.get(PROP_NAMED_TRANSFORMS), new String[]{}), ":");

        log.info("Named Images Transforms");
        for (final Map.Entry<String, String> entry : this.namedTransformsMap.entrySet()) {
            log.info("{} ~> {}", entry.getKey(), entry.getValue());
        }
    }
}