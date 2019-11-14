/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.filefetch.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.filefetch.FileFetchConfiguration;
import com.adobe.acs.commons.filefetch.FileFetcher;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.dam.api.Rendition;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

/**
 * Implementation of the FileFetcher service
 */
@Component(service = { Runnable.class,
    FileFetcher.class }, configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        "webconsole.configurationFactory.nameHint=File Fetcher for: {remoteUrl}, saving to: {damPath}" })
@Designate(ocd = FileFetchConfiguration.class, factory = true)
public class FileFetcherImpl implements FileFetcher, Runnable {

  private static final Logger log = LoggerFactory.getLogger(FileFetcherImpl.class);

  private static final String SERVICE_USER_NAME = "file-fetch";

  protected FileFetchConfiguration config;

  @Reference
  private ResourceResolverFactory factory;

  private Exception lastException = null;

  private boolean lastJobSucceeded = true;

  private String lastModified = null;

  @Reference
  private Replicator replicator;

  @Activate
  public void activate(FileFetchConfiguration config) {
    log.info("Activing with configuration: {}", config);
    this.config = config;
    run();
  }

  public FileFetchConfiguration getConfig() {
    return config;
  }

  @Override
  public Exception getLastException() {
    return this.lastException;
  }

  @Override
  public String getLastModified() {
    return lastModified;
  }

  @Override
  public boolean isLastJobSucceeded() {
    return this.lastJobSucceeded;
  }

  protected HttpURLConnection openConnection() throws IOException {
    return (HttpURLConnection) new URL(config.remoteUrl()).openConnection();
  }

  @Override
  public void run() {
    try {
      updateFile();
      lastJobSucceeded = true;
      lastException = null;
    } catch (Exception e) {
      log.warn("Failed to run fetch file job for {}", config.remoteUrl(), e);
      lastException = e;
      lastJobSucceeded = false;
    }
  }

  public void setFactory(ResourceResolverFactory factory) {
    this.factory = factory;
  }

  public void setReplicator(Replicator replicator) {
    this.replicator = replicator;
  }

  private HttpURLConnection setupConnection() throws IOException {
    log.trace("fetchFile");

    log.debug("Opening connection to {}", config.remoteUrl());
    HttpURLConnection con = openConnection();
    con.setConnectTimeout(config.timeout());

    List<Pair<String, String>> header = Arrays.stream(config.headers()).map(h -> h.split("\\="))
        .filter(h -> (h.length >= 2)).map(h -> {
          String value = Arrays.stream(ArrayUtils.remove(h, 0)).collect(Collectors.joining("="));
          return new ImmutablePair<String, String>(h[0], value);
        }).collect(Collectors.toList());
    for (Pair<String, String> p : header) {
      log.trace("Adding request property {}={}", p.getKey(), p.getValue());
      con.addRequestProperty(p.getKey(), p.getValue());
    }
    if (StringUtils.isNotEmpty(lastModified)) {
      con.addRequestProperty("If-Modified-Since", lastModified);
    }
    con.setUseCaches(false);
    return con;
  }

  @Override
  public void updateFile() throws IOException, ReplicationException {
    log.trace("updateFile");

    try (ResourceResolver resolver = factory
        .getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER_NAME))) {
      AssetManager manager = Optional.ofNullable(resolver.adaptTo(AssetManager.class))
          .orElseThrow(() -> new PersistenceException("Failed to get Asset Manager"));

      HttpURLConnection con = null;

      try {
        con = setupConnection();

        int responseCode = con.getResponseCode();
        if (responseCode == 304) {
          log.debug("Received Not Modified status code, no further action required");
        } else if (Arrays.stream(config.validResponseCodes()).anyMatch(rc -> rc == responseCode)) {
          log.debug("Received valid status code: {}", responseCode);
          String path = config.damPath();
          Resource assetResource = resolver.getResource(path);

          Asset asset = null;
          try (InputStream is = con.getInputStream()) {
            if (assetResource != null) {
              asset = Optional.ofNullable(assetResource.adaptTo(Asset.class))
                  .orElseThrow(() -> new PersistenceException("Failed to adapt Resource to Asset"));
              log.debug("Updating asset: {}", path);
              for (Rendition r : asset.getRenditions()) {
                asset.removeRendition(r.getName());
              }
              asset.removeRendition("original");
              asset.addRendition("original", is, config.mimeType());
            } else {
              log.debug("Adding new asset: {}", path);
              asset = manager.createAsset(path, is, config.mimeType(), true);
              assetResource = Optional.ofNullable(asset.adaptTo(Resource.class))
                  .orElseThrow(() -> new PersistenceException("Failed to adapt Asset to Resource"));
            }
          }

          lastModified = con.getHeaderField("Last-Modified");

          log.info("Replicating fetched file {}", path);
          replicator.replicate(resolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path);
        } else {
          log.warn("Received invalid status code: {}", responseCode);
          throw new IOException("Received invalid status code: " + responseCode);
        }
      } finally {
        if (con != null) {
          con.disconnect();
        }
      }

      log.debug("Update complete!");

    } catch (LoginException e) {
      log.error("Failed to get service user", e);
    }

  }

}
