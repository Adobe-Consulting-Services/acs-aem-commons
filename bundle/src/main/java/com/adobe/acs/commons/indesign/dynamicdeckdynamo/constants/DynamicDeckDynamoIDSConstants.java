/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants;

/**
 * This class will be used to store Indesign Server Specific Constants
 */
public class DynamicDeckDynamoIDSConstants {

    /**
     * Private Constructor will prevent the instantiation of this class directly
     */
    private DynamicDeckDynamoIDSConstants() {

    }
    
    public static final String IDS_EXTENDSCRIPT_JOB = "dam/proxy/ids/job";
    public static final String IDS_JOB_PAYLOAD = "ids.job.payload";
    public static final String IDS_JOB_SCRIPT = "ids.job.script";

    /**
     * Job parameter for created InDesign Snippet name prefix
     */
    public static final String IDS_TEMPLATE_PATH = "idTemplatePath";
    public static final String IDS_ADD_SOAP_ARGS = "idsprint.addtlSoapArgs";
    public static final String IDS_EXPORTED = "idsExported";
    public static final String IDS_ARGS_TAG_XML = "tagXmlPath";
    public static final String IDS_ARGS_IMAGE_LIST = "imageList";
    public static final String IDS_ARGS_FORMATS = "formats";
    public static final String IDS_SCRIPT_ROOT_PATH = "dam/indesign/scripts/";
    public static final String IDS_ASSET_NAME = "asset_name";
    public static final String IDS_ARGS_TYPE = "type";
    public static final String INPUT_PAYLOAD = "offloading.input.payload";
    public static final String OUTPUT_PAYLOAD = "offloading.output.payload";

}
