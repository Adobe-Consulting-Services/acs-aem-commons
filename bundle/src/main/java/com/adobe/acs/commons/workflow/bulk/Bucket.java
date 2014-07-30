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

package com.adobe.acs.commons.workflow.bulk;

import com.day.cq.commons.jcr.JcrUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;

public final class Bucket {
    private static final Logger log = LoggerFactory.getLogger(Bucket.class);
    private static final int MAX_DEPTH = 100000; // one hundred thousand

    private static final String NT_SLING_FOLDER = "sling:Folder";

    private final int bucketSize;

    private final long total;

    private final String bucketType;

    private int[] depthTracker;

    private int bucketCount = 0;

    private String rootPath;

    /**
     * Create a new Bucket.
     *
     * @param bucketSize Max number of resource per bucket
     * @param total      Total number of resources to bucket out; If this is less than bucketSize,
     *                   a single bucket will be created
     */
    public Bucket(final int bucketSize, final long total) {
        this(bucketSize, total, NT_SLING_FOLDER, "/content/" + String.valueOf(System.currentTimeMillis()));
    }

    /**
     * Create a new Bucket.
     *
     * @param bucketSize Max number of resource per bucket
     * @param total      Total number of resources to bucket out; If this is less than bucketSize,
     *                   a single bucket will be created
     * @param rootPath   the absolute path to create the buckets
     * @param bucketType nodeType used when creating the buckets
     */
    public Bucket(final int bucketSize, final long total, final String rootPath, final String bucketType) {
        this.bucketSize = bucketSize;

        if (this.bucketSize > total) {
            this.total = this.bucketSize;
        } else {
            this.total = total;
        }

        this.bucketType = bucketType;
        this.rootPath = rootPath;

        this.initDepthTracker();
    }

    /**
     * Gets and creates the path to the bucket for this
     *
     * @param resourceResolver {@link org.apache.sling.api.resource.ResourceResolver}
     * @return the absolute path to the appropriate bucket
     * @throws RepositoryException
     */
    public String getNextPath(ResourceResolver resourceResolver) throws RepositoryException {
        this.updateDepthTracker();

        if (this.needsNewBucket()) {
            this.bucketCount = 0;
        }

        final String path = this.getOrCreateBucketPath(resourceResolver);

        this.bucketCount++;

        return path;
    }

    /**
     * Determines if a new bucket is need (if the current bucket is full).
     *
     * @return true if a new bucket is required
     */
    private boolean needsNewBucket() {
        return this.bucketCount >= this.bucketSize;
    }

    /**
     * Creates the parent bucket structure to place the Page.
     *
     * @param resourceResolver the resource resolver
     * @return the path to the newly created bucket
     * @throws RepositoryException
     */
    private String getOrCreateBucketPath(final ResourceResolver resourceResolver)
            throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);

        final StringBuilder sb = new StringBuilder(this.rootPath);
        for (int i = 0; i < this.depthTracker.length; i++) {
            final String tmp = Integer.toString(this.depthTracker[i] + 1);
            sb.append("/").append(tmp);
        }

        final String folderPath = sb.toString();
        if (resourceResolver.getResource(folderPath) != null) {
            return folderPath;
        } else {
            Node node = JcrUtil.createPath(folderPath, this.bucketType, this.bucketType, session, false);
            log.debug("Created new folder path at [ {} ]", node.getPath());
            return node.getPath();
        }
    }

    /**
     * Creates and initializes the depth tracker array.
     *
     * @return the depth tracker array initialized to all 0's
     */
    private void initDepthTracker() throws IllegalStateException {
        int depth = getDepth();

        this.depthTracker = new int[depth];
        Arrays.fill(depthTracker, 0);
    }

    /**
     * Manages tracker used to determine the parent bucket structure.
     *
     * @return The updated depth tracker array
     */
    private void updateDepthTracker() {
        if (!this.needsNewBucket()) {
            return;
        }

        for (int i = this.depthTracker.length - 1; i >= 0; i--) {
            if (this.depthTracker[i] >= this.bucketSize - 1) {
                this.depthTracker[i] = 0;
            } else {
                this.depthTracker[i] = this.depthTracker[i] + 1;
                log.debug("Updating depthTracker: {}", Arrays.toString(this.depthTracker));
                break;
            }
        }
    }

    /**
     * Determines the bucket depth required to organize the pages so no more than bucketSize siblings ever exist.
     *
     * @return The node depth required to achieve desired bucket-size
     */
    private int getDepth() throws IllegalStateException {
        int depth = 0;
        long remainingSize = this.total;

        if(this.bucketSize < 2) {
            throw new IllegalStateException("Trying to build bucket structure with bucket size [ "
                    + this.bucketSize
                    + "]. Refusing as this does not make sense, and is a flat list of nodes.");
        }

        do {
            remainingSize = (long) Math.ceil((double) remainingSize / (double) this.bucketSize);

            log.debug("Remaining size of [ {} ] at depth [ {} ]", remainingSize, depth);

            depth++;
        } while (remainingSize > this.bucketSize && depth < MAX_DEPTH);


        if(depth == MAX_DEPTH) {
            throw new IllegalStateException("Bucket Max Depth of {} reached. Cowardly refusing to support such a large bucket " +
                    "structure");
        }

        log.debug("Final depth of [ {} ]", depth);

        return depth;
    }
}
