/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.dom.html;

import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public interface HTMLMetaElement extends HTMLElement {
    @JSProperty
    String getName();

    @JSProperty
    void setName(String name);

    @JSProperty
    String getHttpEquiv();

    @JSProperty
    void setHttpEquiv(String httpEquiv);

    @JSProperty
    String getContent();

    @JSProperty
    void setContent(String content);
}
