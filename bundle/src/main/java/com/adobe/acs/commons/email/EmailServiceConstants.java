/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.email;

import aQute.bnd.annotation.ProviderType;

/**
 * Defines special keys for the replacement variable map
 * passed to EmailService.sendEmail().
 */
@ProviderType
public final class EmailServiceConstants {


    private EmailServiceConstants() {

    }

    /**
     * Sender Email Address variable passed in the input parameter
     * map to the sendEmail() function.
     */
    public static final String SENDER_EMAIL_ADDRESS = "senderEmailAddress";

    /**
     * Sender Name variable passed in the input parameter
     * map to the sendEmail() function.
     */
    public static final String SENDER_NAME = "senderName";


    /**
     * Subject line variable used to specify the subject in the input parameter map.
     */
    public static final String SUBJECT = "subject";

}
