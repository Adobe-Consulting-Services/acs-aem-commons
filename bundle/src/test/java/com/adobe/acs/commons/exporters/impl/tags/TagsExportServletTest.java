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
package com.adobe.acs.commons.exporters.impl.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TagsExportServletTest {

  private static final String BRAND_TAGS_ROOT = "/etc/tags/root/brandTest";

  private static final String PARAMETER_PATH = "path";

  private static final String PARAMETER_LOCALIZED = "localized";

  private static final String PARAMETER_DEFAULT_LOCALIZATION = "defaultLocalization";

  private static final String EXPECTED_OUTPUT_WRONG_PATH_MESSAGE = "Path '/wrong/Path' do not contains tag root. Probably You've made mistake during typing path. Export tags cannot be done.";

  @Rule
  public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private TagsExportServlet servlet;

  private TagsExportService service;

  @Before
  public void setUp() throws NoSuchFieldException, IllegalAccessException {
    service = new TagsExportService();
    context.load()
        .json(getClass().getResourceAsStream("brandTestTags.json"), "/etc");
    context.registerService(TagsExportService.class, service);
    servlet = new TagsExportServlet();
    mockExportServiceInServlet();
  }

  @Test
  public void testCorrectParameters_defaultLocalizationAppliedOnlyToTagWithoutLocalization() throws Exception {
    Map<String, Object> params = ImmutableMap.of(
        PARAMETER_PATH, "/etc/tags/root/zeroLocalizedTitle",
        PARAMETER_LOCALIZED,"true",
        PARAMETER_DEFAULT_LOCALIZATION,"pl");

    context.request().setParameterMap(params);
    servlet.doGet(context.request(), context.response());

    assertEquals(HttpStatus.SC_OK, context.response().getStatus());
    String output = context.response().getOutputAsString();
    assertTrue(output.contains("en[root] {{root}}"));
    assertTrue(output.contains("pl[Zero Localized Title] {{zeroLocalizedTitle}}"));
  }

  @Test
  public void testWrongPath_emptyOutput() throws Exception {
    Map<String, Object> params = ImmutableMap.of(
        PARAMETER_PATH, "/wrong/Path",
        PARAMETER_LOCALIZED,"true",
        PARAMETER_DEFAULT_LOCALIZATION,"pl");

    context.request().setParameterMap(params);
    servlet.doGet(context.request(), context.response());

    assertEquals(HttpStatus.SC_OK, context.response().getStatus());
    String output = context.response().getOutputAsString();
    assertEquals(EXPECTED_OUTPUT_WRONG_PATH_MESSAGE, output);
  }

  @Test
  public void testMissingPath_emptyOutput() throws Exception {
    Map<String, Object> params = ImmutableMap.of(
        PARAMETER_LOCALIZED,"true",
        PARAMETER_DEFAULT_LOCALIZATION,"pl");

    context.request().setParameterMap(params);
    servlet.doGet(context.request(), context.response());

    assertEquals(HttpStatus.SC_OK, context.response().getStatus());
    String output = context.response().getOutputAsString();
    assertTrue(output.isEmpty());
  }

  @Test
  public void testCorrectParameters_nonLocalizedOutput() throws Exception {
    Map<String, Object> params = ImmutableMap.of(
        PARAMETER_PATH, BRAND_TAGS_ROOT,
        PARAMETER_LOCALIZED,"false",
        PARAMETER_DEFAULT_LOCALIZATION,"pl");

    context.request().setParameterMap(params);
    servlet.doGet(context.request(), context.response());

    assertEquals(HttpStatus.SC_OK, context.response().getStatus());
    String output = context.response().getOutputAsString();
    assertTrue(output.contains("Products {{products}}"));
    assertFalse(output.contains("pl["));
    assertFalse(output.contains("en["));
  }

  @Test
  public void missingLocalizationParams_defaultParamsApplied_nonLocalizedOutput() throws Exception {
    Map<String, Object> params = ImmutableMap.of(
        PARAMETER_PATH, BRAND_TAGS_ROOT);

    context.request().setParameterMap(params);
    servlet.doGet(context.request(), context.response());

    assertEquals(HttpStatus.SC_OK, context.response().getStatus());
    String output = context.response().getOutputAsString();
    assertTrue(output.contains("Products {{products}}"));
    assertFalse(output.contains("pl["));
    assertFalse(output.contains("en["));
  }

  private void mockExportServiceInServlet()
      throws NoSuchFieldException, IllegalAccessException {
    Field serviceField = servlet.getClass().getDeclaredField("tagsExportService");
    serviceField.setAccessible(true);
    serviceField.set(servlet, service);
  }
}