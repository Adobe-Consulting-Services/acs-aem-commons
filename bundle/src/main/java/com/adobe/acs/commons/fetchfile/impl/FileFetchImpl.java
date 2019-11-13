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
package com.adobe.acs.commons.fetchfile.impl;

import java.io.ByteArrayInputStream;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.xmlbeans.impl.util.Base64;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.fetchfile.FileFetch;
import com.adobe.acs.commons.fetchfile.FileFetchConfiguration;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

/**
 * Implementation of the FileFetch service
 */
@Component(service = { Runnable.class, FileFetch.class }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = FileFetchConfiguration.class, factory = true)
public class FileFetchImpl implements FileFetch, Runnable {

  private static final Logger log = LoggerFactory.getLogger(FileFetchImpl.class);

  private static final String SERVICE_USER_NAME = "file-fetch";

  protected FileFetchConfiguration config;

  @Reference
  private ResourceResolverFactory factory;

  private Exception lastException = null;

  private boolean lastJobSucceeded = true;

  @Reference
  private Replicator replicator;

  @Activate
  public void activate(FileFetchConfiguration config)  {
    log.info("Activing with configuration: {}", config);
    this.config = config;
    run();
  }

  private byte[] fetchFile() throws IOException {
    log.trace("fetchFile");

    HttpURLConnection con = null;
    try {
      log.debug("Opening connection to {}", config.remoteUrl());
      con = openConnection();

      List<Pair<String, String>> header = Arrays.stream(config.headers()).map(h -> h.split("\\="))
          .filter(h -> (h.length >= 2)).map(h -> {
            String value = Arrays.stream(ArrayUtils.remove(h, 0)).collect(Collectors.joining("="));
            return new ImmutablePair<String, String>(h[0], value);
          }).collect(Collectors.toList());
      for (Pair<String, String> p : header) {
        log.trace("Adding request property {}={}", p.getKey(), p.getValue());
        con.addRequestProperty(p.getKey(), p.getValue());
      }
      con.setUseCaches(false);
      int rc = con.getResponseCode();
      log.debug("Retrieved response code: {}", rc);
      if (ArrayUtils.contains(config.validResponseCodes(), rc)) {
        log.debug("Retrieved valid response code {}", rc);
        try (InputStream is = con.getInputStream()) {
          return IOUtils.toByteArray(is);
        }
      } else {
        log.warn("Retrieved invalid response code {}", rc);
        throw new IOException("Failed to download file: " + rc);
      }
    } finally {
      if (con != null) {
        con.disconnect();
      }
    }
  }

  @Override
  public Exception getLastException() {
    return this.lastException;
  }

  private String hash(byte[] data) {
    return new String(Base64.encode(DigestUtils.getMd5Digest().digest(data)));
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

  @Override
  public void updateFile() throws IOException, ReplicationException {
    log.trace("updateFile");

    try (ResourceResolver resolver = factory
        .getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER_NAME))) {
      AssetManager manager = Optional.ofNullable(resolver.adaptTo(AssetManager.class))
          .orElseThrow(() -> new PersistenceException("Failed to get Asset Manager"));

      byte[] bin = fetchFile();

      boolean publish = false;
      String path = config.damPath();
      Resource assetResource = resolver.getResource(path);

      String fileHash = hash(bin);
      Asset asset = null;
      if (assetResource != null) {
        asset = Optional.ofNullable(assetResource.adaptTo(Asset.class))
            .orElseThrow(() -> new PersistenceException("Failed to adapt Resource to Asset"));

        String assetHash = assetResource.getValueMap().get("jcr:content/metadata/hash", "");
        if (!fileHash.equals(assetHash)) {
          log.info("File does not match, updating...");
          asset.removeRendition("original");
          asset.addRendition("original", new ByteArrayInputStream(bin), Collections.emptyMap());
          updateHash(fileHash, assetResource);
          publish = true;
        } else {
          log.info("Hashes match, not updating");
        }
      } else {
        log.info("Adding new file...");
        asset = manager.createAsset(path, new ByteArrayInputStream(bin), config.mimeType(), true);
        assetResource = Optional.ofNullable(asset.adaptTo(Resource.class))
            .orElseThrow(() -> new PersistenceException("Failed to adapt Asset to Resource"));
        updateHash(fileHash, assetResource);
        publish = true;
      }

      if (publish) {
        log.info("Replicating...");
        replicator.replicate(resolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path);
      }

      log.debug("Update complete!");

    } catch (LoginException e) {
      log.error("Failed to get service user", e);
    }

  }

  private void updateHash(String hash, Resource assetResource) throws PersistenceException {
    ModifiableValueMap properties = Optional.ofNullable(assetResource.getChild("jcr:content/metadata"))
        .map(r -> r.adaptTo(ModifiableValueMap.class))
        .orElseThrow(() -> new PersistenceException("Failed to open resource for modification"));
    properties.put("hash", hash);
    assetResource.getResourceResolver().commit();
  }

}
