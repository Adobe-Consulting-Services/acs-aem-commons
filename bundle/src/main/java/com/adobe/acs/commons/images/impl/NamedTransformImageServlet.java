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
import com.adobe.acs.commons.images.ImageTransformer;
import com.adobe.acs.commons.images.NamedImageTransformer;
import com.adobe.acs.commons.util.PathInfoUtil;
import com.day.cq.commons.jcr.JcrConstants;
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
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
@Component(
        label = "ACS AEM Commons - Named Transform Image Servlet",
        description = "Transform images programatically by applying a named transform to the requested Image.",
        metatype = true
)
@Properties({
        @Property(
                label = "Resource Types",
                description = "Resource Types and Node Types to bind this servlet to.",
                name = "sling.servlet.resourceTypes",
                value = { "nt/file", "nt/resource", "dam/Asset", "cq/Page", "cq/PageContent", "nt/unstructured",
                        "foundation/components/image", "foundation/components/parbase", "foundation/components/page" },
                propertyPrivate = false
        ),
        @Property(
                label = "Extension",
                description = "",
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
@References({
        @Reference(
                name = "namedImageTransformers",
                referenceInterface = NamedImageTransformer.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
        ),
        @Reference(
                name = "imageTransformers",
                referenceInterface = ImageTransformer.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
        )
})
@Service(Servlet.class)
public class NamedTransformImageServlet extends SlingSafeMethodsServlet implements OptingServlet {
    private static final Logger log = LoggerFactory.getLogger(NamedTransformImageServlet.class);

    @Reference
    private MimeTypeService mimeTypeService;

    private static final ValueMap EMPTY_PARAMS = new ValueMapDecorator(new LinkedHashMap<String, Object>());

    private static final Pattern LAST_SUFFIX_PATTERN = Pattern.compile("(image|img)\\.(.+)");

    private static final double IMAGE_GIF_MAX_QUALITY = 255;

    private static final double IMAGE_MAX_QUALITY = 1.0;

    private static final String MIME_TYPE_GIF = "image/gif";

    private static final String MIME_TYPE_PNG = "image/png";

    private Map<String, NamedImageTransformer> namedImageTransformers = new ConcurrentHashMap<String, NamedImageTransformer>();

    private Map<String, ImageTransformer> imageTransformers = new ConcurrentHashMap<String, ImageTransformer>();

    /* Asset Rendition Pattern Picker */

    private static final String DEFAULT_ASSET_RENDITION_PICKER_REGEX = "cq5dam\\.web\\.(.*)";

    @Property(label = "Asset Rendition Picker Regex",
            description = "Regex to select the Rendition to transform when directly transforming a DAM Asset."
                    + " [ Default: cq5dam.web.(.*) ]",
            value = DEFAULT_ASSET_RENDITION_PICKER_REGEX)
    private static final String PROP_ASSET_RENDITION_PICKER_REGEX = "prop.asset-rendition-picker-regex";

    private static RenditionPatternPicker renditionPatternPicker =
            new RenditionPatternPicker(Pattern.compile(DEFAULT_ASSET_RENDITION_PICKER_REGEX));

    /**
     * Only accept requests that.
     * - Are not null
     * - Have a suffix
     * - Whose first suffix segment is a registered transform name
     * - Whose last suffix matches the image file name pattern
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

        final String transformName = PathInfoUtil.getFirstSuffixSegment(request);
        if (!this.namedImageTransformers.keySet().contains(transformName)) {
            return false;
        }

        final String lastSuffix = PathInfoUtil.getLastSuffixSegment(request);
        final Matcher matcher = LAST_SUFFIX_PATTERN.matcher(lastSuffix);
        if (!matcher.matches()) {
            return false;
        }

        return true;
    }

    @Override
    protected final void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws
            ServletException, IOException {

        // Get the transform names from the suffix
        final List<NamedImageTransformer> selectedNamedImageTransformers = getNamedImageTransformers(request);

        // Collect and combine the image transformers and their params
        final ValueMap imageTransformersWithParams = getImageTransformersWithParams(selectedNamedImageTransformers);

        final Image image = this.resolveImage(request);
        final String mimeType = this.getMimeType(request, image);
        Layer layer = this.getLayer(image);
        
        // Transform the image
        layer = this.transform(layer, imageTransformersWithParams);

        final double quality = (mimeType.equals(MIME_TYPE_GIF) ? IMAGE_GIF_MAX_QUALITY : IMAGE_MAX_QUALITY);
        response.setContentType(mimeType);

        layer.write(mimeType, quality, response.getOutputStream());

        response.flushBuffer();
    }

    /**
     * Execute the ImageTransformers as specified by the Request's suffix segments against the Image layer.
     *
     * @param layer      the Image layer
     * @param transforms the transforms and their params
     * @return the transformed Image layer
     */
    protected final Layer transform(Layer layer, final ValueMap transforms) {

        for (final String type : transforms.keySet()) {
            final ImageTransformer imageTransformer = this.imageTransformers.get(type);
            if (imageTransformer == null) {
                log.warn("Skipping transform. Missing ImageTransformer for type: {}");
                continue;
            }

            final ValueMap transformParams = transforms.get(type, EMPTY_PARAMS);

            if (transformParams != null) {
                layer = imageTransformer.transform(layer, transformParams);
            }
        }

        return layer;
    }

    /**
     * Gets the NamedImageTransformers based on the Suffix segments in order.
     *
     * @param request the SlingHttpServletRequest object
     * @return a list of the NamedImageTransformers specified by the HTTP Request suffix segments
     */
    protected List<NamedImageTransformer> getNamedImageTransformers(final SlingHttpServletRequest request) {
        final List<NamedImageTransformer> transformers = new ArrayList<NamedImageTransformer>();

        String[] suffixes = PathInfoUtil.getSuffixSegments(request);
        if (suffixes.length < 2) {
            log.warn("Named Transform Image Servlet requires at least one named transform");
            return transformers;
        }

        log.error("Suffixed: {}", suffixes);
        suffixes = (String[]) ArrayUtils.subarray(suffixes, 0, suffixes.length - 1);

        for (final String transformerName : suffixes) {
            final NamedImageTransformer transformer = this.namedImageTransformers.get(transformerName);
            if (transformer != null) {
                log.error("Adding from suffix: {}", transformerName);
                transformers.add(transformer);
            }
        }

        return transformers;
    }

    /**
     * Collect and combine the image transformers and their params.
     *
     * @param namedImageTransformers the named transformers and their params
     * @return the combined named image transformers and their params
     */
    protected ValueMap getImageTransformersWithParams(final List<NamedImageTransformer> namedImageTransformers) {
        final ValueMap params = new ValueMapDecorator(new LinkedHashMap<String, Object>());

        for (final NamedImageTransformer namedImageTransformer : namedImageTransformers) {
            params.putAll(namedImageTransformer.getTransforms());
        }

        log.error("w params {}", params);

        return params;
    }

    /**
     * Intelligently determines how to find the Image based on the associated SlingRequest.
     *
     * @param request the SlingRequest Obj
     * @return the Image object configured w the info of where the image to render is stored in CRX
     */
    protected final Image resolveImage(final SlingHttpServletRequest request) {
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

        } else if (DamUtil.isRendition(resource)
                || ResourceUtil.isA(resource, JcrConstants.NT_FILE)
                || ResourceUtil.isA(resource, JcrConstants.NT_RESOURCE)) {
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
     * Gets the mimeType of the image.
     * - The last segments suffix is looked at first and used
     * - if the last suffix segment's "extension" is .orig or .original then use the underlying resources mimeType
     * - else look up the mimetype to use based on this "extension"
     * - default to the resource's mimetype if the requested mimetype by extension is not supported.
     *
     * @param image the image to get the mimeType for
     * @return the string representation of the image's mimeType
     */
    private String getMimeType(final SlingHttpServletRequest request, final Image image) {
        final String lastSuffix = PathInfoUtil.getLastSuffixSegment(request);

        final String mimeType = mimeTypeService.getMimeType(lastSuffix);

        if (!StringUtils.endsWithIgnoreCase(lastSuffix, ".orig")
            && !StringUtils.endsWithIgnoreCase(lastSuffix, ".original")
            && (ImageIO.getImageWritersByMIMEType(mimeType).hasNext())) {
            return mimeType;
        } else {
            try {
                return image.getMimeType();
            } catch (final RepositoryException e) {
                return MIME_TYPE_PNG;
            }
        }
    }

    /**
     * Gets the Image layer.
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
        } else {
            image.crop(layer);
            image.rotate(layer);
        }

        return layer;
    }

    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        final String regex = PropertiesUtil.toString(properties.get(PROP_ASSET_RENDITION_PICKER_REGEX),
                DEFAULT_ASSET_RENDITION_PICKER_REGEX);
        try {
            renditionPatternPicker = new RenditionPatternPicker(regex);
            log.info("Asset Rendition Pattern Picker: {}", regex);
        } catch (Exception ex) {
            log.error("Error creating RenditionPatternPicker with regex [ {} ], defaulting to [ {} ]", regex,
                    DEFAULT_ASSET_RENDITION_PICKER_REGEX);
            renditionPatternPicker = new RenditionPatternPicker(DEFAULT_ASSET_RENDITION_PICKER_REGEX);
        }
    }

    protected final void bindNamedImageTransformers(final NamedImageTransformer service,
                                                    final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(NamedImageTransformer.PROP_NAME), null);
        if (type != null) {
            this.namedImageTransformers.put(type, service);
        }
    }

    protected final void unbindNamedImageTransformers(final NamedImageTransformer service,
                                                      final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(NamedImageTransformer.PROP_NAME), null);
        if (type != null) {
            this.namedImageTransformers.remove(type);
        }
    }

    protected final void bindImageTransformers(final ImageTransformer service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(ImageTransformer.PROP_TYPE), null);
        if (type != null) {
            imageTransformers.put(type, service);
        }
    }

    protected final void unbindImageTransformers(final ImageTransformer service, final Map<Object, Object> props) {
        final String type = PropertiesUtil.toString(props.get(ImageTransformer.PROP_TYPE), null);
        if (type != null) {
            imageTransformers.remove(type);
        }
    }
}