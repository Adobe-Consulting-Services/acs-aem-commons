/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.throttling;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CpuLoadEstimatorTest {

    @Test
    public void testCalculations() {
        assertEquals(5, CpuLoadEstimator.calculateRequests(5, 50, 5));
        assertEquals(5, CpuLoadEstimator.calculateRequests(100, 100, 5));

        assertEquals(5, CpuLoadEstimator.calculateRequests(85, 70, 10)); // (100-85) / (100-70) * 10
    }

}
