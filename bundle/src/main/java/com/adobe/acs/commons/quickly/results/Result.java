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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {

    public enum Target {
        BLANK("_blank"),
        TOP("_top"),
        SELF("_self");

        private final String value;

        Target(final String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }
    }

    public enum Method {
        GET("get"),
        POST("post"),
        NOOP("noop"),
        JS("js"),
        CMD("cmd");

        private final String value;

        Method(final String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }
    }

    public enum Mode {
        DEV("dev"),
        ANY("any");

        private final String value;

        Mode(final String value) {
            this.value = value;
        }

        public String toString() {
            return this.value;
        }
    }

    private String resultType;

    private String title;

    private String description;

    private String path;

    private String actionURI;

    private Method actionMethod;

    private String actionScript;

    private Target actionTarget;

    private String autoComplete;

    private Map<String, String> actionParams;

    private AuthoringUIMode authoringMode;

    private List<Mode> modes;

    private Result(final Builder builder) {
        this.setResultType(builder.resultType);
        this.setTitle(builder.title);
        this.setDescription(builder.description);
        this.setPath(builder.path);
        this.setActionURI(builder.actionURI);
        this.setActionMethod(builder.actionMethod);
        this.setActionTarget(builder.actionTarget);
        this.setActionParams(builder.actionParams);
        this.setActionScript(builder.actionScript);
        this.setAutoComplete(builder.autoComplete);

        this.setAuthoringMode(builder.authoringMode);
        this.setModes(builder.modes);
    }

    public String getAutoComplete() {
        return autoComplete;
    }

    public void setAutoComplete(final String autoComplete) {
        this.autoComplete = autoComplete;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(final String resultType) {
        this.resultType = resultType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getActionURI() {
        return actionURI;
    }

    public void setActionURI(final String action) {
        this.actionURI = action;
    }

    public Method getActionMethod() {
        return this.actionMethod;
    }

    public void setActionMethod(final Method actionMethod) {
        this.actionMethod = actionMethod;
    }

    public Target getActionTarget() {
        return this.actionTarget;
    }

    public void setActionTarget(final Target actionTarget) {
        this.actionTarget = actionTarget;
    }

    public String getActionScript() {
        return actionScript;
    }

    public void setActionScript(final String actionScript) {
        this.actionScript = actionScript;
    }

    public Map<String, String> getActionParams() {
        if (actionParams == null) {
            return new HashMap<String, String>();
        } else {
            return actionParams;
        }
    }

    public void setActionParams(final Map<String, String> actionParams) {
        this.actionParams = actionParams;
    }

    public AuthoringUIMode getAuthoringMode() {
        return authoringMode;
    }

    public void setAuthoringMode(final AuthoringUIMode authoringMode) {
        this.authoringMode = authoringMode;
    }

    public List<Mode> getModes() {
        return this.modes;
    }

    public void setModes(final List<Mode> modes) {
        if (this.modes == null) {
            this.modes = new ArrayList<Mode>();
        }

        if (modes == null || CollectionUtils.isEmpty(modes)) {
            this.modes.add(Mode.ANY);
        } else {
            this.modes = modes;
        }
    }

    public static class Builder {

        private String autoComplete;

        private String resultType;

        private String title;

        private String description;

        private String path;

        private String actionURI;

        private Method actionMethod = Method.GET;

        private Target actionTarget = Target.SELF;

        private String actionScript;

        private Map<String, String> actionParams;

        private AuthoringUIMode authoringMode = null;

        private List<Mode> modes = new ArrayList<Mode>();

        public Builder(String title) {
            this.title = title;
        }

        public Result build() {
            return new Result(this);
        }

        public Builder resultType(String resultType) {
            this.resultType = resultType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder autoComplete(String autoComplete) {
            this.autoComplete = autoComplete;
            return this;
        }

        public Builder actionURI(String actionURI) {
            this.actionURI = actionURI;
            return this;
        }

        public Builder actionMethod(Method actionMethod) {
            this.actionMethod = actionMethod;
            return this;
        }

        public Builder actionTarget(Target actionTarget) {
            this.actionTarget = actionTarget;
            return this;
        }

        public Builder actionParams(Map<String, String> actionParams) {
            this.actionParams = actionParams;
            return this;
        }

        public Builder actionScript(String actionScript) {
            this.actionScript = actionScript;
            this.actionMethod = Method.JS;
            return this;
        }

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
