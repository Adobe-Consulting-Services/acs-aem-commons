/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.marketo.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.acs.commons.marketo.FormValue;

import io.wcm.testing.mock.aem.junit.AemContext;

public class TestFormValueImpl {

  @Rule
  public final AemContext context = new AemContext();

  @Before
  public void init() {
    context.addModelsForPackage("com.adobe.acs.commons.marketo", "com.adobe.acs.commons.marketo.impl");
    context.load().json("/com/adobe/acs/commons/marketo/formvalue.json", "/content");
  }

  @Test
  public void valid() throws IOException {
    context.currentResource("/content/formvalue/jcr:content/root/valid");
    FormValue formValue = Optional.ofNullable(context.currentResource()).map(r -> r.adaptTo(FormValue.class))
        .orElse(null);
    assertNotNull(formValue);

    assertEquals("Test", formValue.getName());
    assertEquals("static", formValue.getSource());
    assertEquals("Value", formValue.getValue());
  }
}
