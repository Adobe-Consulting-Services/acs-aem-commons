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
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LinesTest {

    LinesGenerator<String> one2OneLines = new LinesGenerator<String>(TO_ID);

    @Test
    public void generate_singleLine() throws Exception {
        // given
        List<String> left = Lists.newArrayList("A");
        List<String> right = Lists.newArrayList("A");

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(1));
        Line<String> line = lines.get(0);
        assertThat(line, isLineWith(Optional.of("A"), Optional.of("A")));
    }

    @Test
    public void generate_twoLines() throws Exception {
        // given
        List<String> left = Lists.newArrayList("A");
        List<String> right = Lists.newArrayList("B");

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(2));

        assertThat(lines.get(0), isLineWith(Optional.of("A"), Optional.<String>absent()));
        assertThat(lines.get(1), isLineWith(Optional.<String>absent(), Optional.of("B")));
    }

    @Test
    public void generate_twoLines_firstSpacerLeft() throws Exception {
        // given
        List<String> left = Lists.newArrayList("B");
        List<String> right = Lists.newArrayList("A", "B");

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(2));

        assertThat(lines.get(0), isLineWith(Optional.<String>absent(), Optional.of("A")));
        assertThat(lines.get(1), isLineWith(Optional.of("B"), Optional.of("B")));
    }

    @Test
    public void generate_twoLines_firstSpacerRight() throws Exception {
        // given
        List<String> left = Lists.newArrayList("A", "B");
        List<String> right = Lists.newArrayList("B");

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(2));

        assertThat(lines.get(0), isLineWith(Optional.of("A"), Optional.<String>absent()));
        assertThat(lines.get(1), isLineWith(Optional.of("B"), Optional.of("B")));
    }

    @Test
    public void generate_matrixTest1() throws Exception {
        // given
        List<String> left = Lists.newArrayList("A", "B", "C", "E");
        List<String> right = Lists.newArrayList("B", "C", "D", "E");

        /*
         *   A | -
         *   B | B
         *   C | C
         *   - | D
         *   E | E
         */

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(5));

        Iterator<Line<String>> lineIterator = lines.iterator();
        assertThat(lineIterator.next(), isLineWith(Optional.of("A"), Optional.<String>absent()));
        assertThat(lineIterator.next(), isLineWith(Optional.of("B"), Optional.of("B")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("C"), Optional.of("C")));
        assertThat(lineIterator.next(), isLineWith(Optional.<String>absent(), Optional.of("D")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("E"), Optional.of("E")));
    }

    @Test
    public void generate_matrixTest2() throws Exception {
        // given
        List<String> left = Lists.newArrayList("E", "A", "B", "C", "X");
        List<String> right = Lists.newArrayList("B", "C", "D", "E", "X");

        /*
         *   E | -
         *   A | -
         *   B | B
         *   C | C
         *   - | D
         *   - | E
         *   X | X
         */

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(7));

        Iterator<Line<String>> lineIterator = lines.iterator();
        assertThat(lineIterator.next(), isLineWith(Optional.of("E"), Optional.<String>absent()));
        assertThat(lineIterator.next(), isLineWith(Optional.of("A"), Optional.<String>absent()));
        assertThat(lineIterator.next(), isLineWith(Optional.of("B"), Optional.of("B")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("C"), Optional.of("C")));
        assertThat(lineIterator.next(), isLineWith(Optional.<String>absent(), Optional.of("D")));
        assertThat(lineIterator.next(), isLineWith(Optional.<String>absent(), Optional.of("E")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("X"), Optional.of("X")));
    }

    @Test
    public void generate_matrixTest3() throws Exception {
        // given
        List<String> left = Lists.newArrayList("B", "C", "D", "E", "X");
        List<String> right = Lists.newArrayList("E", "A", "B", "C", "X");

        /*
         *   - | E
         *   - | A
         *   B | B
         *   C | C
         *   D | -
         *   E | -
         *   X | X
         */

        // when
        final List<Line<String>> lines = one2OneLines.generate(left, right);

        // then
        assertThat(lines.size(), is(7));

        Iterator<Line<String>> lineIterator = lines.iterator();
        assertThat(lineIterator.next(), isLineWith(Optional.<String>absent(), Optional.of("E")));
        assertThat(lineIterator.next(), isLineWith(Optional.<String>absent(), Optional.of("A")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("B"), Optional.of("B")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("C"), Optional.of("C")));
        assertThat(lineIterator.next(), isLineWith(Optional.of("D"), Optional.<String>absent()));
        assertThat(lineIterator.next(), isLineWith(Optional.of("E"), Optional.<String>absent()));
        assertThat(lineIterator.next(), isLineWith(Optional.of("X"), Optional.of("X")));
    }



    public static TypeSafeMatcher<Line<String>> isLineWith(final Optional<String> left, final Optional<String> right) {
        return new TypeSafeMatcher<Line<String>>() {
            @Override
            protected boolean matchesSafely(Line<String> stringLine) {
                return left.equals(Optional.fromNullable(stringLine.getLeft()))
                        && right.equals(Optional.fromNullable(stringLine.getRight()));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with left: " + left + " right: " + right);
            }
        };
    }


    static final Function<String, Serializable> TO_ID = new Function<String, Serializable>() {
        @Nullable
        @Override
        public Serializable apply(@Nullable String input) {
            return input;
        }
    };

}