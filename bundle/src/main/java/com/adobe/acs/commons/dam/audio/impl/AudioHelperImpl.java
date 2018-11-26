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
package com.adobe.acs.commons.dam.audio.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import com.day.cq.dam.handler.ffmpeg.FfmpegNotFoundException;

@Component(service = AudioHelper.class)
@Designate(ocd=AudioHelperImpl.Config.class)
public class AudioHelperImpl implements AudioHelper {

   private static final Logger log = LoggerFactory.getLogger(AudioHelperImpl.class);

   @ObjectClassDefinition(name = "ACS Commons - Audio Processor", description = "ACS Commons - Audio Processor")
   public @interface Config {
      static String DEFAULT_DIR = "./logs/ffmpeg";

      @AttributeDefinition(defaultValue = { DEFAULT_DIR })
      String ffmpeg_workingdir();
   }

   @Reference(policy = ReferencePolicy.STATIC)
   private ExecutableLocator locator;

   private File workingDir;

   @Activate
   protected final void activate(ComponentContext ctx, AudioHelperImpl.Config config) {
      String slingHome = ctx.getBundleContext().getProperty("sling.home");
      workingDir = FFMpegAudioUtils.resolveWorkingDir(slingHome, config.ffmpeg_workingdir());
      if (!workingDir.exists() && !workingDir.mkdirs()) {
         throw new IllegalStateException("Could not create " + workingDir.getPath());
      }
   }

   @Override
   @SuppressWarnings("squid:S2095")
   public <A, R> R process(Asset asset, ResourceResolver resourceResolver, A args, AudioProcessor<A, R> audioProcessor)
         throws AudioException {
      File tmpDir = null;
      File tmpWorkingDir = null;

      try {
         // creating temp directory
         tmpDir = FFMpegAudioUtils.createTempDir(null);

         // creating temp working directory for ffmpeg
         tmpWorkingDir = FFMpegAudioUtils.createTempDir(workingDir);
      } catch (IOException e) {
         throw new AudioException(e);
      }

      // streaming file to temp directory
      final File tmpFile = new File(tmpDir, asset.getName().replace(' ', '_'));

      try (FileOutputStream fos = new FileOutputStream(tmpFile); InputStream is = asset.getOriginal().getStream();) {

         IOUtils.copy(is, fos);

         return audioProcessor.processAudio(asset, resourceResolver, tmpFile, locator, tmpWorkingDir, args);

      } catch (IOException e) {
         throw new AudioException(e);
      } catch (FfmpegNotFoundException e) {
         log.error(e.getMessage(), e);
         return null;
      } finally {
         try {
            // cleaning up temp directory
            if (tmpDir != null) {
               FileUtils.deleteDirectory(tmpDir);
            }
         } catch (IOException e) {
            log.warn("Could not delete temp directory: {}", tmpDir.getPath());
         }
         try {
            // cleaning up ffmpeg's temp working directory
            if (tmpWorkingDir != null) {
               FileUtils.deleteDirectory(tmpWorkingDir);
            }
         } catch (IOException e) {
            log.warn("Could not delete ffmpeg's temporary working directory: {}", tmpWorkingDir.getPath());
         }
      }
   }
}
