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

import js.geometry.IRect;
import js.geometry.Polygon;
import js.graphics.gen.ElementProperties;
import js.json.JSMap;

/**
 * A ScriptElement representing a polygon. A thin wrapper around a Polygon
 * object
 */
public final class PolygonElement extends AbstractScriptElement {

  public static final PolygonElement DEFAULT_INSTANCE = new PolygonElement(null, Polygon.DEFAULT_INSTANCE);
  public static final String TAG = "polygon";

  @Override
  public String tag() {
    return TAG;
  }

  @Override
  public IRect bounds() {
    return polygon().bounds();
  }

  @Override
  public JSMap toJson() {
    loadTools();
    JSMap m = toJsonAux(polygon().toJson());
    return m;
  }

  public PolygonElement(ElementProperties properties, Polygon polygon) {
    super(properties);
    mPolygon = polygon;
  }

  @Override
  public PolygonElement withProperties(ElementProperties properties) {
    return new PolygonElement(properties, mPolygon);
  }

  @Override
  public PolygonElement parse(Object object) {
    JSMap m = (JSMap) object;

    Polygon poly = Polygon.DEFAULT_INSTANCE.parse(m);
    ElementProperties properties = parsePropertiesFromElement(m);
    return new PolygonElement(properties, poly);
  }

  public Polygon polygon() {
    return mPolygon;
  }

  public PolygonElement withPolygon(Polygon polygon) {
    return new PolygonElement(properties(), polygon);
  }

  private final Polygon mPolygon;

}
