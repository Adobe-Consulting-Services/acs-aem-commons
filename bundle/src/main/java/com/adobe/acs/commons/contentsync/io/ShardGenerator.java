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
 * Generate shards based on an incremental counter.
 * Shards a separated by slashes, e.g.
 * <pre>
 *     3     -> 3
 *     33    -> 33
 *     333   -> 33/3
 *     3333  -> 33/33
 *     33333 -> 33/33/3
 * </pre>
 */
public class ShardGenerator {
    public static int DEFAULT_SHARD_SIZE = 2;

    private final String parentPath;
    private final int shardSize; //  characters per level, e.g. 1/2/3 vs 10/21/33
    private int counter;

    public ShardGenerator(String parentPath){
        this(parentPath, DEFAULT_SHARD_SIZE);
    }

    public ShardGenerator(String parentPath, int shardSize){
        this.parentPath = parentPath;
        this.shardSize = shardSize;
    }

    public void nextShard(){
        counter++;
    }

    /**
     * split the number string into shards of size {@link #shardSize} from left to right
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
     * @return  path to the current nshard
     */
    public String getPath() {
        return parentPath + "/" + getShard();
    }
}
