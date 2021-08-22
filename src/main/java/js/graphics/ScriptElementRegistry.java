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

import java.util.Map;

import js.geometry.IRect;
import js.json.JSMap;

public final class ScriptElementRegistry {

  public static ScriptElementRegistry sharedInstance() {
    return sSharedInstance;
  }

  public void register(ScriptElement instance) {
    mMap.put(instance.tag(), instance);
  }

  public ScriptElement elementForTag(String tag) {
    ScriptElement elem = mMap.get(tag);
    if (elem == null) {
      if (alert("ignoring the fact that there is no element for the tag:", tag))
        return null;
      throw badArg("No ScriptElement found for tag:", quote(tag));
    }
    return elem;
  }

  private static final ScriptElementRegistry sSharedInstance;

  static {
    ScriptElementRegistry s = new ScriptElementRegistry();
    s.register(PointElement.DEFAULT_INSTANCE);
    s.register(RectElement.DEFAULT_INSTANCE);
    s.register(MaskElement.DEFAULT_INSTANCE);
    s.register(PolygonElement.DEFAULT_INSTANCE);
    s.register(TextElement.DEFAULT_INSTANCE);
    sSharedInstance = s;
  }

  private Map<String, ScriptElement> mMap = concurrentHashMap();

  private static final String DEFAULT_TAG = RectElement.DEFAULT_INSTANCE.tag();

  /**
   * An object that implements ScriptElement to serve as a parser that produces
   * concrete instances
   */
  public static final ScriptElement PARSER = new AbstractScriptElement(null) {

    @Override
    public IRect bounds() {
      throw notSupported();
    }

    public ScriptElement parse(Object object) {
      JSMap m = (JSMap) object;
      String tag = m.opt(ScriptUtil.TAG_KEY, DEFAULT_TAG);
      ScriptElement parser = sharedInstance().elementForTag(tag);
      if (parser == null)
        return null;
      return (ScriptElement) parser.parse(m);
    }
  };

}
