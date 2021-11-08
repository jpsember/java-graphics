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
import js.geometry.Polygon;
import js.graphics.gen.ElementProperties;
import js.json.JSList;
import js.json.JSMap;

/**
 * A ScriptElement representing an axis-aligned rectangle (or box)
 */
public class RectElement extends AbstractScriptElement {

  public static final RectElement DEFAULT_INSTANCE = new RectElement(null, IRect.DEFAULT_INSTANCE);
  public static final String TAG = "box";

  // Consider moving this elsewhere
  public static final int BOX_ROT_MIN = -90;
  public static final int BOX_ROT_MAX = 90;

  @Override
  public String tag() {
    return TAG;
  }

  @Override
  public IPoint location() {
    return mBounds.location();
  }

  @Override
  public IRect bounds() {
    return mBounds;
  }

  @Override
  public final String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    loadTools();
    // Rects are the default elements, so omit the tag
    return toJsonAux(null).remove(ScriptUtil.TAG_KEY) //
        .put("bounds", mBounds.toJson());
  }

  @Override
  public RectElement parse(Object object) {
    JSMap m = (JSMap) object;
    IRect bounds;
    // If there is no 'bounds' element, the dimensions might be stored in the old x0, y0, x1, y1 format instead
    JSList boundsList = m.optJSList("bounds");
    if (boundsList != null) {
      bounds = IRect.DEFAULT_INSTANCE.parse(boundsList);
    } else {
      int x0 = m.getInt("x0");
      int y0 = m.getInt("y0");
      int x1 = m.getInt("x1");
      int y1 = m.getInt("y1");
      bounds = new IRect(x0, y0, x1 - x0, y1 - y0);
    }
    ElementProperties properties = parsePropertiesFromElement(m);

    // If there's an explicit 'rot' argument, replace any existing one in the properties
    Integer rot = m.optInt("rot");
    if (rot != null)
      properties = properties.toBuilder().rotation(rot).build();

    return new RectElement(properties, bounds);
  }

  public RectElement(ElementProperties properties, IRect bounds) {
    super(properties);
    mBounds = bounds;
  }

  public RectElement withBounds(IRect bounds) {
    return new RectElement(properties(), bounds);
  }

  @Override
  public RectElement withProperties(ElementProperties properties) {
    return new RectElement(properties, bounds());
  }

  @Override
  public RectElement applyTransform(Matrix matrix) {
    return withBounds(applyTruncatedHeuristicTransform(bounds(), matrix));
  }

  protected final IRect mBounds;

  /**
   * Construct list of vertices of a truncated rectangle
   */
  public static List<IPoint> truncatedRect(IRect r, float s) {
    List<IPoint> pts = arrayList();

    float x0 = r.x;
    float y0 = r.y;
    float x3 = r.endX();
    float y3 = r.endY();

    float x1 = x0 * (1 - s) + x3 * s;
    float x2 = x3 * (1 - s) + x0 * s;
    float y1 = y0 * (1 - s) + y3 * s;
    float y2 = y3 * (1 - s) + y0 * s;

    addPt(pts, x1, y0);
    addPt(pts, x2, y0);
    addPt(pts, x3, y1);
    addPt(pts, x3, y2);
    addPt(pts, x2, y3);
    addPt(pts, x1, y3);
    addPt(pts, x0, y2);
    addPt(pts, x0, y1);

    return pts;
  }

  @Deprecated
  public static List<IPoint> truncatedRect(IRect r) {
    return truncatedRect(r, 0.2f);
  }

  /**
   * Apply a transformation to IRect, using truncated rect convex hull heuristic
   */
  public static IRect applyTruncatedHeuristicTransform(IRect rect, Matrix tfm) {
    List<IPoint> hullPoints = truncatedRect(rect, 0.2f);
    Polygon trunc = Polygon.DEFAULT_INSTANCE.withVertices(hullPoints);
    Polygon tfmPoly = trunc.applyTransform(tfm);
    return tfmPoly.bounds();
  }

  private static void addPt(List<IPoint> dest, float x, float y) {
    dest.add(new IPoint(x, y));
  }

}
