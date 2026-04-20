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
package com.adobe.acs.commons.models.injectors.impl.model.impl;

import com.adobe.acs.commons.models.injectors.annotation.TagProperty;
import com.adobe.acs.commons.models.injectors.impl.model.TestSharedValueMapValueModel;
import com.adobe.acs.commons.models.injectors.impl.model.TestTagPropertyInjectModel;
import com.day.cq.tagging.Tag;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Model(adapters = TestTagPropertyInjectModel.class, adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TestTagPropertyInjectModelImpl implements TestTagPropertyInjectModel {


    @TagProperty(value = "cq:tags", injectionStrategy = InjectionStrategy.OPTIONAL)
    Tag singleTag;

    @TagProperty(value="singleTagInherited", inherit = true, injectionStrategy = InjectionStrategy.OPTIONAL)
    Tag singleTagInherited;

    @TagProperty(value="cq:tags", injectionStrategy = InjectionStrategy.OPTIONAL)
    Tag[] multipleTagsArray;

    @TagProperty(value="cq:tags", injectionStrategy = InjectionStrategy.OPTIONAL)
    Set<Tag> multipleTagsSet;

    @TagProperty(value="cq:tags", injectionStrategy = InjectionStrategy.OPTIONAL)
    List<Tag> multipleTagsList;

    @TagProperty(value="cq:tags",injectionStrategy = InjectionStrategy.OPTIONAL)
    Collection<Tag> multipleTagsCollection;

    @Override
    public Tag getSingleTag() {
        return singleTag;
    }

    @Override
    public Tag getSingleTagInherited() {
        return singleTagInherited;
    }

    @Override
    public Tag[] getMultipleTagsArray() {
        return multipleTagsArray;
    }

    @Override
    public Set<Tag> getMultipleTagsSet() {
        return multipleTagsSet;
    }

    @Override
    public List<Tag> getMultipleTagsList() {
        return multipleTagsList;
    }

    @Override
    public Collection<Tag> getMultipleTagsCollection() {
        return multipleTagsCollection;
    }
}
