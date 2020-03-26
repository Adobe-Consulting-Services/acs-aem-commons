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
(function ($, window, undefined) {
    $(document).on("foundation-contentloaded.publishWizard", function(e) {

        var configOptions = $(".publish-wizard-config").data("config");

        if(configOptions && configOptions.texts && configOptions.texts.types) {

            //add the sharedProperties type to the list of reference types
            configOptions.texts.types.sharedProperties = "Shared Component Properties/Root Page";

            $(".publish-wizard-config").data("config", JSON.stringify(configOptions));
        }

    });

}(jQuery, this));
