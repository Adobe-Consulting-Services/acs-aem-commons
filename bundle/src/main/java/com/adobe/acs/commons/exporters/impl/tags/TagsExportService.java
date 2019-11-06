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
package com.adobe.acs.commons.exporters.impl.tags;

import com.day.cq.commons.jcr.JcrConstants;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jcr.query.Query;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = TagsExportService.class)
public class TagsExportService {

  private static final Logger log = LoggerFactory.getLogger(TagsExportService.class);

  private static final String TAGS_QUERY = "SELECT * FROM [cq:Tag] AS s WHERE ISDESCENDANTNODE('%s')";

  private static final String DEFAULT_LANGUAGE = "en";

  private static final String[] TAGS_ROOTS = {"tags","cq:Tags"};

  private static final String LOCALIZED_JCR_TITLE = JcrConstants.JCR_TITLE + ".";

  private static final int ROOT_TAG_LEVEL = 3;

  private static final int ARRAY_INDEX_CORRECTION = 1;

  /**
   * Export all tags under passed root path in non-localized structure ex. 'product {{product}}'
   * @param path root path from which export will begin
   * @param rr ResourceResolver object
   * @return tags structure as single String
   */
  public String exportNonLocalizedTagsForPath(String path, ResourceResolver rr) {
    return exportTagsForPath(path, rr, DEFAULT_LANGUAGE, TagExportMode.NON_LOCALIZED);
  }

  /**
   * Export all tags under passed root path in localized structure ex. 'es[Produtos] fr[Products in french] en[Products] {{products}}'
   * If tag does not contain assigned localization as default is used 'en'
   * @param path root path from which export will begin
   * @param rr ResourceResolver object
   * @return tags structure as single String
   */
  public String exportLocalizedTagsForPath(String path, ResourceResolver rr) {
    return exportLocalizedTagsForPath(path, rr, DEFAULT_LANGUAGE);
  }

  /**
   * Export all tags under passed root path in localized structure ex. 'es[Produtos] fr[Products in french] en[Products] {{products}}'
   * @param path root path from which export will begin
   * @param rr ResourceResolver object
   * @param lang localization, which will be used as default if tag do not contain any
   * @return tags structure as single String
   */
  public String exportLocalizedTagsForPath(String path, ResourceResolver rr, String lang) {
    return exportTagsForPath(path, rr, lang, TagExportMode.LOCALIZED);
  }

  private String exportTagsForPath(String path, ResourceResolver rr, String lang, TagExportMode mode) {
    StringBuilder result = new StringBuilder();
    if (TagsExportService.containsTagRoot(path)) {
      List<Resource> tags = getAllTags(path, rr);
      int tagsDepth = tagsDepth(tags);
      if (tagsDepth != 0) {
        String[] tagArray = new String[tagsDepth];
        tags.forEach(tag -> {
          Arrays.fill(tagArray, StringUtils.EMPTY);

          if (TagExportMode.LOCALIZED.equals(mode)) {
            result.append(tagLocalizedAsCsv(tag, tagArray, lang));
          } else {
            result.append(tagNonLocalizedAsCsv(tag, tagArray));
          }
        });
      }
      log.info("Tags in number of {} has been exported for root path {}", tags.size(), path);
    } else {
      result.append(String.format("Path '%s' do not contains tag root. Probably You've made mistake during typing path. Export tags cannot be done.", path));
      log.error("Path {} does not contain tags root path. Export tags cannot be done.", path);
    }
    return result.toString();
  }

  private static boolean containsTagRoot(String path) {
    return Arrays.stream(path.split("/"))
        .anyMatch(segment -> StringUtils.equalsAnyIgnoreCase(segment, TAGS_ROOTS));
  }

  private static List<Resource> getAllTags(String path, ResourceResolver rr) {
    return Lists.newArrayList(rr.findResources(String.format(TAGS_QUERY, path), Query.JCR_SQL2));
  }

  private static int tagsDepth(List<Resource> tags) {
    return tags.stream()
        .map(Resource::getPath)
        .map(s -> s.split("/").length)
        .reduce(Integer::max)
        .map(v -> v - ROOT_TAG_LEVEL)
        .orElse(0);
  }

  private static String tagLocalizedAsCsv(Resource resource, String[] tagElements, String lang) {
    if (!StringUtils.equalsAnyIgnoreCase(resource.getName(), TAGS_ROOTS)) {
      ValueMap map = resource.getValueMap();
      int arrayIndex = calculateArrayIndex(resource);
      Set<String> keys = getTitleKeys(map);
      String name = extractName(resource);
      if (keys.isEmpty()) {
        String title = extractTitle(resource, JcrConstants.JCR_TITLE, name);
        tagElements[arrayIndex] = lang + "[" + title + "] {{" + name + "}}";
      } else {
        keys.forEach(key -> {
          String title = extractTitle(resource, key, name);
          String localLang = StringUtils.substringAfter(key, LOCALIZED_JCR_TITLE);
          tagElements[arrayIndex] += localLang + "[" + title + "] ";
        });
        tagElements[arrayIndex] += "{{" + resource.getName() + "}}";
      }
      return tagLocalizedAsCsv(resource.getParent(), tagElements, lang);
    } else {
      return createTagRow(tagElements);
    }
  }

  private static String tagNonLocalizedAsCsv(Resource resource, String[] tagElements) {
    if (!StringUtils.equalsAnyIgnoreCase(resource.getName(), TAGS_ROOTS)) {
      String name = extractName(resource);
      String title = extractTitle(resource, JcrConstants.JCR_TITLE, name);
      tagElements[calculateArrayIndex(resource)] = title + " {{" + name + "}}";

      return tagNonLocalizedAsCsv(resource.getParent(), tagElements);
    } else {
      return createTagRow(tagElements);
    }
  }

  private static Set<String> getTitleKeys(ValueMap map) {
    return map.keySet().stream()
        .filter(key -> key.startsWith(LOCALIZED_JCR_TITLE))
        .collect(Collectors.toSet());
  }

  private static int calculateArrayIndex(Resource resource) {
    return resource.getPath().split("/").length - ROOT_TAG_LEVEL - ARRAY_INDEX_CORRECTION;
  }

  private static String extractName(Resource resource) {
    return StringUtils.substringAfterLast(resource.getPath(), "/");
  }

  private static String extractTitle(Resource resource, String key, String defaultTitle) {
    return resource.getValueMap()
        .get(key, defaultTitle).replaceAll(",", StringUtils.SPACE);
  }

  private static String createTagRow(String[] tagElements) {
    return String.join(",", tagElements) + ",\n";
  }

  private enum TagExportMode {
    NON_LOCALIZED, LOCALIZED
  }
}
