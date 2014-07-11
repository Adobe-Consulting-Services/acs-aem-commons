package com.adobe.acs.commons.workflow.bulk;

import com.day.cq.commons.jcr.JcrUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;

public class Bucket {
    private static final Logger log = LoggerFactory.getLogger(Bucket.class);
    private static final String NT_SLING_FOLDER = "sling:Folder";

    private final int bucketSize;
    private final long total;
    private final String bucketType;

    private int[] depthTracker = new int[0];
    private int bucketCount = 0;
    private String rootPath;

    public Bucket(final int bucketSize, final long total) {
        this.bucketSize = bucketSize;
        this.total = total;
        this.bucketType = NT_SLING_FOLDER;
        this.rootPath = "/content/" + String.valueOf(System.currentTimeMillis());

        this.initDepthTracker();
    }

    public Bucket(final int bucketSize, final long total, final String rootPath, final String bucketType) {
        this.bucketSize = bucketSize;
        this.total = total;
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
    private String getOrCreateBucketPath(ResourceResolver resourceResolver)
            throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);
        String folderPath = this.rootPath;

        for (int i = 0; i < this.depthTracker.length; i++) {
            final String tmp = Integer.toString(this.depthTracker[i] + 1);
            folderPath += "/" + tmp;
        }

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
    private void initDepthTracker() {
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
                log.debug("Updating depthTracker at location [ {} ] to [ {} ]", i, this.depthTracker[i]);
                break;
            }
        }
    }

    /**
     * Determines the bucket depth required to organize the pages so no more than bucketSize siblings ever exist.
     *
     * @return The node depth required to achieve desired bucket-size
     */
    private int getDepth() {
        int depth = 0;
        long remainingSize = total;

        do {
            remainingSize = (long) Math.ceil((double) remainingSize / (double) this.bucketSize);

            log.debug("Remaining size of [ {} ] at depth [ {} ]", remainingSize, depth);

            depth++;
        } while (remainingSize > this.bucketSize);

        log.debug("Final depth of [ {} ]", depth);

        return depth;
    }
}
