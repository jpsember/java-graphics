/**
 * MIT License
 * 
 * Copyright (c) 2021 Jeff Sember
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 **/
package js.graphics;

import static js.base.Tools.*;

import js.graphics.gen.ElementProperties;
import js.json.JSMap;

/**
 * An implementation of ScriptElement that includes boilerplate for properties
 */
public abstract class AbstractScriptElement implements ScriptElement {

  public AbstractScriptElement(ElementProperties properties) {
    mProperties = nullTo(properties, ElementProperties.DEFAULT_INSTANCE).build();
  }

  @Override
  public final ElementProperties properties() {
    return mProperties;
  }

  public static ElementProperties parsePropertiesFromElement(JSMap map) {
    JSMap m = map.optJSMap(ScriptUtil.TAG_PROPERTIES);
    if (m == null)
      return ElementProperties.DEFAULT_INSTANCE;
    return ElementProperties.DEFAULT_INSTANCE.parse(m);
  }

  private final ElementProperties mProperties;

}
