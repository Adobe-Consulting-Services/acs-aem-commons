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

import com.adobe.acs.commons.dam.audio.watson.TranscriptionService;
import com.adobe.acs.commons.http.HttpClientFactory;
import com.adobe.acs.commons.http.JsonObjectResponseHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.fluent.Request;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

@Component
@Service
public class TranscriptionServiceImpl implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionServiceImpl.class);

    private static final JsonObjectResponseHandler HANDLER = new JsonObjectResponseHandler();

    @Reference(target = "(factory.name=watson-speech-to-text)")
    private HttpClientFactory httpClientFactory;

    @Activate
    protected void activate(Map<String, Object> config) {
    }

    @Override
    public String startTranscriptionJob(InputStream stream, String mimeType) {
        Request request = httpClientFactory.post("/speech-to-text/api/v1/recognitions?continuous=true&timestamps=true").
                addHeader("Content-Type", mimeType).
                bodyStream(stream);

        try {
            JSONObject json = httpClientFactory.getExecutor().execute(request).handleResponse(HANDLER);

            log.trace("content: {}", json.toString(2));
            return json.getString("id");
        } catch (Exception e) {
            log.error("error submitting job", e);
            return null;
        }
    }

    @Override
    public Result getResult(String jobId) {
        log.debug("getting result for {}", jobId);
        Request request = httpClientFactory.get("/speech-to-text/api/v1/recognitions/" + jobId);
        try {
            JSONObject json = httpClientFactory.getExecutor().execute(request).handleResponse(HANDLER);

            log.trace("content: {}", json.toString(2));
            if (json.getString("status").equals("completed")) {
                JSONArray results = json.getJSONArray("results").getJSONObject(0).getJSONArray("results");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    if (result.getBoolean("final")) {
                        JSONObject firstAlternative = result.getJSONArray("alternatives").getJSONObject(0);
                        String line = firstAlternative.getString("transcript");
                        if (StringUtils.isNotBlank(line)) {
                            double firstTimestamp = firstAlternative.getJSONArray("timestamps").getJSONArray(0).getDouble(1);
                            builder.append("[").append(firstTimestamp).append("]: ").append(line).append("\n");
                        }
                    }
                }

                String concatenated = builder.toString();
                concatenated = concatenated.replace("%HESITATION ", "");

                return new ResultImpl(true, concatenated);
            } else {
                return new ResultImpl(false, null);
            }
        } catch (Exception e) {
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
