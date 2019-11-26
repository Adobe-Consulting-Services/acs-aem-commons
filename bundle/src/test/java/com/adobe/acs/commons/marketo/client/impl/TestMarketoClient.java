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
package com.adobe.acs.commons.marketo.client.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.client.MarketoClient;
import com.adobe.acs.commons.marketo.client.MarketoField;
import com.adobe.acs.commons.marketo.client.MarketoForm;

import io.wcm.testing.mock.aem.junit.AemContext;

public class TestMarketoClient {

  private MarketoClientConfiguration config = new MarketoClientConfiguration() {

    @Override
    public String getClientId() {
      return "CLIENT";
    }

    @Override
    public String getClientSecret() {
      return "SECRET";
    }

    @Override
    public String getEndpointHost() {
      return "SERVER";
    }

  };

  @Rule
  public final AemContext context = new AemContext();

  @Before
  public void init() {
    context.addModelsForPackage("com.adobe.acs.commons.marketo", "com.adobe.acs.commons.marketo.impl");
    context.load().json("/com/adobe/acs/commons/marketo/pages.json", "/content/page");
    context.load().json("/com/adobe/acs/commons/marketo/cloudconfig.json", "/etc/cloudservices/marketo/test");
  }

  @Test
  public void testError() throws IOException {
    MarketoClient client = new StaticResponseMarketoClient("/com/adobe/acs/commons/marketo/response-error.json");
    try {
      client.getForms(config);
      fail();
    } catch (IOException e) {
      assertEquals("Retrieved errors in response: Access token invalid", e.getMessage());
    }
  }

  @Test
  public void testGetFields() throws IOException {
    MarketoClient client = new StaticResponseMarketoClient(new String[] {
        "/com/adobe/acs/commons/marketo/token-response.json", "/com/adobe/acs/commons/marketo/field-response.json",
        "/com/adobe/acs/commons/marketo/response-noassets.json" });
    List<MarketoField> fields = client.getFields(config);
    assertNotNull(fields);
    assertFalse(fields.isEmpty());
    assertEquals(1, fields.size());

    assertEquals("Address", fields.get(0).getId());
  }

  @Test
  public void testGetForms() throws IOException {
    MarketoClient client = new StaticResponseMarketoClient(new String[] {
        "/com/adobe/acs/commons/marketo/token-response.json", "/com/adobe/acs/commons/marketo/form-response.json",
        "/com/adobe/acs/commons/marketo/response-noassets.json" });
    List<MarketoForm> forms = client.getForms(config);
    assertNotNull(forms);
    assertFalse(forms.isEmpty());
    assertEquals(1, forms.size());

    assertEquals("MarketoForm [id=1, locale=en_US, name=Sample Form]", forms.get(0).toString());
  }

  @Test
  public void testGetToken() throws IOException {
    MarketoClient client = new StaticResponseMarketoClient("/com/adobe/acs/commons/marketo/token-response.json");
    String token = client.getApiToken(config);
    assertNotNull(token);
    assertEquals("TOKEN", token);
  }

  @Test
  public void testNotSuccess() throws IOException {
    MarketoClient client = new StaticResponseMarketoClient("/com/adobe/acs/commons/marketo/response-notsuccess.json");
    try {
      client.getForms(config);
      fail();
    } catch (IOException e) {
      // expected to occur
    }
  }
}
