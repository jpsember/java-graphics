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

import java.util.List;

import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.graphics.gen.ElementProperties;
import js.json.JSMap;

public final class TextElement extends AbstractScriptElement {

  public static final TextElement DEFAULT_INSTANCE = new TextElement(null, "", IRect.DEFAULT_INSTANCE);
  public static final String TAG = "t";

  @Override
  public String tag() {
    return TAG;
  }

  @Override
  public IRect bounds() {
    return mBounds;
  }

  @Override
  public JSMap toJson() {
    loadTools();
    JSMap m = map();
    m.put("b", bounds().toJson());
    m.put("s", mText);
    toJsonAux(m);
    return m;
  }

  @Override
  public TextElement withProperties(ElementProperties properties) {
    return new TextElement(properties, mText, mBounds);
  }

  public TextElement(ElementProperties properties, String text, IRect bounds) {
    super(properties);
    mText = nullToEmpty(text);
    mBounds = checkNotNull(bounds);
  }

  public TextElement(String text, IPoint location) {
    super(ElementProperties.DEFAULT_INSTANCE);

    mText = text;
    mTextRows = split(text, '\n');
    int maxRowSize = 2;
    for (String row : mTextRows) {
      maxRowSize = Math.max(maxRowSize, row.length());
    }
    int width = Math.round(PADDING * 2 + maxRowSize * FONT_CHAR_WIDTH);
    int height = Math.round(PADDING * 2 + mTextRows.size() * FONT_HEIGHT);
    mBounds = new IRect(location.x, location.y, width, height);
  }

  @Override
  public TextElement parse(Object object) {
    JSMap m = (JSMap) object;

    IRect bounds = IRect.DEFAULT_INSTANCE.parse(m.getList("b"));
    String text = m.get("s");
    ElementProperties properties = parsePropertiesFromElement(m);
    return new TextElement(properties, text, bounds);
  }

  @Override
  public String toString() {
    return text();
  }

  public TextElement applyTransform(Matrix m) {
    return withLocation(m.apply(bounds().location()));
  }

  public TextElement withText(String text) {
    return new TextElement(properties(), text, bounds());
  }

  public TextElement withLocation(IPoint loc) {
    return new TextElement(properties(), text(), bounds().withLocation(loc));
  }

  public TextElement withBounds(IRect bounds) {
    return new TextElement(properties(), text(), bounds);
  }

  public List<String> textRows() {
    if (mTextRows == null)
      mTextRows = split(text(), '\n');
    return mTextRows;
  }

  public String text() {
    return mText;
  }

  public static final float FONT_SCALE_FACTOR = 0.6f;
  public static final float FONT_HEIGHT = 16 * FONT_SCALE_FACTOR;
  public static final float PADDING = 4 * FONT_SCALE_FACTOR;
  public static final float FONT_CHAR_WIDTH = FONT_HEIGHT * 0.67f;

  private final String mText;
  private final IRect mBounds;
  private List<String> mTextRows;
}
