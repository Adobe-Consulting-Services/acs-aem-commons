package com.adobe.acs.commons.search.impl;

import com.day.cq.search.Predicate;
import com.day.cq.search.eval.EvaluationContext;
import com.day.cq.search.eval.PredicateEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NativePredicateEvaluatorTest {


    PredicateEvaluator predicateEvaluator = new NativePredicateEvaluator();

    Map<String, String> predicateParams;

    @Mock
    Predicate predicate;

    @Mock
    EvaluationContext evaluationContext;

    @Before
    public void setUp() throws Exception {
        predicateParams = new HashMap<String, String>();
        when(predicate.getParameters()).thenReturn(predicateParams);
    }

    @Test
    public void canXpath() throws Exception {
        assertTrue(predicateEvaluator.canXpath(predicate, evaluationContext));
    }

    @Test
    public void canFilter() throws Exception {
        assertFalse(predicateEvaluator.canFilter(predicate, evaluationContext));
    }

    @Test
    public void getXPathExpression_allParams() throws Exception {
        final String expected = "rep:native('lucene', 'name:(Hello OR World)')";

        predicateParams.put("type", "lucene");
        predicateParams.put("expression", "name:(Hello OR World)");

        assertEquals(expected, predicateEvaluator.getXPathExpression(predicate, evaluationContext));
    }

    @Test
    public void getXPathExpression_NoSelectorParam() throws Exception {
        final String expected = "rep:native('lucene', 'name:(Hello OR World)')";

        predicateParams.put("type", "lucene");
        predicateParams.put("expression", "name:(Hello OR World)");

        assertEquals(expected, predicateEvaluator.getXPathExpression(predicate, evaluationContext));
    }

    @Test
    public void getXPathExpression_DefaultParams() throws Exception {
        final String expected = "rep:native('lucene', 'name:(Hello OR World)')";

        predicateParams.put("expression", "name:(Hello OR World)");

        assertEquals(expected, predicateEvaluator.getXPathExpression(predicate, evaluationContext));
    }


    @Test
    public void getXPathExpression_InvalidParams() throws Exception {
        final String expected = "";

        predicateParams.put("expression", "   ");

        assertEquals(expected, predicateEvaluator.getXPathExpression(predicate, evaluationContext));
    }
}