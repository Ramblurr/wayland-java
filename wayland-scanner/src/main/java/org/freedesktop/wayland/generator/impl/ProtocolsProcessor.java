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

import org.freedesktop.wayland.generator.api.WaylandCustomProtocol;
import org.freedesktop.wayland.generator.api.WaylandCustomProtocols;
import org.freedesktop.wayland.generator.api.WaylandProtocols;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

@SupportedAnnotationTypes(
        {
                "org.freedesktop.wayland.generator.api.WaylandCoreProtocols",
                "org.freedesktop.wayland.generator.api.WaylandCustomProtocols",
                "org.freedesktop.wayland.generator.api.WaylandProtocols"
        }
)
@SupportedSourceVersion(SourceVersion.RELEASE_22)
public class ProtocolsProcessor extends AbstractProcessor {

    public static Set<ProtocolGenConfig> gatherProtocols(ProcessingEnvironment penv, final RoundEnvironment roundEnv) {
        Set<ProtocolGenConfig> ret = new LinkedHashSet<>();

        // gather @WaylandProtocols
        for (final Element elem : roundEnv.getElementsAnnotatedWith(WaylandProtocols.class)) {
            final WaylandProtocols protocol = elem.getAnnotation(WaylandProtocols.class);
            var paths = ProtocolXmlPathResolver.resolvePaths(protocol);
            if (paths.isEmpty()) {
                penv.getMessager().printError("wayland-scanner could not resolve any protocol xml files", elem);
                continue;
            }
            for (var path : paths) {
                ret.add(new ProtocolGenConfig(protocol,
                        elem, getPackage(elem),
                        path
                ));
            }
        }

        // gather @WaylandCustomProtocols
        for (final Element elem : roundEnv.getElementsAnnotatedWith(WaylandCustomProtocols.class)) {
            final WaylandCustomProtocols protocols = elem.getAnnotation(WaylandCustomProtocols.class);
            for (final WaylandCustomProtocol protocol : protocols.value()) {
                ret.add(new ProtocolGenConfig(
                        protocol,
                        elem, getPackage(elem),
                        ProtocolXmlPathResolver.resolvePath(protocol)
                ));
            }
        }

        return ret;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> nope, final RoundEnvironment roundEnv) {
        Set<ProtocolGenConfig> protocolGenConfigs = gatherProtocols(this.processingEnv, roundEnv);
        for (var config : protocolGenConfigs) {
            this.processingEnv.getMessager().printNote("processing wayland protocol file=" + config.protocolFile());
            try {
                new ProtocolGenerator().scan(this.processingEnv.getMessager(),
                        config.packageElement(),
                        this.processingEnv.getFiler(),
                        config
                );
            } catch (final Exception e) {
                this.processingEnv
                        .getMessager()
                        .printError(e.getMessage());
                e.printStackTrace();
                this.processingEnv.getMessager()
                        .printError(String.format(
                                        "wayland-scanner got an error while trying to generate java bindings from a protocol xml. context was '%s'", config),
                                config.element()
                        );
            }
        }
        for (final Element elem : roundEnv.getElementsAnnotatedWith(WaylandCustomProtocols.class)) {
            final WaylandCustomProtocols protocols = elem.getAnnotation(WaylandCustomProtocols.class);

            final PackageElement packageElement = getPackage(elem);
            for (final WaylandCustomProtocol protocol : protocols.value()) {
            }
        }
        return true;
    }

    private static PackageElement getPackage(Element element) {
        while (element.getKind() != ElementKind.PACKAGE) {
            element = element.getEnclosingElement();
        }
        return (PackageElement) element;
    }

    public static void processCustomProtocols() {

    }
}
