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
package com.adobe.acs.commons.wcm.comparisons.impl;

import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import com.adobe.acs.commons.wcm.comparisons.lines.Lines;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PageCompareDataLinesTest {

    Lines<PageCompareDataLine> underTest = new PageCompareDataLinesImpl();

    @Test
    public void generate_emptyCollections_shouldGenerateList() throws Exception {
        // given
        List<PageCompareDataLine> left = Collections.emptyList();
        List<PageCompareDataLine> right = Collections.emptyList();

        // when
        final List<Line<PageCompareDataLine>> result = underTest.generate(left, right);

        // then
        assertThat(result, is(Collections.<Line<PageCompareDataLine>>emptyList()));
    }

    @Test
    public void generate_oneLine_shouldGenerateList() throws Exception {
        // given

        PageCompareDataLine leftLine = mock(PageCompareDataLine.class);
        when(leftLine.getUniqueName()).thenReturn("left");

        List<PageCompareDataLine> left = Lists.newArrayList(leftLine);

        PageCompareDataLine rightLine = mock(PageCompareDataLine.class);
        when(rightLine.getUniqueName()).thenReturn("right");

        List<PageCompareDataLine> right = Lists.newArrayList(rightLine);

        // when
        final List<Line<PageCompareDataLine>> result = underTest.generate(left, right);

        // then
        assertThat(result, is(not(Collections.<Line<PageCompareDataLine>>emptyList())));
        assertThat(result.get(0).getLeft(), is(leftLine));
        assertThat(result.get(0).getRight(), is(nullValue()));
        assertThat(result.get(1).getLeft(), is(nullValue()));
        assertThat(result.get(1).getRight(), is(rightLine));
    }

}