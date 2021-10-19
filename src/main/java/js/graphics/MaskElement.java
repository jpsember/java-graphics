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

import js.geometry.IRect;
import js.graphics.gen.ElementProperties;
import js.json.JSMap;

public class MaskElement extends RectElement {

  public static final MaskElement DEFAULT_INSTANCE = new MaskElement(null, IRect.DEFAULT_INSTANCE);
  public static final String TAG = "mask";

  @Override
  public String tag() {
    return TAG;
  }

  @Override
  public JSMap toJson() {
    return toJsonAux(null).put("bounds", mBounds.toJson());
  }

  @Override
  public MaskElement parse(Object object) {
    JSMap m = (JSMap) object;
    IRect bounds;
    bounds = IRect.DEFAULT_INSTANCE.parse(m.getList("bounds"));
    ElementProperties properties = parsePropertiesFromElement(m);
    return new MaskElement(properties, bounds);
  }

  public MaskElement(ElementProperties properties, IRect bounds) {
    super(properties, bounds);
  }

  public MaskElement(IRect bounds) {
    this(null, bounds);
  }
}
