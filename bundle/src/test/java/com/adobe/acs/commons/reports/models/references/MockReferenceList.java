/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.models.references;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;

import com.adobe.granite.references.Reference;
import com.adobe.granite.references.ReferenceList;

public class MockReferenceList extends ArrayList<Reference> implements ReferenceList {

  private static final long serialVersionUID = 1L;
  private Resource resource;

  public MockReferenceList(Resource resource) {
    this.resource = resource;
  }

  @Override
  public Resource getResource() {
    return this.resource;
  }

  @Override
  public List<Reference> subList(String... types) {
    return this;
  }

}
