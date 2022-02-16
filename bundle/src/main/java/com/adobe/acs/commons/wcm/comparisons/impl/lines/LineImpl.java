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

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import com.google.common.base.Optional;

class LineImpl<T> implements Line<T> {

    private final Optional<T> left;
    private final Optional<T> right;

    static <T> Line<T> right(final T rightValue) {
        return new LineImpl<T>(null, rightValue);
    }

    static <T> Line<T> left(final T leftValue) {
        return new LineImpl<T>(leftValue, null);
    }

    static <T> Line<T> both(final T leftValue, final T rightValue) {
        return new LineImpl<T>(leftValue, rightValue);
    }

    private LineImpl(T left, T right) {
        this.left = Optional.fromNullable(left);
        this.right = Optional.fromNullable(right);
    }

    public T getLeft() {
        return left.orNull();
    }

    public T getRight() {
        return right.orNull();
    }

    @Override
    public State getState() {
        if (!left.isPresent()) {
            return State.ONLY_RIGHT;
        }
        if (!right.isPresent()) {
            return State.ONLY_LEFT;
        }
        if (left.get().equals(right.get())) {
            return State.EQUAL;
        }
        return State.NOT_EQUAL;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("left", left)
                .append("right", right)
                .toString();
    }
}
