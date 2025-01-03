package com.adobe.acs.commons.redirects.servlets.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.redirects.models.ImportLog;
import com.adobe.acs.commons.redirects.models.RedirectRule;


@RunWith(MockitoJUnitRunner.class)
public class CsvRedirectImporterTest {

    @Mock
    private ImportLog importLog;

    private CsvRedirectImporter importer;

    @Before
    public void setUp() {
        importer = new CsvRedirectImporter(importLog);
    }

    @Test
    public void testBasicImport() throws IOException {
        String csv = "source,target,statusCode\n" +
                    "/content/old,/content/new,301";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(1, rules.size());
        Map<String, Object> rule = rules.iterator().next();
        assertEquals("/content/old", rule.get(RedirectRule.SOURCE_PROPERTY_NAME));
        assertEquals("/content/new", rule.get(RedirectRule.TARGET_PROPERTY_NAME));
        assertEquals("301", rule.get(RedirectRule.STATUS_CODE_PROPERTY_NAME));
    }

    @Test
    public void testImportWithOptionalFields() throws IOException {
        String csv = "Source Url,Target Url,Status Code,Evaluate URI,Notes\n" +
                    "/old,/new,301,true,Test note";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(1, rules.size());
        Map<String, Object> rule = rules.iterator().next();
        assertEquals(true, rule.get(RedirectRule.EVALUATE_URI_PROPERTY_NAME));
        assertEquals("Test note", rule.get(RedirectRule.NOTE_PROPERTY_NAME));
    }

    @Test
    public void testImportWithDates() throws IOException {
        String csv = "source,target,statusCode,untilDate,effectiveFrom\n" +
                    "/old,/new,301,\"Dec 31, 2024\",\"Jan 1, 2024\"";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(1, rules.size());
        Map<String, Object> rule = rules.iterator().next();

        Calendar untilDate = (Calendar) rule.get(RedirectRule.UNTIL_DATE_PROPERTY_NAME);
        Calendar effectiveFrom = (Calendar) rule.get(RedirectRule.EFFECTIVE_FROM_PROPERTY_NAME);

        assertNotNull(untilDate);
        assertNotNull(effectiveFrom);
        assertEquals(2024, untilDate.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, untilDate.get(Calendar.MONTH));
        assertEquals(31, untilDate.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testImportWithTags() throws IOException {
        String csv = "source,target,statusCode,tags\n" +
                    "/old,/new,301,\"tag1\ntag2\"";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(1, rules.size());
        Map<String, Object> rule = rules.iterator().next();
        String[] tags = (String[]) rule.get(RedirectRule.TAGS_PROPERTY_NAME);
        assertArrayEquals(new String[]{"tag1", "tag2"}, tags);
    }

    @Test
    public void testInvalidStatusCode() throws IOException {
        String csv = "source,target,statusCode\n" +
                    "/old,/new,invalid";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(0, rules.size());
        verify(importLog).warn(eq("Row 1"), anyString());
    }

    @Test
    public void testMissingRequiredFields() throws IOException {
        String csv = "source,target,statusCode\n" +
                    ",/new,301";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(0, rules.size());
        verify(importLog).warn(eq("Row 1"), anyString());
    }

    @Test
    public void testInvalidDate() throws IOException {
        String csv = "source,target,statusCode,untilDate\n" +
                    "/old,/new,301,invalid-date";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(1, rules.size());
        Map<String, Object> rule = rules.iterator().next();
        assertNull(rule.get(RedirectRule.UNTIL_DATE_PROPERTY_NAME));
        verify(importLog).info(anyString(), anyString());
    }

    @Test
    public void testMultipleRules() throws IOException {
        String csv = "source,target,statusCode\n" +
                    "/old1,/new1,301\n" +
                    "/old2,/new2,302";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(2, rules.size());
    }

    @Test
    public void testEmptyCsv() throws IOException {
        String csv = "source,target,statusCode";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(0, rules.size());
    }

    @Test
    public void testWhitespaceHandling() throws IOException {
        String csv = "source,target,statusCode\n" +
                    "  /old  ,  /new  ,  301  ";

        Collection<Map<String, Object>> rules = importCsv(csv);

        assertEquals(1, rules.size());
        Map<String, Object> rule = rules.iterator().next();
        assertEquals("/old", rule.get(RedirectRule.SOURCE_PROPERTY_NAME).toString().trim());
        assertEquals("/new", rule.get(RedirectRule.TARGET_PROPERTY_NAME).toString().trim());
    }

    private Collection<Map<String, Object>> importCsv(String csv) throws IOException {
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        return importer.read(is);
    }

}
