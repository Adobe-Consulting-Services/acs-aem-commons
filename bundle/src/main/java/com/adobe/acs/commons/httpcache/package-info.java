/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

/**
 * Http Cache - Caching sub-system for http requests. In nutshell, HTTP responses for the configured patterns of URIs
 * are cached on first request and served from cache for subsequent requests. This caching layer shall be availed for
 * requests for which responses are expensive to compute and can be cached.
 */
@org.osgi.annotation.versioning.Version("1.0.0")
package com.adobe.acs.commons.httpcache;

