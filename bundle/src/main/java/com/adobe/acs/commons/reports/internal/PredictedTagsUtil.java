package com.adobe.acs.commons.reports.internal;

import com.adobe.acs.commons.reports.models.PredictedTag;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.util.PathUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PredictedTagsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictedTagsUtil.class);

    public static final double MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE = 0.0;

    public List<PredictedTag> getPredictedTags(final Resource resource,
                                               final String relativePropertyPath,
                                               final Double lowerConfidenceThreshold) {

        final double validatedLowerConfidenceValue = validateLowerConfidenceThreshold(lowerConfidenceThreshold);

        if (resource == null) {
            LOGGER.error("getPredictedTags : The given resource is null, hence returning empty list.");
            return Collections.emptyList();
        }

        final Asset asset = resolveToAsset(resource);
        if (asset == null) {
            LOGGER.warn("getPredictedTags : The given resource could not be resolved to an asset, hence returning empty list.");
            return Collections.emptyList();
        }

        final Resource predictedTagsResource = getPredictedTagsResource(resource, asset, relativePropertyPath);
        if (predictedTagsResource == null) {
            LOGGER.info("getPredictedTags : No predicted tags found at the property path, hence returning empty list.");
            return Collections.emptyList();
        }

        final List<Resource> predictedTagResources = ImmutableList.copyOf(predictedTagsResource.getChildren());
        final List<PredictedTag> predictedTags = new ArrayList<>();
        for (final Resource predictedTagResource : predictedTagResources) {
            final PredictedTag predictedTag = predictedTagResource.adaptTo(PredictedTag.class);
            if (predictedTag != null && (predictedTag.getConfidence() >= validatedLowerConfidenceValue)) {
                predictedTags.add(predictedTag);
            }
        }

        sortByConfidence(predictedTags);

        LOGGER.debug("getPredictedTags : Loaded predictedTags {}.", predictedTagResources);
        return predictedTags;
    }


    /**
     * Sort the list of predicted tag sorted by confidence
     * @param tagList
     */
    protected void sortByConfidence(final List<PredictedTag> tagList) {
        tagList.sort((p1, p2) -> {
            final double p1Confidence = p1 != null ? p1.getConfidence() : MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE;
            final double p2Confidence = p2 != null ? p2.getConfidence() : MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE;
            // invert order: elements with the highest confidence go first
            return -Double.compare(p1Confidence, p2Confidence);
        });
    }

    /**
     * Resolve the resource to an asset. Return null if the resource cannot be resolved to an asset
     * @param resource the resource
     * @return the resource as asset. Return null if the resource cannot be resolved to an asset
     */
    public Asset resolveToAsset(final Resource resource) {
        return DamUtil.resolveToAsset(resource);
    }


    protected Resource getPredictedTagsResource(final Resource resource,
                                                final Asset asset,
                                                final String relativePropertyPath) {
        if (resource == null || asset == null) {
            return null;
        }

        final ResourceResolver resourceResolver = resource.getResourceResolver();
        String predictedTagsPath;
        if (StringUtils.isNotBlank(relativePropertyPath)) {
            predictedTagsPath = PathUtil.append(resource.getPath(), relativePropertyPath);
        } else {
            predictedTagsPath = resource.getPath();
        }

        final Resource predictedTagsResource = resourceResolver.getResource(predictedTagsPath);
        if (predictedTagsResource == null) {
            // fallback on expected standard path
            predictedTagsPath = PathUtil.append(asset.getPath(), DamConstants.PREDICTED_TAGS);
            return resourceResolver.getResource(predictedTagsPath);
        }
        return predictedTagsResource;
    }

    /**
     * Validate and return lower confidence threshold. Value must be a double and not less than 0.0
     * @param lowerConfidenceThresholdValue the input threshold value
     * @return the validated lower confidence threshold
     */
    protected double validateLowerConfidenceThreshold(final Double lowerConfidenceThresholdValue) {
        return lowerConfidenceThresholdValue != null
                && !lowerConfidenceThresholdValue.isNaN()
                && !lowerConfidenceThresholdValue.isInfinite()
                && lowerConfidenceThresholdValue >= MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE ?
                lowerConfidenceThresholdValue :
                MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE;
    }
}
