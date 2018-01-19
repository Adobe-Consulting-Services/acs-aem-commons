/*
 *
 *  * #%L
 *  * ACS AEM Commons Bundle
 *  * %%
 *  * Copyright (C) 2016 Adobe
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 *
 */
package com.adobe.acs.commons.wcm.comparisons.impl;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

@RunWith(MockitoJUnitRunner.class)
public class CompareFilterTest {

    @Test
    public void shouldInitialize() throws Exception {
        String[] ignoreProperties = {"propa", "propb"};
        String[] ignoreResources = {"resa", "resb"};

        CompareFilter compareFilter = new CompareFilter(ignoreProperties, ignoreResources);

        assertThat(getInternalState(compareFilter, "ignoreProperties"), CoreMatchers.<Object>is(ignoreProperties));
        assertThat(getInternalState(compareFilter, "ignoreResources"), CoreMatchers.<Object>is(ignoreResources));
    }

    @Test
    public void filterProperty_ignorePropertyMatches_shouldFilter() throws Exception {
        String[] ignoreProperties = {"(.*/)?jcr:primaryType"};
        String[] ignoreResources = {};

        CompareFilter compareFilter = new CompareFilter(ignoreProperties, ignoreResources);

        assertThat("should filter my/jcr:primaryType", compareFilter.filterProperty("my/jcr:primaryType"), is(true));
    }

    @Test
    public void filterProperty_ignorePropertyDoesntMatch_shouldNotFilter() throws Exception {
        String[] ignoreProperties = {"(.*/)?jcr:primaryType"};
        String[] ignoreResources = {};

        CompareFilter compareFilter = new CompareFilter(ignoreProperties, ignoreResources);

        assertThat("should not filter my/jcr:uuid", compareFilter.filterProperty("my/jcr:uuid"), is(false));
    }

    @Test
    public void filterResource_ignoreResourceMatches_shouldFilter() throws Exception {
        String[] ignoreProperties = {};
        String[] ignoreResources = {"(.*/)test_[0-9]*"};

        CompareFilter compareFilter = new CompareFilter(ignoreProperties, ignoreResources);

        assertThat("should filter a/test_1", compareFilter.filterResource("a/test_1"), is(true));
    }

    @Test
    public void filterResource_ignoreResourceDoesntMatch_shouldNotFilter() throws Exception {
        String[] ignoreProperties = {};
        String[] ignoreResources = {"(.*/)test_[0-9]*"};

        CompareFilter compareFilter = new CompareFilter(ignoreProperties, ignoreResources);

        assertThat("should not filter a/zweitertest_1", compareFilter.filterResource("a/zweitertest_1"), is(false));
    }


}