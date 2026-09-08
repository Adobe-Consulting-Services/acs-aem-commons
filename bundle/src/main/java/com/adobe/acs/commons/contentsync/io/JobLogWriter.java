/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import com.adobe.acs.commons.contentsync.ContentSyncService;

/**
 * Writing log messages to sharded JCR nodes.
 * 
 * The writer buffers log messages and periodically flushes them to a JCR resource,
 * creating new shards as needed to avoid oversized nodes.
 * <p>
 * Usage:
 * <pre>
 * try (JobLogWriter writer = new JobLogWriter(resolverFactory, nodePath)) {
 *     writer.write("message 1");
 *     writer.write("message 2");
 *     // ...
 * }
 * </pre>
 */
public class JobLogWriter implements AutoCloseable {
    /**
     * Default maximum number of messages per shard.
     */
    static final int BUCKET_SIZE = 1024;

    /**
     * Default flush interval in milliseconds.
     */
    static final int FLUSH_INTERVAL_MS = 1000;

    /**
     * Property name for storing log messages in the JCR node.
     */
    static final String DATA_PROPERTY = "messages";

    /**
     * The ResourceResolverFactory used to obtain service resource resolvers.
     */
    final ResourceResolverFactory resolverFactory;

    /**
     * Buffer for accumulating log messages before flushing.
     */
    final List<String> buffer;

    /**
     * ShardGenerator for generating sharded node paths.
     */
    final ShardGenerator shardGenerator;

    /**
     * Maximum number of messages per shard.
     */
    final int bucketSize;

    /**
     * Flush interval in milliseconds.
     */
    final int flushInterval;

    /**
     * Timestamp of the last flush operation.
     */
    long lastFlushed;

   /**
     * Constructs a JobLogWriter with custom bucket size, flush interval, and shard width.
     *
     * @param resolverFactory the ResourceResolverFactory
     * @param nodePath        the base path for log shards
     * @param bucketSize      the maximum number of messages per shard
     * @param flushInterval   the flush interval in milliseconds or -1 to disable
     * @param shardWidth      the width of each shard
     */
    public JobLogWriter(ResourceResolverFactory resolverFactory, String nodePath, int bucketSize, int flushInterval, int shardWidth) {
        this.resolverFactory = resolverFactory;
        this.buffer = new ArrayList<>();
        this.shardGenerator = new ShardGenerator(nodePath, shardWidth);
        this.bucketSize = bucketSize;
        this.flushInterval = flushInterval;
        this.lastFlushed = System.currentTimeMillis();
   }

    /**
     * Constructs a JobLogWriter with default bucket size, flush interval, and shard width.
     *
     * @param resolverFactory the ResourceResolverFactory
     * @param nodePath        the base path for log shards
     */
    public JobLogWriter(ResourceResolverFactory resolverFactory, String nodePath) {
        this(resolverFactory, nodePath, BUCKET_SIZE, FLUSH_INTERVAL_MS, ShardGenerator.DEFAULT_SHARD_SIZE);
    }

    /**
     * Writes a log message to the buffer and flushes if needed.
     *
     * @param msg the log message
     * @throws IOException if an error occurs during flushing
     */
    public void write(String msg) throws IOException {
        buffer.add(msg);
        flushIfNeeded();
    }

    /**
     * Flushes the buffer to a JCR resource. If the buffer reaches the bucket size,
     * a new shard is created for subsequent messages.
     *
     * @throws IOException if an error occurs during resource access or writing
     */
    public void flush() throws IOException {
        if(buffer.isEmpty()) {
            // nothing to flush
            return;
        }
        try (ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(ContentSyncService.AUTH_INFO)) {
            String shardPath = shardGenerator.getPath();

            Resource res = ResourceUtil.getOrCreateResource(resourceResolver, shardPath,
                    JcrConstants.NT_UNSTRUCTURED, JcrResourceConstants.NT_SLING_FOLDER, false);
            res.adaptTo(ModifiableValueMap.class).put(DATA_PROPERTY, buffer.toArray(new String[0]));
            resourceResolver.commit();
            lastFlushed = System.currentTimeMillis();
        } catch (LoginException e){
            throw new IOException(e);
        }

        if (buffer.size() >= bucketSize) {
            // create next shard
            shardGenerator.nextShard();
            buffer.clear();
        }
    }

    /**
     * Flushes any remaining messages in the buffer when closing the writer.
     *
     * @throws IOException if an error occurs during flushing
     */
    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * Flushes the buffer if the bucket is full or the flush interval has elapsed.
     *
     * @throws IOException if an error occurs during flushing
     */
    void flushIfNeeded() throws IOException {
        boolean isBucketFull = buffer.size() >= bucketSize;
        boolean flushTimeout = flushInterval > 0 && System.currentTimeMillis() - lastFlushed > flushInterval;
        if (isBucketFull || flushTimeout) {
            flush();
        }
    }
}
