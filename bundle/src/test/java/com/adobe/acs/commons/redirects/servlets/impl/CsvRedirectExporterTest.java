package com.adobe.acs.commons.redirects.servlets.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.redirects.models.RedirectRule;

@RunWith(MockitoJUnitRunner.class)
public class CsvRedirectExporterTest {

    private CsvRedirectExporter exporter;
    private ByteArrayOutputStream outputStream;

    @Mock
    private RedirectRule mockRule;

    @Before
    public void setUp() {
        exporter = new CsvRedirectExporter();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testBasicExport() throws IOException {
        // Setup
        when(mockRule.getSource()).thenReturn("/content/old");
        when(mockRule.getTarget()).thenReturn("/content/new");
        when(mockRule.getStatusCode()).thenReturn(301);

        // Execute
        exporter.export(Collections.singletonList(mockRule), outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("/content/old"));
        assertTrue(output.contains("/content/new"));
        assertTrue(output.contains("301"));
    }

    @Test
    public void testExportWithDates() throws IOException {
        // Setup
        Calendar untilDate = createCalendar(2024, 12, 31);
        Calendar effectiveFrom = createCalendar(2024, 1, 1);

        when(mockRule.getSource()).thenReturn("/old");
        when(mockRule.getTarget()).thenReturn("/new");
        when(mockRule.getStatusCode()).thenReturn(301);
        when(mockRule.getUntilDate()).thenReturn(untilDate);
        when(mockRule.getEffectiveFrom()).thenReturn(effectiveFrom);

        // Execute
        exporter.export(Collections.singletonList(mockRule), outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("Dec 31, 2024"));
        assertTrue(output.contains("Jan 1, 2024"));
    }

    @Test
    public void testExportWithTags() throws IOException {
        // Setup
        when(mockRule.getSource()).thenReturn("/old");
        when(mockRule.getTarget()).thenReturn("/new");
        when(mockRule.getStatusCode()).thenReturn(301);
        when(mockRule.getTagIds()).thenReturn(new String[]{"tag1", "tag2"});

        // Execute
        exporter.export(Collections.singletonList(mockRule), outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("tag1\ntag2"));
    }

    @Test
    public void testExportWithNotes() throws IOException {
        // Setup
        when(mockRule.getSource()).thenReturn("/old");
        when(mockRule.getTarget()).thenReturn("/new");
        when(mockRule.getStatusCode()).thenReturn(301);
        when(mockRule.getNote()).thenReturn("Test note");

        // Execute
        exporter.export(Collections.singletonList(mockRule), outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("Test note"));
    }

    @Test
    public void testExportMultipleRules() throws IOException {
        // Setup
        RedirectRule rule1 = createMockRule("/old1", "/new1", 301);
        RedirectRule rule2 = createMockRule("/old2", "/new2", 302);
        List<RedirectRule> rules = Arrays.asList(rule1, rule2);

        // Execute
        exporter.export(rules, outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("/old1"));
        assertTrue(output.contains("/new1"));
        assertTrue(output.contains("/old2"));
        assertTrue(output.contains("/new2"));
    }

    @Test
    public void testExportEmptyRules() throws IOException {
        // Execute
        exporter.export(Collections.emptyList(), outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("Source")); // Header should still be present
        assertEquals(1, output.split("\n").length); // Only header line
    }

    @Test
    public void testExportWithAllFields() throws IOException {
        // Setup
        when(mockRule.getSource()).thenReturn("/old");
        when(mockRule.getTarget()).thenReturn("/new");
        when(mockRule.getStatusCode()).thenReturn(301);
        when(mockRule.getNote()).thenReturn("Test note");
        when(mockRule.getEvaluateURI()).thenReturn(true);
        when(mockRule.getContextPrefixIgnored()).thenReturn(true);
        when(mockRule.getTagIds()).thenReturn(new String[]{"tag1"});
        when(mockRule.getCacheControlHeader()).thenReturn("no-cache");
        when(mockRule.getCreatedBy()).thenReturn("admin");
        when(mockRule.getModifiedBy()).thenReturn("admin");

        Calendar created = createCalendar(2024, 1, 1);
        Calendar modified = createCalendar(2024, 1, 2);
        when(mockRule.getCreated()).thenReturn(created);
        when(mockRule.getModified()).thenReturn(modified);

        // Execute
        exporter.export(Collections.singletonList(mockRule), outputStream);

        // Verify
        String output = outputStream.toString("UTF-8");
        assertTrue(output.contains("Test note"));
        assertTrue(output.contains("true"));
        assertTrue(output.contains("tag1"));
        assertTrue(output.contains("no-cache"));
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("Jan 1, 2024"));
        assertTrue(output.contains("Jan 2, 2024"));
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