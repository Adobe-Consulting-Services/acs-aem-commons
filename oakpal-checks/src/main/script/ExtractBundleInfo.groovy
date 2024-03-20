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
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.JSON
import groovy.json.JsonOutput

//println properties['aemVersion']

def baseUrl = "http://${properties['crx.host']}:${properties['crx.port']}"

def http = new HTTPBuilder(baseUrl)

http.auth.basic 'admin', 'admin'


def bundles = []

http.request(GET, JSON) {
    uri.path = '/system/console/bundles.json'

    response.success = { resp, allJson ->
        allJson.data.each { bundle ->
            bundles << bundle.id
        }
    }
}

def exports = [:]

bundles.each { bundleId ->
    http.request(GET, JSON) {
        uri.path = "/system/console/bundles/${bundleId}.json"

        response.success = { response, bundleJson ->
            def bundleExports = bundleJson.data[0].props.find { prop ->
                prop.key == "Exported Packages"
            }
            if (bundleExports) {
                bundleExports.value.each { bundleExport ->
                    if (bundleExport == "-") { // no packages exported
                        return
                    }

                    def matcher = bundleExport =~ /(.*),version=(.*)/

                    if (matcher.count == 0) {
                        println "Export ${bundleExport} did not match expected format"
                        return
                    }

                    def packageName = matcher[0][1]
                    def versionName = matcher[0][2]
                    if (!exports[packageName]) {
                        exports[packageName] = new HashSet<>();
                    }
                    def bootDelegationPostfixIndex = versionName.indexOf(" -- Overwritten by Boot Delegation");
                    if (bootDelegationPostfixIndex > 0) {
                        versionName = versionName.substring(0, bootDelegationPostfixIndex);
                    }
                    exports[packageName].add(versionName)
                }
            }
        }
    }
}

def exportsOutput = JsonOutput.prettyPrint(JsonOutput.toJson(exports));

def outputFile = new File("src/main/resources/bundleinfo/${properties['aemVersion']}.json")
outputFile.getParentFile().mkdirs()

outputFile.newWriter().withWriter { w ->
    w << exportsOutput
}