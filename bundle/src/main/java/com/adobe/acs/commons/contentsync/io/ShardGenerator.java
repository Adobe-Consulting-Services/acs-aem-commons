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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for generating hierarchical shard paths based on an incremental counter.
 * <p>
 * Shards are split from the counter value into segments of configurable size,
 * separated by slashes. For example, with a shard size of 2:
 * <pre>
 *   counter = 3     -> "3"
 *   counter = 33    -> "33"
 *   counter = 333   -> "33/3"
 *   counter = 3333  -> "33/33"
 *   counter = 33333 -> "33/33/3"
 * </pre>
 * The full path is constructed by appending the shard string to the parent path.
 */
public class ShardGenerator {
    /**
     * Default number of characters per shard segment.
     */
    public static int DEFAULT_SHARD_SIZE = 2;

    /**
     * The parent path to which shards are appended.
     */
    private final String parentPath;

    /**
     * Number of characters per shard segment.
     */
    private final int shardSize;

    /**
     * Internal counter used to generate shard values.
     */
    private int counter;

    /**
     * Constructs a ShardGenerator with the default shard size.
     *
     * @param parentPath the base path for shards
     */
    public ShardGenerator(String parentPath){
        this(parentPath, DEFAULT_SHARD_SIZE);
    }

    /**
     * Constructs a ShardGenerator with a custom shard size.
     *
     * @param parentPath the base path for shards
     * @param shardSize  the number of characters per shard segment
     */
    public ShardGenerator(String parentPath, int shardSize){
        this.parentPath = parentPath;
        this.shardSize = shardSize;
    }

    /**
     * Advances the internal counter to the next shard.
     */
    public void nextShard(){
        counter++;
    }

    /**
     * Splits the current counter value into shard segments of size {@link #shardSize}.
     *
     * @return the shard string (e.g. "33/3" for counter=333 and shardSize=2)
     */
    public String getShard(){
        String numStr = String.valueOf(counter);
        List<String> shards = new ArrayList<>();
        for(int i = 0; i < numStr.length(); i += shardSize){
            int endIndex = Math.min(i + shardSize, numStr.length());
            String shard = numStr.substring(i, endIndex);
            shards.add(shard);
        }
        return String.join("/", shards);
    }

    /**
     * Returns the full path to the current shard, combining the parent path and shard string.
     *
     * @return the full path to the current shard
     */
    public String getPath() {
        return parentPath + "/" + getShard();
    }
}
