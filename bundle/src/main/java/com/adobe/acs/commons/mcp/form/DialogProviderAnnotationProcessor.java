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
package com.adobe.acs.commons.mcp.form;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.models.annotations.Model;

/**
 * Processes the DialogProvider annotation, producing a corresponding OSGi
 * service to provide the generated sling dialog resources. This annotation
 * processor will skip any classes which do not identify their corresponding
 * sling model either as part of the model annotation or by a property or getter
 * method.
 */
public class DialogProviderAnnotationProcessor extends AbstractProcessor {

    private static final Logger LOG = Logger.getLogger(DialogProviderAnnotationProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(DialogProvider.class)) {
            try {
                processDialogProviderAnnotation(annotatedElement);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(DialogProvider.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void processDialogProviderAnnotation(Element element) throws IOException {
        TypeElement t = (TypeElement) element;
        String className = t.getQualifiedName().toString();
        String serviceClassName = DialogResourceProvider.getServiceClassName(className);
        if (providesResourceType(t)) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, String.format("Generated resource provider service for class %s => %s", className, serviceClassName));
            }
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(serviceClassName);
            writeServiceStub(builderFile, serviceClassName, className);
        } else {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, String.format("Class %s declares or inherits the DialogProvider annotation but does not declare a resource type -- no resource provider generated.", className));
            }
        }
    }

    private void writeServiceStub(JavaFileObject builderFile, String serviceClass, String targetClass) throws IOException {
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            String packageName = StringUtils.substringBeforeLast(serviceClass, ".");
            String className = StringUtils.substringAfterLast(serviceClass, ".");
            String osgiService = DialogResourceProvider.class.getCanonicalName();
            out.println(String.format("package %s;", packageName));
            out.println();
            out.println("import javax.annotation.Generated;");
            out.println("import org.osgi.annotation.versioning.ConsumerType;");
            out.println("import org.osgi.framework.BundleContext;");
            out.println("import org.osgi.service.component.annotations.*;");
            out.println();
            out.println("@Generated(\"Created by the ACS Commons DialogProviderAnnotationProcessor\")");
            out.println("@ConsumerType");
            out.println(String.format("@Component(service = %s.class, immediate = true)", osgiService));
            out.println(String.format("public class %s implements %s {", className, osgiService));
            out.println();
            out.println(String.format("    @Override%n    public Class getTargetClass() {%n        return %s.class;%n    }", targetClass));
            out.println("    @Activate\n    public void activate(BundleContext context) throws InstantiationException, IllegalAccessException, ReflectiveOperationException {\n        this.doActivate(context);\n    }\n");
            out.println("    @Deactivate\n    public void deactivate(BundleContext context) {\n        this.doDeactivate();\n    }");
            out.println("}");
            out.flush();
        }
    }

    private boolean providesResourceType(TypeElement t) {
        Model model = t.getAnnotation(Model.class);
        if (model != null && model.resourceType() != null && model.resourceType().length > 0) {
            return true;
        } else {
            return t.getEnclosedElements().stream().anyMatch(this::elementProvidesResourceType);
        }
    }

    private boolean elementProvidesResourceType(Element t) {
        switch (t.getKind()) {
            case LOCAL_VARIABLE:
            case FIELD:
                return t.getSimpleName().contentEquals("resourceType");
            case METHOD:
                return t.getSimpleName().contentEquals("getResourceType");
            default:
                return false;
        }
    }
}
