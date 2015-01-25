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

import com.squareup.javawriter.JavaWriter;
import org.freedesktop.wayland.HasValue;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;

import static org.freedesktop.wayland.generator.impl.StringUtil.getDoc;
import static org.freedesktop.wayland.generator.impl.StringUtil.getJavaTypeNameEnum;

public class EnumWriter {
    public void write(final Writer writer,
                      final String sharedPackage,
                      final String copyright,
                      final Element interfaceNode,
                      final Element enumNode) throws IOException {
        final JavaWriter javaWriter = new JavaWriter(writer);
        //imports
        javaWriter.emitPackage(sharedPackage)
                  .emitSingleLineComment(copyright.replace("\n",
                                                           "\n//"));
        //class javadoc
        javaWriter.emitJavadoc(getDoc(enumNode));
        //begin type
        javaWriter.beginType(getJavaTypeNameEnum(sharedPackage,
                                                 interfaceNode,
                                                 enumNode),
                             "enum",
                             EnumSet.of(Modifier.PUBLIC),
                             null,
                             HasValue.class.getName());

        //enum values
        //javaWriter.emitEnumValue()
        javaWriter.emitEmptyLine();
        final NodeList enumEntries = enumNode.getElementsByTagName("entry");
        for (int i = 0; i < enumEntries.getLength(); i++) {
            final Element enumEntry = (Element) enumEntries.item(i);
            final String summary = enumEntry.getAttribute("summary");
            String name = enumEntry.getAttribute("name");
            final String value = enumEntry.getAttribute("value");

            if (Character.isDigit(name.charAt(0))) {
                name = "_" + name;
            }

            javaWriter.emitJavadoc(summary)
                      .emitEnumValue(name.toUpperCase() + "(" + value + ")",
                                     (i + 1) == enumEntries.getLength());
        }

        //field
        javaWriter.emitEmptyLine()
                  .emitField(int.class.getName(),
                             "value",
                             EnumSet.of(Modifier.PRIVATE,
                                        Modifier.FINAL));
        //constructor
        javaWriter.emitEmptyLine()
                  .beginConstructor(EnumSet.of(Modifier.PRIVATE),
                                    int.class.getName(),
                                    "value")
                  .emitStatement("this.value = value")
                  .endConstructor();
        //getter
        javaWriter.emitEmptyLine()
                  .beginMethod(int.class.getName(),
                               "getValue",
                               EnumSet.of(Modifier.PUBLIC))
                  .emitStatement("return this.value")
                  .endMethod();

        javaWriter.endType();
    }
}
