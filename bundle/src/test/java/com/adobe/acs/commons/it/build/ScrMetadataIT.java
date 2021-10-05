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
package com.adobe.acs.commons.it.build;

import com.adobe.acs.commons.http.JsonObjectResponseHandler;
import com.google.gson.JsonObject;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.osgi.framework.Constants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The purpose of this test is to validate that SCR and Metatype properties are not inadvertantly changed between ACS AEM Commons releases.
 * It does this by downloading the bundle from the latest release and comparing the SCR and Metatype XML files to the ones generated
 * by the current build.
 *
 * In exceptional cases, there are use cases where changes are appropriate. These can be controlled by three sets defined in this class:
 *
 * <dl>
 *   <dt>PROPERTIES_TO_IGNORE</dt>
 *   <dd>These are properties which should be ignored on every component. These should be relatively rare.</dd>
 *   <dt>COMPONENT_PROPERTIES_TO_IGNORE</dt>
 *   <dd>These are properties to ignore on a specific component. The syntax for these values is the form PID:PROPERTY_NAME</dd>
 *   <dt>COMPONENT_PROPERTIES_TO_IGNORE_FOR_TYPE_CHANGE</dt>
 *   <dd>These are properties to ignore specifically for type changes, but will still produce a test failure when the property value changes. Syntax is the same as COMPONENT_PROPERTIES_TO_IGNORE</dd>
 * </dl>
 *
 * In addition, this test validates that all factory components have an OSGi Web Console name hint and all variables
 * referenced from the name hint exist. Currently there is no affordance for ignoring components or variables for this
 * aspect of the test.
 *
 */
@SuppressWarnings("PMD.SystemPrintln")
public class ScrMetadataIT {

    private static final Set<String> PROPERTIES_TO_IGNORE;

    private static final Set<String> COMPONENT_PROPERTIES_TO_IGNORE;

    private static final Set<String> COMPONENT_PROPERTIES_TO_IGNORE_FOR_TYPE_CHANGE;

    private static final Set<String> ALLOWED_SCR_NS_URIS;

    private static final String PROP_NAMEHINT = "webconsole.configurationFactory.nameHint";

    private static final Pattern EXTRACT_VARIABLES;

    static {
        PROPERTIES_TO_IGNORE = new HashSet<>();
        PROPERTIES_TO_IGNORE.add(Constants.SERVICE_PID);
        PROPERTIES_TO_IGNORE.add(PROP_NAMEHINT);
        //PROPERTIES_TO_IGNORE.add(Constants.SERVICE_VENDOR);

        COMPONENT_PROPERTIES_TO_IGNORE = new HashSet<>();
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.genericlists.impl.GenericListJsonResourceProvider:provider.roots");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.genericlists.impl.GenericListJsonResourceProvider:provider.ownsRoots");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.httpcache.engine.impl.HttpCacheEngineImpl:jmx.objectname");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreImpl:jmx.objectname");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.httpcache.store.mem.impl.MemHttpCacheStoreImpl:jmx.objectname");
        // the following two values changed due to https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/1773
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.rewriter.impl.PlainXMLSerializerFactory:pipeline.type");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.rewriter.impl.XMLParserGeneratorFactory:pipeline.type");

        // properties removed when updating to OSGi R6 annotations
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.reports.internal.ReportCSVExportServlet:service.vendor");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.reports.internal.ReportsRenderCondition:service.vendor");

        // properties removed for #2190 (RequireAem implementation)
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.util.impl.AemCapabilityHelperImpl:service.vendor");

        // properties removed for #2293 (Copy Properties workflow process implementation)
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.util.impl.WorkflowHelperImpl:service.vendor");

        // properties removed for #2350 (Vanity Service)
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.wcm.vanity.impl.VanityURLServiceImpl:service.vendor");

        // the following four values changed due to https://github.com/Adobe-Consulting-Services/acs-aem-commons/pull/1852
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.httpcache.invalidator.event.JCRNodeChangeEventHandler:event.topics");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.httpcache.invalidator.event.JCRNodeChangeEventHandler:event.filter");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.replication.packages.automatic.impl.ConfigurationUpdateListener:event.topics");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.replication.packages.automatic.impl.ConfigurationUpdateListener:event.filter");

        // the following four values changed due to https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/2344
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.models.injectors.impl.ChildResourceFromRequestInjector:service.ranking");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.models.injectors.impl.HierarchicalPagePropertyInjector:service.ranking");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.models.injectors.impl.I18nInjector:service.ranking");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.models.injectors.impl.JsonValueMapValueInjector:service.ranking");

        // #2303 - EnsureOakIndexServlet (exposed via the OSGi Console) should be invokable via an inline HTML form
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.oak.impl.EnsureOakIndexServlet:felix.webconsole.title");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.oak.impl.EnsureOakIndexServlet:service.vendor");

        // https://github.com/Adobe-Consulting-Services/acs-aem-commons/pull/2382
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.granite.ui.components.impl.include.IncludeDecoratorFilterImpl:sling.filter.resourceTypes");

        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.redirects.filter.RedirectFilter:storagePath");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.redirects.filter.RedirectFilter:mapUrls");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.redirects.filter.RedirectFilter:enabled");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.redirects.filter.RedirectFilter:event.topics");
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.redirects.filter.RedirectFilter:service.ranking");

        // change due to #2669 
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.mcp.impl.GenericReportExcelServlet:sling.servlet.resourceTypes");

        // https://github.com/Adobe-Consulting-Services/acs-aem-commons/pull/2667
        COMPONENT_PROPERTIES_TO_IGNORE.add("com.adobe.acs.commons.fam.impl.ThrottledTaskRunnerImpl:task.timeout");
        
        COMPONENT_PROPERTIES_TO_IGNORE_FOR_TYPE_CHANGE = new HashSet<>();
        COMPONENT_PROPERTIES_TO_IGNORE_FOR_TYPE_CHANGE.add("com.adobe.acs.commons.fam.impl.ThrottledTaskRunnerImpl:max.cpu");
        COMPONENT_PROPERTIES_TO_IGNORE_FOR_TYPE_CHANGE.add("com.adobe.acs.commons.fam.impl.ThrottledTaskRunnerImpl:max.heap");

        ALLOWED_SCR_NS_URIS = new HashSet<>();
        ALLOWED_SCR_NS_URIS.add("http://www.osgi.org/xmlns/scr/v1.0.0");
        ALLOWED_SCR_NS_URIS.add("http://www.osgi.org/xmlns/scr/v1.1.0");
        ALLOWED_SCR_NS_URIS.add("http://www.osgi.org/xmlns/scr/v1.2.0");
        ALLOWED_SCR_NS_URIS.add("http://www.osgi.org/xmlns/scr/v1.3.0");

        EXTRACT_VARIABLES = Pattern.compile("\\{([^}]+)}");
    }

    private JsonObjectResponseHandler responseHandler = new JsonObjectResponseHandler();

    private XMLInputFactory xmlInputFactory;

    public ScrMetadataIT() {
        this.xmlInputFactory = XMLInputFactory.newFactory();
        this.xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    }

    @Test
    public void test() throws Exception {
        List<String> problems = new ArrayList<>();
        DescriptorList current = getDescriptorsFromCurrent();
        if (current != null) {
            final DescriptorList latestRelease = getDescriptorsFromLatestRelease();
            current.stream().forEach(cd -> {
                Optional<Descriptor> fromLatest = latestRelease.stream().filter(d -> d.name.equals(cd.name)).findFirst();
                if (fromLatest.isPresent()) {
                    problems.addAll(compareDescriptors(cd, fromLatest.get()));
                } else {
                    System.out.printf("Component %s is only in current. Assuming OK.\n", cd.name);
                }

                if (cd.factory) {
                    Optional<Property> nameHint = cd.properties.stream().filter(cp -> cp.name.equals(PROP_NAMEHINT)).findFirst();
                    if (nameHint.isPresent()) {
                        Set<String> propertyNames = cd.properties.stream().map(cp -> cp.name).collect(Collectors.toSet());
                        String nameHintValue = nameHint.get().value;
                        Matcher variableMatcher = EXTRACT_VARIABLES.matcher(nameHintValue);
                        while (variableMatcher.find()) {
                            String variable = variableMatcher.group(1);
                            if (!propertyNames.contains(variable)) {
                                problems.add(String.format("Property %s referenced in nameHint in %s is not defined.", variable, cd.name));
                            }
                        }
                    } else {
                        problems.add(String.format("Component factory %s doesn't have a namehint.", cd.name));
                    }
                }
            });

            if (!problems.isEmpty()) {
                problems.forEach(System.err::println);
                TestCase.fail();
            }
        }
    }

    private List<String> compareDescriptors(Descriptor current, Descriptor latestRelease) {
        List<String> problems = new ArrayList<>();

        current.properties.stream().filter(cp -> !PROPERTIES_TO_IGNORE.contains(cp.name))
                .filter(cp -> !COMPONENT_PROPERTIES_TO_IGNORE.contains(current.name + ":" + cp.name)).forEach(cp -> {
            Optional<Property> fromLatest = latestRelease.properties.stream().filter(p -> p.name.equals(cp.name)).findFirst();
            if (fromLatest.isPresent()) {
                Property lp = fromLatest.get();
                if (!StringUtils.equals(cp.value, lp.value)) {
                    problems.add(String.format("Property %s on component %s has different values (was: {%s}, is: {%s})", cp.name, current.name, lp.value, cp.value));
                }
                if (!COMPONENT_PROPERTIES_TO_IGNORE_FOR_TYPE_CHANGE.contains(current.name + ":" + cp.name) && !StringUtils.equals(cp.type, lp.type)) {
                    problems.add(String.format("Property %s on component %s has different types (was: {%s}, is: {%s})", cp.name, current.name, lp.type, cp.type));
                }
            } else {
                System.out.printf("Property %s on component %s is only in current. Assuming OK.\n", cp.name, current.name);
            }
        });

        latestRelease.properties.stream().filter(lp -> !PROPERTIES_TO_IGNORE.contains(lp.name))
                .filter(lp -> !COMPONENT_PROPERTIES_TO_IGNORE.contains(latestRelease.name + ":" + lp.name)).forEach(lp -> {
            Optional<Property> fromCurrent = current.properties.stream().filter(p -> p.name.equals(lp.name)).findFirst();
            if (!fromCurrent.isPresent()) {
                problems.add(String.format("Property %s on component %s has been removed.", lp.name, latestRelease.name));
            }
        });

        return problems;
    }

    private DescriptorList getDescriptorsFromLatestRelease() throws Exception {
        JsonObject packageDetails = (JsonObject) Request.Get("https://search.maven.org/solrsearch/select?q=g:%22com.adobe.acs%22+AND+a:%22acs-aem-commons-bundle%22&rows=1").execute().handleResponse(responseHandler);
        String latestVersion = packageDetails.getAsJsonObject("response").getAsJsonArray("docs").get(0).getAsJsonObject().get("latestVersion").getAsString();

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File cachedFile = new File(tempDir, String.format("acs-aem-commons-bundle-%s.jar", latestVersion));
        if (cachedFile.exists()) {
            System.out.printf("Using cached file %s\n", cachedFile);
        } else {

            String url = String.format("https://search.maven.org/remotecontent?filepath=com/adobe/acs/acs-aem-commons-bundle/%s/acs-aem-commons-bundle-%s.jar", latestVersion, latestVersion);
            System.out.printf("Fetching %s\n", url);

            InputStream content = Request.Get(url).execute().returnContent().asStream();

            IOUtils.copy(content, new FileOutputStream(cachedFile));
        }
        return parseJar(new FileInputStream(cachedFile), false);
    }


    private DescriptorList getDescriptorsFromCurrent() throws Exception {
        String artifactPath = System.getProperty("artifactPath");
        if (artifactPath == null) {
            System.err.println("Artifact Path not set, presumably because this test is run from an IDE. Not checking JAR contents.");//NOPMD
            return null;
        }

        return parseJar(new FileInputStream(artifactPath), true);
    }

    private DescriptorList parseJar(InputStream is, boolean checkNs) throws Exception {
        DescriptorList result = new DescriptorList();

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {
                    if (entry.getName().startsWith("OSGI-INF/metatype")) {
                        result.merge(parseMetatype(new InputStreamFacade(zis), entry.getName()));
                    } else if (entry.getName().startsWith("OSGI-INF/")) {
                        result.merge(parseScr(new InputStreamFacade(zis), entry.getName(), checkNs));
                    }
                }

                entry = zis.getNextEntry();
            }
        }


        return result;
    }

    private Descriptor parseScr(InputStream is, String name, boolean checkNs) throws Exception {
        Descriptor result = new Descriptor();

        XMLEventReader reader = xmlInputFactory.createXMLEventReader(is);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                String elementName = start.getName().getLocalPart();
                if (elementName.equals("component")) {
                    result.name = start.getAttributeByName(new QName("name")).getValue();
                    if (checkNs) {
                        String scrUri = start.getName().getNamespaceURI();
                        if (!ALLOWED_SCR_NS_URIS.contains(scrUri)) {
                            throw new Exception(String.format("Banned Namespace URI %s found for %s", scrUri, name));
                        }
                    }
                } else if (elementName.equals("property")) {
                    String propName = start.getAttributeByName(new QName("name")).getValue();
                    Attribute value = start.getAttributeByName(new QName("value"));
                    Attribute typeAttr = start.getAttributeByName(new QName("type"));
                    String type = typeAttr == null ? "String" : typeAttr.getValue();
                    if (value != null) {
                        result.properties.add(new Property(propName, value.getValue(), type));
                    } else {
                        result.properties.add(new Property(propName, cleanText(reader.getElementText()), type));
                    }
                }
            }
        }

        return result;
    }

    private Descriptor parseMetatype(InputStream is, String name) throws Exception {
        Descriptor result = new Descriptor();

        XMLEventReader reader = xmlInputFactory.createXMLEventReader(is);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement start = event.asStartElement();
                String elementName = start.getName().getLocalPart();
                if (elementName.equals("Designate")) {
                    Attribute pidAttribute = start.getAttributeByName(new QName("pid"));
                    if (pidAttribute != null) {
                        result.name = pidAttribute.getValue();
                    } else {
                        pidAttribute = start.getAttributeByName(new QName("factoryPid"));
                        if (pidAttribute != null) {
                            result.name = pidAttribute.getValue();
                        }
                        result.factory = true;
                    }
                } else if (elementName.equals("AD")) {
                    String propName = start.getAttributeByName(new QName("id")).getValue();
                    Attribute value = start.getAttributeByName(new QName("default"));
                    Attribute typeAttr = start.getAttributeByName(new QName("type"));
                    String type = typeAttr == null ? "String" : typeAttr.getValue();
                    if (value == null) {
                        result.properties.add(new Property(propName, "", type));
                    } else {
                        result.properties.add(new Property(propName, value.getValue(), type));
                    }
                }
            }
        }

        if (result.name == null) {
            throw new Exception("Could not identify pid for " + name);
        }
        return result;
    }

    private String cleanText(String input) {
        if (input == null) {
            return null;
        }

        String[] parts = StringUtils.split(input, '\n');
        return Arrays.stream(parts).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"));
    }

    private class DescriptorList {

        private List<Descriptor> list = new ArrayList<>();

        private void merge(Descriptor toAdd) {
            Optional<Descriptor> current = list.stream().filter(cd -> cd.name.equals(toAdd.name)).findFirst();
            if (current.isPresent()) {
                Descriptor cd = current.get();
                toAdd.properties.forEach(ap -> {
                    Optional<Property> currentProperty = cd.properties.stream().filter(cp -> cp.name.equals(ap.name)).findFirst();
                    if (currentProperty.isPresent()) {
                        currentProperty.get().value = ap.value;
                    } else {
                        cd.properties.add(ap);
                    }
                });
                cd.factory = toAdd.factory || cd.factory;
            } else {
                list.add(toAdd);
            }
        }

        public Stream<Descriptor> stream() {
            return list.stream();
        }
    }

    private class Descriptor {
        private String name;
        private List<Property> properties = new ArrayList<>();
        private boolean factory;

        @Override
        public String toString() {
            return name;
        }
    }

    private class Property {
        private final String name;
        private String value;
        private final String type;

        Property(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

    }

    private class InputStreamFacade extends InputStream {
        private ZipInputStream zis;

        InputStreamFacade(ZipInputStream zis) {
            this.zis = zis;
        }

        @Override
        public int read() throws IOException {
            return zis.read();
        }

        @Override
        public int read(@NotNull byte[] b) throws IOException {
            return zis.read(b);
        }

        @Override
        public int read(@NotNull byte[] b, int off, int len) throws IOException {
            return zis.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return zis.skip(n);
        }

        @Override
        public int available() throws IOException {
            return zis.available();
        }

        @Override
        public void close() throws IOException {
            zis.closeEntry();
        }

        @Override
        public synchronized void mark(int readlimit) {
            zis.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            zis.reset();
        }

        @Override
        public boolean markSupported() {
            return zis.markSupported();
        }
    }
}