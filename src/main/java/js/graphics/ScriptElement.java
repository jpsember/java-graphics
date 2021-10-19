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

import js.data.AbstractData;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.gen.ElementProperties;
import js.json.JSMap;

/**
 * An object that can appear within a Script
 */
public interface ScriptElement extends AbstractData {

  /**
   * Get string associating this element with its concrete class (e.g. to locate
   * a parser for it).
   * 
   * All instances of a particular class of ScriptElement must return the same
   * tag object, since equality is tested via == and not .equals()
   */
  default String tag() {
    throw notSupported("tag not supported for class", getClass());
  }

  /**
   * Determine if this element is of a particular type (by examining its tag)
   */
  default boolean is(ScriptElement defaultInstance) {
    return tag() == defaultInstance.tag();
  }

  /**
   * Calculate axis-aligned rectangle for editing purposes
   */
  default IRect bounds() {
    throw notSupported();
  }

  /**
   * Get representative point, e.g. for transformation purposes; default returns
   * bounds().location()
   */
  default IPoint location() {
    return bounds().location();
  }

  /**
   * Get the properties of this element
   */
  ElementProperties properties();

  /**
   * Construct a copy of this element, with new properties
   */
  default ScriptElement withProperties(ElementProperties properties) {
    throw notSupported();
  }

  /**
   * Start serializing this element to a JSMap, filling in as many fields as
   * possible, before the subclass gets it. This includes the TAG and PROPERTIES
   * fields
   */
  default JSMap toJsonAux(JSMap m) {
    if (m == null)
      m = map();
    m.put(ScriptUtil.TAG_KEY, tag());
    ElementProperties properties = properties();
    // Don't include a properties field if it's the default instance
    if (!properties.equals(ElementProperties.DEFAULT_INSTANCE))
      m.put(ScriptUtil.TAG_PROPERTIES, properties);
    return m;
  }

  // The abstract data classes will attempt to use a class variable to parse 
  // such items, so supply one that can act as a parser
  //
  static final ScriptElement DEFAULT_INSTANCE = ScriptElementRegistry.PARSER;

}
