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

import com.adobe.acs.commons.wcm.comparisons.One2OneData;
import com.adobe.acs.commons.wcm.comparisons.One2OneDataLoader;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static com.adobe.acs.commons.wcm.comparisons.impl.One2OneDataImplTest.mockResource;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class One2OneDataLoaderImplTest {

    @Test
    public void shouldInitialize() throws Exception {
        // given
        Resource resource = mockResource("/my/path", "latest", new Date());

        // when
        One2OneDataLoader one2OneData = new One2OneDataLoaderImpl();
        final One2OneData data = one2OneData.load(resource, "latest");

        // then
        assertNotNull(data);

    }

}