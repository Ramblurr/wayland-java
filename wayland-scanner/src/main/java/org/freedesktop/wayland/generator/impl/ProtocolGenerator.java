/*
 * Copyright © 2015 Erik De Rijcke
 * Copyright © 2024 Casey Link
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.freedesktop.wayland.generator.impl;

import org.freedesktop.wayland.generator.api.WaylandCustomProtocol;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ProtocolGenerator {
    public void scan(final Messager messager,
                     final PackageElement packageElement,
                     final Filer filer,
                     final ProtocolGenConfig config
    )
            throws IOException, SAXException, ParserConfigurationException {

        Objects.requireNonNull(messager);
        Objects.requireNonNull(messager);
        Objects.requireNonNull(config);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (config.validateDtd()) {
            factory.setValidating(true);
        }
        final DocumentBuilder xmlBuilder = factory.newDocumentBuilder();
        xmlBuilder.setErrorHandler(
                new ErrorHandler() {
                    public void warning(final SAXParseException e) throws SAXException {
                        messager.printMessage(Diagnostic.Kind.WARNING,
                                e.getMessage());
                    }

                    public void error(final SAXParseException e) throws SAXException {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                e.getMessage());
                        throw e;
                    }

                    public void fatalError(final SAXParseException e) throws SAXException {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                                e.getMessage());
                        throw e;
                    }
                });
        final Document doc = xmlBuilder.parse(config.protocolFile());
        doc.getDocumentElement()
                .normalize();

        final DocumentTraversal docTraversal = (DocumentTraversal) doc;
        final TreeWalker treeWalker = docTraversal.createTreeWalker(doc.getDocumentElement(),
                NodeFilter.SHOW_ALL,
                null,
                false);
        final Element protocolElement = (Element) treeWalker.getRoot();
        new ProtocolWriter().write(packageElement,
                filer,
                config,
                protocolElement);
    }

}

