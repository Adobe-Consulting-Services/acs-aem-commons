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

package com.adobe.acs.commons.cqsearch.impl;

import com.adobe.acs.commons.cqsearch.impl.NodeExistsPredicateEvaluator;
import com.day.cq.search.Predicate;
import com.day.cq.search.eval.EvaluationContext;
import com.day.cq.search.eval.PredicateEvaluator;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Row;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class NodeExistsPredicateEvaluatorTest {
    PredicateEvaluator predicateEvaluator = new NodeExistsPredicateEvaluator();

    Session session;

    Node node;

    Map<String, String> predicateParams;

    @Mock
    Predicate predicate;

    @Mock
    Row row;

    @Mock
    EvaluationContext evaluationContext;

    @Before
    public void setUp() throws Exception {
        predicateParams = new HashMap<String, String>();

        session = MockJcr.newSession();

        node = JcrUtils.getOrCreateByPath("/content/asset.pdf", JcrConstants.NT_UNSTRUCTURED, session);

        JcrUtils.getOrCreateByPath("/content/asset.pdf/jcr:content/renditions/rendition-1", JcrConstants.NT_UNSTRUCTURED, session);
        JcrUtils.getOrCreateByPath("/content/asset.pdf/jcr:content/renditions/rendition-2", JcrConstants.NT_UNSTRUCTURED, session);
        JcrUtils.getOrCreateByPath("/content/asset.pdf/jcr:content/renditions/rendition-3", JcrConstants.NT_UNSTRUCTURED, session);
        JcrUtils.getOrCreateByPath("/content/asset.pdf/jcr:content/renditions/rendition-4", JcrConstants.NT_UNSTRUCTURED, session);
        JcrUtils.getOrCreateByPath("/content/asset.pdf/jcr:content/renditions/rendition-5", JcrConstants.NT_UNSTRUCTURED, session);

        when(row.getNode()).thenReturn(node);
        when(predicate.getParameters()).thenReturn(predicateParams);
    }


    @Test
    public void includes_AndIncludes() throws Exception {
        when(predicate.getBool(NodeExistsPredicateEvaluator.OR)).thenReturn(false);

        predicateParams.put("exists", "./jcr:content/renditions/rendition-1");
        predicateParams.put("2_exists", "jcr:content/renditions/rendition-2");
        predicateParams.put("3_exists", "jcr:content/renditions/rendition-3");
        predicateParams.put("notexists", "jcr:content/renditions/rendition-99");
        predicateParams.put("1_notexists", "null");

        assertTrue(predicateEvaluator.includes(predicate, row, evaluationContext));
    }

    @Test
    public void includes_AndExcludes() throws Exception {
        when(predicate.getBool(NodeExistsPredicateEvaluator.OR)).thenReturn(false);

        predicateParams.put("exists", "jcr:content/renditions/rendition-1");
        predicateParams.put("two_exists", "./jcr:content/renditions/rendition-2");
        predicateParams.put("3_exists", "jcr:content/renditions/rendition-3");
        predicateParams.put("notexists", "jcr:content/renditions/rendition-99");
        predicateParams.put("1_notexists", "null");
        predicateParams.put("exists", "jcr:content/renditions/rendition-100");

        assertFalse(predicateEvaluator.includes(predicate, row, evaluationContext));
    }

    @Test
    public void includes_AndEmpty() throws Exception {
        assertFalse(predicateEvaluator.canFilter(predicate, evaluationContext));
    }

    @Test
    public void includes_OrIncludes() throws Exception {
        when(predicate.getBool(NodeExistsPredicateEvaluator.OR)).thenReturn(true);

        predicateParams.put("exists", "./jcr:content/renditions/rendition-x");
        predicateParams.put("2_exists", "jcr:content/renditions/rendition-y");
        predicateParams.put("3_exists", "jcr:content/renditions/rendition-z");
        predicateParams.put("notexists", "null");

        assertTrue(predicateEvaluator.includes(predicate, row, evaluationContext));
    }

    @Test
    public void includes_OrExcludes() throws Exception {
        when(predicate.getBool(NodeExistsPredicateEvaluator.OR)).thenReturn(true);

        predicateParams.put("exists", "./jcr:content/renditions/rendition-w");
        predicateParams.put("2_exists", "jcr:content/renditions/rendition-x");
        predicateParams.put("3_exists", "jcr:content/renditions/rendition-y");
        predicateParams.put("notexists", "jcr:content/renditions/rendition-1");
        predicateParams.put("1_notexists", "jcr:content/renditions/rendition-2");

        assertFalse(predicateEvaluator.includes(predicate, row, evaluationContext));
    }

    @Test
    public void includes_OrEmpty() throws Exception {
        assertFalse(predicateEvaluator.canFilter(predicate, evaluationContext));
    }
}