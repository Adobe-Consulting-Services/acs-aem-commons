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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestShardGenerator {
    @Test
    public void testSharding(){
        assertShard(3, "3");
        assertShard(33, "33");
        assertShard(333, "33/3");
        assertShard(3333, "33/33");
        assertShard(33333, "33/33/3");
    }

    void assertShard(int n, String expected){
        ShardGenerator generator = new ShardGenerator("/var/test");
        for(int i = 0; i < n; i++){
            generator.nextShard();
        }
        assertEquals(expected, generator.getShard());
    }
}
