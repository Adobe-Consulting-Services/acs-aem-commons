/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.models.via.impl;

import com.adobe.acs.commons.models.injectors.impl.InjectorAEMContext;
import com.adobe.acs.commons.models.via.annotations.PageContentResourceViaType;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class PageContentResourceViaProviderTest
{


    @Rule
    public final AemContext context = InjectorAEMContext.provide();

    @Test
    public void test(){
        context.currentPage("/content/we-retail/language-masters/en/experience");
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root");

        PageContentResourceViaProvider systemUnderTest = new PageContentResourceViaProvider();

        Object newAdaptableResourcePage = systemUnderTest.getAdaptable(context.currentResource(), "");
        Resource resourcePageJcrContent = (Resource) newAdaptableResourcePage;
        assertEquals("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content", resourcePageJcrContent.getPath());



        Object newAdaptableCurrentPage = systemUnderTest.getAdaptable(context.request(), PageContentResourceViaProvider.VIA_CURRENT_PAGE);
        Resource currentPageJcrContent = (Resource) newAdaptableCurrentPage;
        assertEquals("/content/we-retail/language-masters/en/experience/jcr:content", currentPageJcrContent.getPath());
    }




}
