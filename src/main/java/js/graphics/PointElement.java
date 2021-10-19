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

import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.gen.ElementProperties;
import js.json.JSMap;

/**
 * A ScriptElement representing a geometric point
 */
public class PointElement extends AbstractScriptElement {

  public static final PointElement DEFAULT_INSTANCE = new PointElement(null, IPoint.DEFAULT_INSTANCE);

  public static final String TAG = "pt";

  @Override
  public String tag() {
    return TAG;
  }

  @Override
  public IPoint location() {
    return mLocation;
  }

  @Override
  public IRect bounds() {
    return new IRect(mLocation.x, mLocation.y, 1, 1);
  }

  @Override
  public JSMap toJson() {
    return toJsonAux(null).put("loc", mLocation.toJson());
  }

  @Override
  public PointElement parse(Object object) {
    JSMap m = (JSMap) object;
    IPoint location = IPoint.DEFAULT_INSTANCE.parse(m.getList("loc"));
    return new PointElement(parsePropertiesFromElement(m), location);
  }

  public PointElement(ElementProperties properties, IPoint location) {
    super(properties);
    mLocation = location;
  }

  @Override
  public PointElement withProperties(ElementProperties properties) {
    return new PointElement(properties, mLocation);
  }

  private final IPoint mLocation;

}
