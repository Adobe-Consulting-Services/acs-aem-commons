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
package com.adobe.acs.commons.wcm.comparisons.impl.lines;

import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.adobe.acs.commons.wcm.comparisons.lines.Line.State.EQUAL;
import static com.adobe.acs.commons.wcm.comparisons.lines.Line.State.NOT_EQUAL;
import static com.adobe.acs.commons.wcm.comparisons.lines.Line.State.ONLY_LEFT;
import static com.adobe.acs.commons.wcm.comparisons.lines.Line.State.ONLY_RIGHT;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LineImplTest {

    @Test
    public void construct_static_right() throws Exception {
        // when
        Line<String> underTest = LineImpl.right("test");

        // then
        assertNotNull("right should be initialized", underTest.getRight());
        assertThat(underTest.getRight(), is("test"));

        assertNull("left should not be initialized", underTest.getLeft());

        assertThat(underTest.getState(), is(ONLY_RIGHT));
    }

    @Test
    public void construct_static_left() throws Exception {
        // when
        Line<String> underTest = LineImpl.left("test");

        // then
        assertNull("right should not be initialized", underTest.getRight());

        assertNotNull("left should be initialized", underTest.getLeft());
        assertThat(underTest.getLeft(), is("test"));

        assertThat(underTest.getState(), is(ONLY_LEFT));
    }

    @Test
    public void construct_static_both_notEqual() throws Exception {
        // when
        Line<String> underTest = LineImpl.both("left", "right");

        // then
        assertNotNull("right should be initialized", underTest.getRight());
        assertThat(underTest.getRight(), is("right"));

        assertNotNull("left should be initialized", underTest.getLeft());
        assertThat(underTest.getLeft(), is("left"));

        assertThat(underTest.getState(), is(NOT_EQUAL));
    }

    @Test
    public void construct_static_both_equal() throws Exception {
        // when
        Line<String> underTest = LineImpl.both("same", "same");

        // then
        assertNotNull("right should be initialized", underTest.getRight());
        assertThat(underTest.getRight(), is("same"));

        assertNotNull("left should be initialized", underTest.getLeft());
        assertThat(underTest.getLeft(), is("same"));

        assertThat(underTest.getState(), is(EQUAL));
    }

}