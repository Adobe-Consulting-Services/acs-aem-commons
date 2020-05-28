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
 */package com.adobe.acs.commons.mcp.form;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
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
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.annotations.Component;

/**
 *
 */
public class DialogProviderAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        boolean success = false;
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(DialogProvider.class)) {
            try {
                success = success || processDialogProviderAnnotation(annotatedElement);
            } catch (IOException ex) {
                Logger.getLogger(DialogProviderAnnotationProcessor.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return success;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(DialogProvider.class.getCanonicalName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    private boolean processDialogProviderAnnotation(Element element) throws IOException {
        TypeElement t = (TypeElement) element;
        String className = t.getQualifiedName().toString();
        String serviceClassName = DialogResourceProvider.getServiceClassName(className);
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(serviceClassName);
        Logger.getLogger(DialogProviderAnnotationProcessor.class.getName())
                .log(Level.INFO, String.format("Writing dialog generator service for class %s => %s", className, serviceClassName));
        writeServiceStub(builderFile, serviceClassName, className);
        return true;
    }

    private void writeServiceStub(JavaFileObject builderFile, String serviceClass, String targetClass) throws IOException {
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            String packageName = StringUtils.substringBeforeLast(serviceClass, ".");
            String className = StringUtils.substringAfterLast(serviceClass, ".");
            String osgiService = DialogResourceProvider.class.getCanonicalName();
            out.println(String.format("package %s;", packageName));
            out.println();
            out.println(String.format("import %s;", ConsumerType.class.getCanonicalName()));
            out.println(String.format("import %s;", Component.class.getCanonicalName()));
            out.println();
            out.println("@ConsumerType");
            out.println(String.format("@Component(service = %s.class, immediate = true)", osgiService));
            out.println(String.format("public class %s implements %s {", className, osgiService));
            out.println();
            out.println("    @Override");
            out.println(String.format("    public Class getTargetClass() {%n        return %s.class;%n    }", targetClass));
            out.println("}");
            out.flush();
        }
    }
}
