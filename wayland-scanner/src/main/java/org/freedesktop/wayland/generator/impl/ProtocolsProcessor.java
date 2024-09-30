//Copyright 2015 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.freedesktop.wayland.generator.impl;

import org.freedesktop.wayland.generator.api.Protocol;
import org.freedesktop.wayland.generator.api.Protocols;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("org.freedesktop.wayland.generator.api.Protocols")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ProtocolsProcessor extends AbstractProcessor {
    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {
        for (final Element elem : roundEnv.getElementsAnnotatedWith(Protocols.class)) {
            final Protocols protocols = elem.getAnnotation(Protocols.class);

            final PackageElement packageElement = getPackage(elem);
            for (final Protocol protocol : protocols.value()) {
                try {
                    new ProtocolGenerator().scan(this.processingEnv.getMessager(),
                            packageElement,
                            this.processingEnv.getFiler(),
                            protocol);
                } catch (final Exception e) {
                    this.processingEnv.getMessager()
                            .printMessage(Diagnostic.Kind.ERROR,
                                    "Got an error while trying to process protocolXML: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    private PackageElement getPackage(Element element) {
        while (element.getKind() != ElementKind.PACKAGE) {
            element = element.getEnclosingElement();
        }
        return (PackageElement) element;
    }
}
