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

import org.freedesktop.wayland.generator.api.Protocol;
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
import java.io.*;
import java.nio.file.Path;

public class ProtocolGenerator {
    public void scan(final Messager messager,
                     final PackageElement packageElement,
                     final Filer filer,
                     final Protocol protocol) throws IOException, SAXException, ParserConfigurationException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (protocol.dtd()) {
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
        File protocolFile = resolveProtocolPath(protocol);
        final Document doc = xmlBuilder.parse(protocolFile);
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
                protocol,
                protocolElement);
    }

    private File resolveProtocolPath(Protocol protocol) {
        System.out.println(String.format("resolving protocol annotation %s", protocol));
        var path = new File(protocol.path());
        if (path.exists() && path.canRead()) {
            return path;
        }

        if (protocol.pkgConfig() != "") {
            path = resolveFromPkgConfig(protocol);
            if (path != null && path.exists() && path.canRead()) {
                return path;
            }
        }

        throw new RuntimeException(String.format("Cannot locate protocol xml for path=%s pkgConfig=%s, pkgConfigDataDirOutput=", protocol.path(), protocol.pkgConfig(),
                pkgConfigDataDir(protocol.pkgConfig())
        ));
    }

    private static File resolveFromPkgConfig(Protocol protocol) {
        String pkgConfigDataDir = pkgConfigDataDir(protocol.pkgConfig());
        System.out.println(String.format("pkgConfigDataDir=%s", pkgConfigDataDir));
        if (pkgConfigDataDir == null)
            return null;
        var ret = Path.of(pkgConfigDataDir, protocol.path()).toFile();
        System.out.println(
                String.format("pkgconfig reg=%s", ret));
        return ret;
    }

    private static String pkgConfigDataDir(String packageName) {
        var process = new ProcessBuilder(
                "pkg-config",
                "--variable=pkgdatadir",
                packageName
        );
        process.redirectErrorStream(true);
        try {
            Process start = process.start();
            InputStream is = start.getInputStream();
            start.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                return line;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}

