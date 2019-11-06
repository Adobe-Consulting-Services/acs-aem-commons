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

import static org.junit.Assert.*;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TagsExportServiceTest {

  @Rule
  public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

  private static final String PATH_6_3 = "/etc";

  private static final String PATH_OVER_6_4 = "/content";

  private static final String EXPECTED_OUTPUT_BRAND_TEST_NON_LOCALIZED = "root {{root}},brandTest {{brandTest}},Products {{products}},,,\n"
      + "root {{root}},brandTest {{brandTest}},Products {{products}},Milk {{milk}},,\n"
      + "root {{root}},brandTest {{brandTest}},Products {{products}},Cereals {{cereals}},,\n"
      + "root {{root}},brandTest {{brandTest}},Products {{products}},Cereals {{cereals}},Other {{other}},\n"
      + "root {{root}},brandTest {{brandTest}},Products {{products}},Cereals {{cereals}},Oat {{oat}},\n";


  private static final String EXPECTED_OUTPUT_BRAND_TEST_LOCALIZED = "en[root] {{root}},en[brandTest] {{brandTest}},es[Produtos] fr[Products in french] en[Products] {{products}},,,\n"
      + "en[root] {{root}},en[brandTest] {{brandTest}},es[Produtos] fr[Products in french] en[Products] {{products}},en[Milk] {{milk}},,\n"
      + "en[root] {{root}},en[brandTest] {{brandTest}},es[Produtos] fr[Products in french] en[Products] {{products}},en[Cereals] {{cereals}},,\n"
      + "en[root] {{root}},en[brandTest] {{brandTest}},es[Produtos] fr[Products in french] en[Products] {{products}},en[Cereals] {{cereals}},ru[овсяной] en[Other] {{other}},\n"
      + "en[root] {{root}},en[brandTest] {{brandTest}},es[Produtos] fr[Products in french] en[Products] {{products}},en[Cereals] {{cereals}},be[belarussian oat] en[Oat] {{oat}},\n";

  private static final String EXPECTED_OUTPUT_COMMA_IN_TITLE_LOCALIZED = "en[root] {{root}},en[commaInTitles] {{commaInTitles}},es[Produtos] fr[Products in french] en[Products] {{products}},\n";

  private static final String EXPECTED_OUTPUT_UNLOCALIZED_TO_LOCALIZED_DEFAULT = "en[root] {{root}},en[Zero Localized Title] {{zeroLocalizedTitle}},en[Products] {{products}},\n";

  private static final String EXPECTED_OUTPUT_UNLOCALIZED_TO_LOCALIZED_PL = "en[root] {{root}},pl[Zero Localized Title] {{zeroLocalizedTitle}},pl[Products] {{products}},\n";

  private static final String EXPECTED_OUTPUT_WRONG_PATH_MESSAGE = "Path '/etc/wrong_tags/root/brandTest' do not contains tag root. Probably You've made mistake during typing path. Export tags cannot be done.";

  private TagsExportService tagsExportService;

  @Before
  public void setUp() {
    context.load()
        .json(getClass().getResourceAsStream("brandTestTags.json"), PATH_6_3);
    tagsExportService = new TagsExportService();
  }

  @Test
  public void checkIsStructureExpected_toNonLocalized() {
    String result = tagsExportService.exportNonLocalizedTagsForPath("/etc/tags/root/brandTest", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_BRAND_TEST_NON_LOCALIZED, result);
  }

  @Test
  public void checkIsStructureExpected_toLocalized() {
    String result = tagsExportService.exportLocalizedTagsForPath("/etc/tags/root/brandTest", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_BRAND_TEST_LOCALIZED, result);
  }

  @Test
  public void commasAreRemovedFromTitles_toLocalized() {
    String result = tagsExportService.exportLocalizedTagsForPath("/etc/tags/root/commaInTitles", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_COMMA_IN_TITLE_LOCALIZED, result);
  }

  @Test
  public void wrongPathDoNotExportAnything_toLocalized() {
    String result = tagsExportService.exportLocalizedTagsForPath("/etc/wrong_tags/root/brandTest", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_WRONG_PATH_MESSAGE, result);
  }

  @Test
  public void tagsWithoutLocalization_defaultLanguageWillBeUsed_toLocalized() {
    String result = tagsExportService.exportLocalizedTagsForPath("/etc/tags/root/zeroLocalizedTitle", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_UNLOCALIZED_TO_LOCALIZED_DEFAULT, result);
  }

  @Test
  public void tagsWithoutLocalization_chosenLanguageWillBeUsed_toLocalized() {
    String result = tagsExportService.exportLocalizedTagsForPath("/etc/tags/root/zeroLocalizedTitle", context.resourceResolver(),"pl");
    assertEquals(EXPECTED_OUTPUT_UNLOCALIZED_TO_LOCALIZED_PL, result);
  }

  @Test
  public void tagsUnderCqTags_shouldRecognizeCorrectlyNonLocalized() {
    context.load()
        .json(getClass().getResourceAsStream("brandTestCqTags.json"), PATH_OVER_6_4);
    String result = tagsExportService.exportNonLocalizedTagsForPath("/content/cq:tags/root/brandTest", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_BRAND_TEST_NON_LOCALIZED, result);
  }

  @Test
  public void tagsUnderCqTags_shouldRecognizeCorrectlyLocalized() {
    context.load()
        .json(getClass().getResourceAsStream("brandTestCqTags.json"), PATH_OVER_6_4);
    String result = tagsExportService.exportLocalizedTagsForPath("/content/cq:tags/root/brandTest", context.resourceResolver());
    assertEquals(EXPECTED_OUTPUT_BRAND_TEST_LOCALIZED, result);
  }

}