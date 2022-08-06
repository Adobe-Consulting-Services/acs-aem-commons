/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.quickly.results;

import com.day.cq.wcm.api.AuthoringUIMode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a Quickly Result
 */
public class Result {

    public enum Mode {
        DEV("dev"),
        ANY("any");

        private final String value;

        Mode(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private Action action;
    private AuthoringUIMode authoringMode;
    private String description;
    private List<Mode> modes;
    private String path;
    private String resultType;
    private Action secondaryAction;
    private String title;

    private Result(final Builder builder) {
        this.setResultType(builder.resultType);
        this.setTitle(builder.title);
        this.setDescription(builder.description);
        this.setPath(builder.path);
        this.setAction(builder.action);
        this.setSecondaryAction(builder.secondaryAction);
        this.setAuthoringMode(builder.authoringMode);
        this.setModes(builder.modes);
    }

    /* Action */

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        if (action == null) {
            this.action = new Action.Builder().build();
        } else {
            this.action = action;
        }
    }

    /* Authoring UI Mode */

    public AuthoringUIMode getAuthoringMode() {
        return authoringMode;
    }

    public void setAuthoringMode(final AuthoringUIMode authoringMode) {
        this.authoringMode = authoringMode;
    }

    /* Description */

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /* Quickly Modes */

    public List<Mode> getModes() {
        return Optional.ofNullable(this.modes)
                .map(Collections::unmodifiableList)
                .orElse(Collections.emptyList());
    }

    public void setModes(final List<Mode> modes) {
        if (CollectionUtils.isEmpty(modes)) {
            this.modes = Collections.singletonList(Mode.ANY);
        } else {
            this.modes = new ArrayList<>(modes);
        }
    }

    /* Path */

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    /* Result Type */

    public String getResultType() {
        return StringUtils.upperCase(resultType);
    }

    public void setResultType(final String resultType) {
        this.resultType = StringUtils.upperCase(resultType);
    }

    /* Secondary Action */

    public Action getSecondaryAction() {
        return secondaryAction;
    }

    public void setSecondaryAction(final Action secondaryAction) {
        if (secondaryAction == null) {
            this.secondaryAction = new Action.Builder().build();
        } else {
            this.secondaryAction = secondaryAction;
        }
    }

    /* Title */

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Result Builder
     */
    public static class Builder {

        private Action action;
        private AuthoringUIMode authoringMode = null;
        private String description;
        private List<Mode> modes = new ArrayList<Mode>();
        private String path;
        private String resultType;
        private Action secondaryAction;
        private String title;

        /**
         * Constructor
         * @param title initialized Result w a title
         */
        public Builder(String title) {
            this.title = title;
        }

        /**
         * Build method
         * @return the built Result obj
         */
        public Result build() {
            return new Result(this);
        }

        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder resultType(String resultType) {
            this.resultType = resultType;
            return this;
        }

        public Builder secondaryAction(Action secondaryAction) {
            this.secondaryAction = secondaryAction;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /* Parameter-less Setters */

        public Builder classic() {
            this.authoringMode = AuthoringUIMode.CLASSIC;
            return this;
        }

        public Builder touch() {
            this.authoringMode = AuthoringUIMode.TOUCH;
            return this;
        }

        public Builder dev() {
            this.modes.add(Mode.DEV);
            return this;
        }
    }
}
