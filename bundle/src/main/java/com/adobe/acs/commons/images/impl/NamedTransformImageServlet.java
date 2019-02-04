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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
@Component(
        service = Servlet.class,
        reference = {
                @Reference(
                        name = "namedImageTransformers",
                        service = NamedImageTransformer.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.MULTIPLE
                ),
                @Reference(
                        name = "imageTransformers",
                        service = ImageTransformer.class,
                        policy = ReferencePolicy.DYNAMIC,
                        cardinality = ReferenceCardinality.MULTIPLE
                )
        },
        property = {
                "sling.servlet.extensions=transform",
                "sling.servlet.methods=GET"
        }
)
@Designate(
        ocd=NamedTransformImageServlet.Config.class
)
public class NamedTransformImageServlet extends SlingSafeMethodsServlet implements OptingServlet {

    private static final Logger log = LoggerFactory.getLogger(NamedTransformImageServlet.class);

    public static final String NAME_IMAGE = "image";

    public static final String NAMED_IMAGE_FILENAME_PATTERN = "acs.commons.namedimage.filename.pattern";

    public static final String DEFAULT_FILENAME_PATTERN = "(image|img)\\.(.+)";

    public static final String RT_LOCAL_SOCIAL_IMAGE = "social:asiFile";

    public static final String RT_REMOTE_SOCIAL_IMAGE = "nt:adobesocialtype";

    @Reference
    private MimeTypeService mimeTypeService;

    private static final ValueMap EMPTY_PARAMS = new ValueMapDecorator(new LinkedHashMap<String, Object>());

    private static final String MIME_TYPE_PNG = "image/png";

    private static final String TYPE_QUALITY = "quality";

    private static final String TYPE_PROGRESSIVE = "progressive";

    private Pattern lastSuffixPattern = Pattern.compile(DEFAULT_FILENAME_PATTERN);

    private Map<String, NamedImageTransformer> namedImageTransformers =
            new ConcurrentHashMap<String, NamedImageTransformer>();

    private Map<String, ImageTransformer> imageTransformers = new ConcurrentHashMap<String, ImageTransformer>();

    /* Asset Rendition Pattern Picker */

    private static final String DEFAULT_ASSET_RENDITION_PICKER_REGEX = "cq5dam\\.web\\.(.*)";

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Named Transform Image Servlet",
            description = "Transform images programatically by applying a named transform to the requested Image."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Resource Types",
                description = "Resource Types and Node Types to bind this servlet to.",
                defaultValue = {
                        "nt/file",
                        "nt/resource",
                        "dam/Asset",
                        "cq/Page",
                        "cq/PageContent",
                        "nt/unstructured",
                        "foundation/components/image",
                        "foundation/components/parbase",
                        "foundation/components/page"
                }
        )
        String[] sling_servlet_resourceTypes();

        @AttributeDefinition(
                name = "Allows Suffix Patterns",
                description = "Regex pattern to filter allowed file names. Defaults to [ "
                        + NamedTransformImageServlet.DEFAULT_FILENAME_PATTERN + " ]",
                defaultValue = NamedTransformImageServlet.DEFAULT_FILENAME_PATTERN)
        String acs_commons_namedimage_filename_pattern();

        @AttributeDefinition(name = "Asset Rendition Picker Regex",
                description = "Regex to select the Rendition to transform when directly transforming a DAM Asset."
                        + " [ Default: cq5dam.web.(.*) ]",
                defaultValue = DEFAULT_ASSET_RENDITION_PICKER_REGEX)
        String prop_asset$_$rendition$_$picker$_$regex();
    }

    private static final String PROP_ASSET_RENDITION_PICKER_REGEX = "prop.asset-rendition-picker-regex";

    private RenditionPatternPicker renditionPatternPicker =
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
        final Matcher matcher = lastSuffixPattern.matcher(lastSuffix);
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
        
        if (layer == null) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Transform the image
        layer = this.transform(layer, imageTransformersWithParams);

        // Get the quality
        final double quality = this.getQuality(mimeType,
                imageTransformersWithParams.get(TYPE_QUALITY, EMPTY_PARAMS));

        // Check if the image is a JPEG which has to be encoded progressively
        final boolean progressiveJpeg = isProgressiveJpeg(mimeType,
                imageTransformersWithParams.get(TYPE_PROGRESSIVE, EMPTY_PARAMS));

        response.setContentType(mimeType);

        if (progressiveJpeg) {
            ProgressiveJpeg.write(layer, quality, response.getOutputStream());
        } else {
            layer.write(mimeType, quality, response.getOutputStream());
        }

        response.flushBuffer();
    }

    /**
     * Execute the ImageTransformers as specified by the Request's suffix segments against the Image layer.
     *
     * @param layer the Image layer
     * @param imageTransformersWithParams the transforms and their params
     * @return the transformed Image layer
     */
    protected final Layer transform(Layer layer, final ValueMap imageTransformersWithParams) {

        for (final String type : imageTransformersWithParams.keySet()) {
            if (StringUtils.equals(TYPE_QUALITY, type)) {
                // Do not process the "quality" transform in the usual manner
                continue;
            }

            final ImageTransformer imageTransformer = this.imageTransformers.get(type);
            if (imageTransformer == null) {
                log.warn("Skipping transform. Missing ImageTransformer for type: {}", type);
                continue;
            }

            final ValueMap transformParams = imageTransformersWithParams.get(type, EMPTY_PARAMS);

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
    protected final List<NamedImageTransformer> getNamedImageTransformers(final SlingHttpServletRequest request) {
        final List<NamedImageTransformer> transformers = new ArrayList<NamedImageTransformer>();

        String[] suffixes = PathInfoUtil.getSuffixSegments(request);
        if (suffixes.length < 2) {
            log.warn("Named Transform Image Servlet requires at least one named transform");
            return transformers;
        }

        int endIndex = suffixes.length - 1;
        // Its OK to check; the above check ensures there are 2+ segments
        if (StringUtils.isNumeric(PathInfoUtil.getSuffixSegment(request, suffixes.length - 2))) {
            endIndex--;
        }

        suffixes = (String[]) ArrayUtils.subarray(suffixes, 0, endIndex);

        for (final String transformerName : suffixes) {
            final NamedImageTransformer transformer = this.namedImageTransformers.get(transformerName);
            if (transformer != null) {
                transformers.add(transformer);
            }
        }

        return transformers;
    }

    /**
     * Collect and combine the image transformers and their params.
     *
     * @param selectedNamedImageTransformers the named transformers and their params
     * @return the combined named image transformers and their params
     */
    protected final ValueMap getImageTransformersWithParams(
            final List<NamedImageTransformer> selectedNamedImageTransformers) {
        final ValueMap params = new ValueMapDecorator(new LinkedHashMap<String, Object>());

        for (final NamedImageTransformer namedImageTransformer : selectedNamedImageTransformers) {
            params.putAll(namedImageTransformer.getImageTransforms());
        }

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
        final ResourceResolver resourceResolver = request.getResourceResolver();

        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
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
                || resourceResolver.isResourceType(resource, JcrConstants.NT_FILE)
                || resourceResolver.isResourceType(resource, JcrConstants.NT_RESOURCE)) {
            // For renditions; use the requested rendition
            final Image image = new Image(resource);
            image.set(Image.PN_REFERENCE, resource.getPath());
            return image;

        } else if (page != null) {
            if (resourceResolver.isResourceType(resource, NameConstants.NT_PAGE)
                    || StringUtils.equals(resource.getPath(), page.getContentResource().getPath())) {
                // Is a Page or Page's Content Resource; use the Page's image resource
                return new Image(page.getContentResource(), NAME_IMAGE);
            } else {
                return new Image(resource);
            }
        } else {
            if (resourceResolver.isResourceType(resource, RT_LOCAL_SOCIAL_IMAGE)
                    && resource.getValueMap().get("mimetype", StringUtils.EMPTY).startsWith("image/")) {
                // Is a UGC image
                return new SocialImageImpl(resource, NAME_IMAGE);
            } else if (resourceResolver.isResourceType(resource, RT_REMOTE_SOCIAL_IMAGE)) {
                // Is a UGC image
                return new SocialRemoteImageImpl(resource, NAME_IMAGE);
            }
        }

        return new Image(resource);
    }

    /**
     * Gets the mimeType of the image.
     * - The last segments suffix is looked at first and used
     * - if the last suffix segment's "extension" is .orig or .original then use the underlying resources mimeType
     * - else look up the mimeType to use based on this "extension"
     * - default to the resource's mimeType if the requested mimeType by extension is not supported.
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


    /**
     * Computes the quality based on the "synthetic" Image Quality transform params
     *
     * Image Quality does not "transform" in the usual manner (it is not a simple layer manipulation)
     * thus this ad-hoc method is required to handle quality manipulation transformations.
     *
     * If "quality" key is no available in "transforms" the default of 82 is used (magic AEM Product quality setting)
     *
     * @param mimeType the desired image mimeType
     * @param transforms the map of image transform params
     * @return
     */
    protected final double getQuality(final String mimeType, final ValueMap transforms) {
        final String key = "quality";
        final int defaultQuality = 82;
        final int maxQuality = 100;
        final int minQuality = 0;
        final int maxQualityGif = 255;
        final double oneHundred = 100D;

        log.debug("Transforming with [ quality ]");

        double quality = transforms.get(key, defaultQuality);

        if (quality > maxQuality || quality < minQuality) {
            quality = defaultQuality;
        }

        quality = quality / oneHundred;

        if (StringUtils.equals("image/gif", mimeType)) {
            quality = quality * maxQualityGif;
        }

        return quality;
    }

    /**
     * @param mimeType mime type string
     * @param transforms all transformers
     * @return <code>true</code> for jpeg mime types if progressive encoding is enabled
     */
    protected boolean isProgressiveJpeg(final String mimeType, final ValueMap transforms) {
        boolean enabled = transforms.get("enabled", false);
        if (enabled) {
            if ("image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType)) {
                return true;
            } else {
                log.debug("Progressive encoding is only supported for JPEGs. Mime type: {}", mimeType);
                return false;
            }
        } else {
            return false;
        }
    }

    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        final String regex = PropertiesUtil.toString(properties.get(PROP_ASSET_RENDITION_PICKER_REGEX),
                DEFAULT_ASSET_RENDITION_PICKER_REGEX);
        final String fileNameRegex = PropertiesUtil.toString(properties.get(NAMED_IMAGE_FILENAME_PATTERN),
                DEFAULT_FILENAME_PATTERN);
        if(StringUtils.isNotEmpty(fileNameRegex)) {
            lastSuffixPattern = Pattern.compile(fileNameRegex);
        }
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