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
package com.adobe.acs.commons.dam.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
    name = "ACS AEM Commons - Color Conversion",
    description = "ACS AEM Commons - Color Conversion"
)
public @interface ColorConversionConfig {
    
    @AttributeDefinition(
        name = "CMYK ICC Profile",
        description = "ICC Profile for CMYK to RGB Conversion",
        type = AttributeType.STRING,
        options = {
            @Option(label = "CoatedFOGRA27", value = "CoatedFOGRA27"),
            @Option(label = "CoatedFOGRA39", value = "CoatedFOGRA39"),
            @Option(label = "JapanColor2001Coated", value = "JapanColor2001Coated"),
            @Option(label = "JapanColor2001Uncoated", value = "JapanColor2001Uncoated"),
            @Option(label = "JapanColor2002Newspaper", value = "JapanColor2002Newspaper"),
            @Option(label = "JapanWebCoated", value = "JapanWebCoated"),
            @Option(label = "UncoatedFOGRA29", value = "UncoatedFOGRA29"),
            @Option(label = "USSheetfedCoated", value = "USSheetfedCoated"),
            @Option(label = "USSheetfedUncoated", value = "USSheetfedUncoated"),
            @Option(label = "USWebCoatedSWOP", value = "USWebCoatedSWOP"),
            @Option(label = "USWebUncoated", value = "USWebUncoated"),
            @Option(label = "WebCoatedFOGRA28", value = "WebCoatedFOGRA28")
        }
    )
    String cmyk_icc_profile() default "JapanColor2001Coated";
}
