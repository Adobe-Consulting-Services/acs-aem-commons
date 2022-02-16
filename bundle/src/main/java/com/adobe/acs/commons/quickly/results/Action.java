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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Result Action or Secondary Action
 */
public class Action {

    public enum Target {
        BLANK("_blank"),
        TOP("_top"),
        SELF("_self");

        private final String value;

        Target(final String value) {
            this.value = value;
        }

        @Override
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

        @Override
        public String toString() {
            return this.value;
        }
    }

    private String uri;
    private Method method;
    private Map<String, String> params;
    private String script;
    private Target target;

    private Action(final Builder builder) {
        this.setMethod(builder.method);
        this.setParams(builder.params);
        this.setScript(builder.script);
        this.setTarget(builder.target);
        this.setUri(builder.uri);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        if (params == null) {
            this.params = new HashMap<String, String>();
        } else {
            this.params = params;
        }
    }

    public static class Builder {
        private Method method = Method.GET;
        private Map<String, String> params;
        private String script;
        private Target target = Target.SELF;
        private String uri = "#";

        public Action build() {
            return new Action(this);
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public Builder target(Target target) {
            this.target = target;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder script(String script) {
            this.script = script;
            this.method = Method.JS;
            return this;
        }
    }
}
