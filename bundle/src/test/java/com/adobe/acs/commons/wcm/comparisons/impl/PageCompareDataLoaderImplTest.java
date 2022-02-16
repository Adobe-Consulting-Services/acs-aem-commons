/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.wcm.comparisons.impl;

import static com.adobe.acs.commons.wcm.comparisons.impl.PageCompareDataImplTest.mockResource;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLoader;


public class PageCompareDataLoaderImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);
    // otherweise we get unnecessary mocking errors; we cannot remove them because
    // they are needed to
    // make PageCompareDataImplTest working :-|

    @Test
    public void shouldInitialize() throws Exception {
        // given
        Resource resource = mockResource("/my/path", "latest", new Date());

        // when
        PageCompareDataLoader one2OneData = new PageCompareDataLoaderImpl();
        final PageCompareData data = one2OneData.load(resource, "latest");

        // then
        assertNotNull(data);

    }

}