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

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;

/**
 * Iterator for traversing sharded job log nodes in the JCR.
 * <p>
 * Each call to {@link #next()} returns the log messages (as a String array)
 * for the current shard node, and advances to the next shard.
 * <p>
 * Usage:
 * <pre>
 * JobLogIterator it = new JobLogIterator(resource);
 * while (it.hasNext()) {
 *     String[] messages = it.next();
 *     // process messages
 * }
 * </pre>
 */
public class JobLogIterator implements Iterator<String[]> {
    /**
     * The root resource node containing the log shards.
     */
    private Resource node;

    /**
     * ShardGenerator for generating shard paths.
     */
    private ShardGenerator shardGenerator;

    /**
     * The current shard resource node.
     */
    private Resource shardNode;

    /**
     * Constructs a JobLogIterator with the default shard size.
     *
     * @param node the root resource node containing log shards
     */
    public JobLogIterator(Resource node){
        this.node = node;
        this.shardGenerator = new ShardGenerator(node.getPath());
    }

    /**
     * Constructs a JobLogIterator with a custom shard size.
     *
     * @param node the root resource node containing log shards
     * @param shardSize the width of each shard
     */
    public JobLogIterator(Resource node, int shardSize){
        this.node = node;
        this.shardGenerator = new ShardGenerator(node.getPath(), shardSize);
    }

    /**
     * Checks if there is another shard with log messages.
     *
     * @return true if another shard exists, false otherwise
     */
    @Override
    public boolean hasNext() {
        String shardPath = shardGenerator.getPath();
        shardNode =  node.getResourceResolver().getResource(shardPath);
        shardGenerator.nextShard();
        return shardNode != null;
    }

    /**
     * Returns the log messages for the current shard node.
     *
     * @return an array of log messages for the current shard, or null if none
     */
    @Override
    public String[] next() {
        if(shardNode == null) throw new IllegalStateException("hasNext() was not called");

        return shardNode.getValueMap().get(JobLogWriter.DATA_PROPERTY, String[].class);
    }
}
