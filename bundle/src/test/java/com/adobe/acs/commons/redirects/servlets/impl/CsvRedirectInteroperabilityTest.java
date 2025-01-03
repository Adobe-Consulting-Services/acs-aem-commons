package com.adobe.acs.commons.redirects.servlets.impl;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.redirects.models.ImportLog;
import com.adobe.acs.commons.redirects.models.RedirectRule;


@RunWith(MockitoJUnitRunner.class)
public class CsvRedirectInteroperabilityTest {

  @Mock
  private ImportLog importLog;

  @Mock
  private RedirectRule mockRule;

  private CsvRedirectExporter exporter;
  private CsvRedirectImporter importer;

  @Before
  public void setUp() {
      exporter = new CsvRedirectExporter();
      importer = new CsvRedirectImporter(importLog);
  }

  @Test
  public void testBasicInterop() throws IOException {
      // Setup mock rule
      when(mockRule.getSource()).thenReturn("/content/old");
      when(mockRule.getTarget()).thenReturn("/content/new");
      when(mockRule.getStatusCode()).thenReturn(301);

      // Export
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      List<RedirectRule> rules = new ArrayList<>();
      rules.add(mockRule);
      exporter.export(rules, outputStream);

      // Import
      ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
      Collection<Map<String, Object>> importedRules = importer.read(inputStream);

      // Verify
      assertEquals(1, importedRules.size());
      Map<String, Object> importedRule = importedRules.iterator().next();
      assertEquals("/content/old", importedRule.get(RedirectRule.SOURCE_PROPERTY_NAME));
      assertEquals("/content/new", importedRule.get(RedirectRule.TARGET_PROPERTY_NAME));
      assertEquals("301", importedRule.get(RedirectRule.STATUS_CODE_PROPERTY_NAME));
  }

  @Test
  public void testComplexInterop() throws IOException {
      // Setup mock rule with all fields
      Calendar untilDate = createCalendar(2024, 12, 31);
      Calendar effectiveFrom = createCalendar(2024, 1, 1);
      String[] tags = new String[]{"tag1", "tag2"};

      when(mockRule.getSource()).thenReturn("/old");
      when(mockRule.getTarget()).thenReturn("/new");
      when(mockRule.getStatusCode()).thenReturn(301);
      when(mockRule.getUntilDate()).thenReturn(untilDate);
      when(mockRule.getEffectiveFrom()).thenReturn(effectiveFrom);
      when(mockRule.getNote()).thenReturn("Test note");
      when(mockRule.getEvaluateURI()).thenReturn(true);
      when(mockRule.getContextPrefixIgnored()).thenReturn(true);
      when(mockRule.getTagIds()).thenReturn(tags);
      when(mockRule.getCacheControlHeader()).thenReturn("no-cache");

      // Export
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      List<RedirectRule> rules = new ArrayList<>();
      rules.add(mockRule);
      exporter.export(rules, outputStream);

      // Import
      ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
      Collection<Map<String, Object>> importedRules = importer.read(inputStream);

      // Verify
      assertEquals(1, importedRules.size());
      Map<String, Object> importedRule = importedRules.iterator().next();

      assertEquals("/old", importedRule.get(RedirectRule.SOURCE_PROPERTY_NAME));
      assertEquals("/new", importedRule.get(RedirectRule.TARGET_PROPERTY_NAME));
      assertEquals("301", importedRule.get(RedirectRule.STATUS_CODE_PROPERTY_NAME));
      assertEquals("Test note", importedRule.get(RedirectRule.NOTE_PROPERTY_NAME));
      assertEquals(true, importedRule.get(RedirectRule.EVALUATE_URI_PROPERTY_NAME));
      assertEquals(true, importedRule.get(RedirectRule.CONTEXT_PREFIX_IGNORED_PROPERTY_NAME));
      assertArrayEquals(tags, (String[]) importedRule.get(RedirectRule.TAGS_PROPERTY_NAME));
      assertEquals("no-cache", importedRule.get(RedirectRule.CACHE_CONTROL_HEADER_NAME));

      // Verify dates
      Calendar importedUntilDate = (Calendar) importedRule.get(RedirectRule.UNTIL_DATE_PROPERTY_NAME);
      Calendar importedEffectiveFrom = (Calendar) importedRule.get(RedirectRule.EFFECTIVE_FROM_PROPERTY_NAME);

      assertNotNull(importedUntilDate);
      assertNotNull(importedEffectiveFrom);
      assertEquals(2024, importedUntilDate.get(Calendar.YEAR));
      assertEquals(Calendar.DECEMBER, importedUntilDate.get(Calendar.MONTH));
      assertEquals(31, importedUntilDate.get(Calendar.DAY_OF_MONTH));
      assertEquals(2024, importedEffectiveFrom.get(Calendar.YEAR));
      assertEquals(Calendar.JANUARY, importedEffectiveFrom.get(Calendar.MONTH));
      assertEquals(1, importedEffectiveFrom.get(Calendar.DAY_OF_MONTH));
  }

  @Test
  public void testMultipleRulesInterop() throws IOException {
      // Setup mock rules
      RedirectRule rule1 = createMockRule("/old1", "/new1", 301);
      RedirectRule rule2 = createMockRule("/old2", "/new2", 302);

      // Export
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      List<RedirectRule> rules = new ArrayList<>();
      rules.add(rule1);
      rules.add(rule2);
      exporter.export(rules, outputStream);

      // Import
      ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
      Collection<Map<String, Object>> importedRules = importer.read(inputStream);

      // Verify
      assertEquals(2, importedRules.size());
      boolean foundRule1 = false;
      boolean foundRule2 = false;

      for (Map<String, Object> rule : importedRules) {
          String source = (String) rule.get(RedirectRule.SOURCE_PROPERTY_NAME);
          String target = (String) rule.get(RedirectRule.TARGET_PROPERTY_NAME);
          String statusCode = (String) rule.get(RedirectRule.STATUS_CODE_PROPERTY_NAME);

          if ("/old1".equals(source) && "/new1".equals(target) && "301".equals(statusCode)) {
              foundRule1 = true;
          } else if ("/old2".equals(source) && "/new2".equals(target) && "302".equals(statusCode)) {
              foundRule2 = true;
          }
      }

      assertTrue("Rule 1 should be found", foundRule1);
      assertTrue("Rule 2 should be found", foundRule2);
  }

  private RedirectRule createMockRule(String source, String target, int statusCode) {
      RedirectRule rule = mock(RedirectRule.class);
      when(rule.getSource()).thenReturn(source);
      when(rule.getTarget()).thenReturn(target);
      when(rule.getStatusCode()).thenReturn(statusCode);
      return rule;
  }

  private Calendar createCalendar(int year, int month, int day) {
      LocalDate date = LocalDate.of(year, month, day);
      return GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));
  }

}
