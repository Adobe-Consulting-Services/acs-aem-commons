/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockCryptoSupport {

    /**
     * create a mock instance of CryptoSupport:
     *
     * protect("admin") => admin-encrypted
     * unprotect("admin-encrypted") => admin
     * isProtected("admin-encrypted") => true
     * isProtected("admin") => false
     *
     */
    public static CryptoSupport getInstance() throws CryptoException  {
        CryptoSupport crypto = mock(CryptoSupport.class);
        doAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return input + "-encrypted";
        }).when(crypto).protect(anyString());
        doAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return input.replaceAll("-encrypted$", "");
        }).when(crypto).unprotect(anyString());
        doAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return input.endsWith("-encrypted");
        }).when(crypto).isProtected(anyString());
        return crypto;
    }
}
