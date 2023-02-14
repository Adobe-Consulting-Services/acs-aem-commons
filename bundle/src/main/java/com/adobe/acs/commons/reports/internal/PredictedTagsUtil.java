/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2023 Adobe
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PredictedTagsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictedTagsUtil.class);

    public static final double MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE = 0.0;

    /**
     * For the given resource and relative property path:
     * Validate that the resource is an asset, extract all predicted tags below the relative property path, adapt them
     * to PredictedTags and filter out those whose confidence value is lower than the lowerConfidenceThreshold
     * @param resource the resource
     * @param relativePropertyPath the property path of the predicted tags, relative to the resource
     * @param lowerConfidenceThreshold the lower threshold for confidence values
     * @return the list of predicted tags, sorted (desc) by confidence
     */
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
                && lowerConfidenceThresholdValue >= MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE
                ? lowerConfidenceThresholdValue
                : MINIMUM_LOWER_CONFIDENCE_THRESHOLD_VALUE;
    }
}
