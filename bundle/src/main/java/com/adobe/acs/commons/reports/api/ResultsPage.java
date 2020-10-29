/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple POJO for representing a page of results.
 */
public final class ResultsPage {

  private final Stream<? extends Object> results;
  private final int pageSize;
  private final int page;
  private final long resultSize;

  public ResultsPage(Stream<? extends Object> results, int pageSize, int page, long resultSize) {
    this.results = results;
    this.resultSize = resultSize;
    this.pageSize = pageSize;
    this.page = page;
  }

  public Stream<Object> getResults() {
    return (Stream<Object>) results;
  }

  public List<Object> getResultsList() {
    return results.collect(Collectors.toList());
  }

  public long getResultsStart() {
    return page != -1 ? (pageSize * page) + 1 : 1;
  }

  public long getResultsEnd() {
    return page != -1 ? (pageSize * page) + resultSize : resultSize;
  }

  public int getNextPage() {
    return (resultSize == pageSize && page != -1) ? page + 1 : -1;
  }

  public int getPreviousPage() {
    return page > 0 ? page - 1 : -1;
  }

  public long getResultSize() {
    return resultSize;
  }
}
