/*
 * #%L
 * ACS AEM Commons Content Package
 * %%
 * Copyright (C) 2014 Adobe
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
def destFile = new File(project.build.directory, "${project.artifactId}-${project.version}-min.zip");
def sourceFile = new File(project.build.directory, "${project.build.finalName}.zip");

def buffer = new byte[1024];

def input = new java.util.zip.ZipInputStream(new FileInputStream(sourceFile));
def output = new java.util.zip.ZipOutputStream(new FileOutputStream(destFile));
 
def entry = input.getNextEntry();
while (entry != null) {
    def name = entry.getName();

    def isTwitterRelated = name =~ /twitter/;

    if (!isTwitterRelated) {
        output.putNextEntry(new java.util.zip.ZipEntry(name));
        def length;
        while ((len = input.read(buffer)) > 0) {
            output.write(buffer, 0, len);
        }
    }
    entry = input.getNextEntry();
}
input.close();
output.close();