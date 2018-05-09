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

import com.google.common.base.Function;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;

import static com.adobe.acs.commons.wcm.comparisons.impl.lines.StepperTest.Step.step;
import static com.adobe.acs.commons.wcm.comparisons.impl.lines.StepperTest.Step.toId;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class StepperTest {

    @Test
    public void shouldStepOverAll() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));

        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        assertThat(underTest.next(), is(steps.get(0)));
        assertThat(underTest.next(), is(steps.get(1)));
        assertThat(underTest.next(), is(steps.get(2)));
        assertNull(underTest.next());
    }

    @Test
    public void shouldFindPositionOfFirst() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        final int position = underTest.positionOfIdAfterCurrent(steps.get(0));

        assertThat(position, is(1));
    }

    @Test
    public void shouldFindPositionOfSecond() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        final int position = underTest.positionOfIdAfterCurrent(steps.get(1));

        assertThat(position, is(2));
    }

    @Test
    public void shouldFindRelativePositionOfSecond() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        underTest.next();
        final int position = underTest.positionOfIdAfterCurrent(steps.get(1));

        assertThat(position, is(1));
    }

    @Test
    public void shouldNotFindRelativePositionOfFirstAfterSecond() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        underTest.next();
        final int position = underTest.positionOfIdAfterCurrent(steps.get(0));

        assertThat(position, is(-1));
    }

    @Test
    public void shouldNotBeEmptyOnStart() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        assertFalse(underTest.isEmpty());
    }

    @Test
    public void shouldNotBeEmptyOnSecond() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        underTest.next();
        underTest.next();
        assertFalse(underTest.isEmpty());
    }

    @Test
    public void shouldBeEmptyOnLast() throws Exception {
        List<Step> steps = newArrayList(step("1"), step("2"), step("3"));
        Stepper<Step> underTest = new Stepper<Step>(steps, toId);

        underTest.next();
        underTest.next();
        underTest.next();
        assertTrue(underTest.isEmpty());
    }

    static class Step {
        final String id;

        Step(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        static Step step(String id) {
            return new Step(id);
        }

        static Function<Step, Serializable> toId = new Function<Step, Serializable>() {
            @Override
            public String apply(Step step) {
                return step.id();
            }
        };
    }
}