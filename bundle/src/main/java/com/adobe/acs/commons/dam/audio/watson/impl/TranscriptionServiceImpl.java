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
package com.adobe.acs.commons.dam.audio.watson.impl;


import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.http.HttpClientFactory;
import com.adobe.acs.commons.http.JsonObjectResponseHandler;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


@Component
public class TranscriptionServiceImpl implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionServiceImpl.class);

    private static final JsonObjectResponseHandler HANDLER = new JsonObjectResponseHandler();

    @Reference(target = "(factory.name=watson-speech-to-text)")
    private HttpClientFactory httpClientFactory;

    @Override
    public String startTranscriptionJob(InputStream stream, String mimeType) {
        Request request = httpClientFactory.post("/speech-to-text/api/v1/recognitions?continuous=true&timestamps=true")
                .addHeader("Content-Type", mimeType)
                .bodyStream(stream);

        try {
            JsonObject json = (JsonObject) httpClientFactory.getExecutor().execute(request).handleResponse(HANDLER);
            Gson gson = new Gson();
            log.trace("content: {}", gson.toJson(json));
            return json.get("id").getAsString();
        } catch (IOException e) {
            log.error("error submitting job", e);
            return null;
        }
    }

    @Override
    public Result getResult(String jobId) {
        log.debug("getting result for {}", jobId);
        Request request = httpClientFactory.get("/speech-to-text/api/v1/recognitions/" + jobId);
        try {
            JsonObject json = (JsonObject) httpClientFactory.getExecutor().execute(request).handleResponse(HANDLER);

            Gson gson = new Gson();
            log.trace("content: {}", gson.toJson(json));
            if (json.has("status") && json.get("status").getAsString().equals("completed")) {
                JsonArray results = json.get("results").getAsJsonArray().get(0).getAsJsonObject().get("results").getAsJsonArray();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    JsonObject result = results.get(i).getAsJsonObject();
                    if (result.get("final").getAsBoolean()) {
                        JsonObject firstAlternative = result.get("alternatives").getAsJsonArray().get(0).getAsJsonObject();
                        String line = firstAlternative.get("transcript").getAsString();
                        if (StringUtils.isNotBlank(line)) {
                            double firstTimestamp = firstAlternative.get("timestamps").getAsJsonArray().get(0).getAsJsonArray().get(1).getAsDouble();
                            builder.append("[").append(firstTimestamp).append("s]: ").append(line).append("\n");
                        }
                    }
                }

                String concatenated = builder.toString();
                concatenated = concatenated.replace("%HESITATION ", "");

                return new ResultImpl(true, concatenated);
            } else {
                return new ResultImpl(false, null);
            }
        } catch (IOException e) {
            log.error("Unable to get result. assuming failure.", e);
            return new ResultImpl(true, "error");
        }

    }

    private static class ResultImpl implements Result {

        private final boolean completed;
        private final String content;

        public ResultImpl(boolean completed, String content) {
            this.completed = completed;
            this.content = content;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public String getContent() {
            return content;
        }
    }
}
