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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * LinesGenerator&lt;T&gt; combines two list of values (left and right) to a comparing map. Values with same id are in one line, with different ids in different lines. Example:
 * left:   A, B, C
 * right:  B, C, E
 * result:
 * A  |  -
 * B  |  B
 * C  |  C
 * -  |  E
 * IDs are created with Function<T, Serializable>
 *
 * @param <T>
 */
public class LinesGenerator<T> {

    private final Function<T, Serializable> toId;

    private Stepper<T> left;
    private Stepper<T> right;

    private T leftValue;
    private int leftSpacer;

    private T rightValue;
    private int rightSpacer;

    public LinesGenerator(Function<T, Serializable> toId) {
        this.toId = toId;
    }

    public List<Line<T>> generate(final Iterable<T> left, Iterable<T> right) {
        this.left = new Stepper<T>(left, toId);
        this.right = new Stepper<T>(right, toId);

        List<Line<T>> lines = new ArrayList<Line<T>>();

        this.leftValue = this.left.next();
        this.rightValue = this.right.next();

        do {
            this.leftSpacer = this.right.positionOfIdAfterCurrent(leftValue);
            this.rightSpacer = this.left.positionOfIdAfterCurrent(rightValue);

            if (leftValue != null && rightValue != null && toId.apply(leftValue).equals(toId.apply(rightValue))) {
                addPair(lines);

            } else if (leftSpacer < rightSpacer && leftSpacer > 0) {
                addWithLeftSpacers(lines);

            } else if (rightSpacer > 0) {
                addWithRightSpacers(lines);

            } else if (leftSpacer > 0) {
                addWithLeftSpacers(lines);

            } else {
                addSeperated(lines);

            }
        } while (leftValue != null || rightValue != null);

        return lines;
    }

    private void addSeperated(List<Line<T>> lines) {
        if (leftValue != null) {
            lines.add(LineImpl.left(leftValue));
            leftValue = this.left.next();
        }
        if (rightValue != null) {
            lines.add(LineImpl.right(rightValue));
            rightValue = this.right.next();
        }
    }

    private void addWithLeftSpacers(List<Line<T>> lines) {
        for (int i = 0; i < leftSpacer; i++) {
            lines.add(LineImpl.right(rightValue));
            rightValue = this.right.next();
        }
    }

    private void addWithRightSpacers(List<Line<T>> lines) {
        for (int i = 0; i < rightSpacer; i++) {
            lines.add(LineImpl.left(leftValue));
            leftValue = this.left.next();
        }
    }

    private void addPair(List<Line<T>> lines) {
        lines.add(LineImpl.both(leftValue, rightValue));
        this.leftValue = this.left.next();
        this.rightValue = this.right.next();
    }


}
