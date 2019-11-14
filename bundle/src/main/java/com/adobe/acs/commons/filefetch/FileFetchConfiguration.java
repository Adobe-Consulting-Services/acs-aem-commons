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
package com.adobe.acs.commons.filefetch;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * A configuration for configuring a service to fetch a file from a remote
 * source and saving it as a DAM Asset.
 */
@ObjectClassDefinition(name = "ACS AEM Commons - File Fetch", description = "Service Configuration")
public @interface FileFetchConfiguration {

  @AttributeDefinition(name = "DAM Path", description = "The path under which to save the file")
  String damPath();

  @AttributeDefinition(name = "Headers", description = "Headers to add to the request")
  String[] headers() default {};

  @AttributeDefinition(name = "Mime Type", description = "The mime type of the asset to create")
  String mimeType();

  @AttributeDefinition(name = "Remote URL", description = "The URL from which to retrieve the file")
  String remoteUrl();

  @AttributeDefinition(name = "Update Cron Expression", description = "A cron expression on when to fetch the file")
  String scheduler_expression();

  @AttributeDefinition(name = "Valid Response Codes", description = "Responses which will be considered successful")
  int[] validResponseCodes() default { 200 };

  @AttributeDefinition(name = "Connection Timeout", description = "Maximum timeout for a connection response")
  int timeout() default 5000;
}
