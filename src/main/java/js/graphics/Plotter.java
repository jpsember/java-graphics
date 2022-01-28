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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import js.file.DirWalk;
import js.file.Files;
import js.base.BaseObject;
import js.base.ProblemReporter;
import js.geometry.FRect;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.geometry.Polygon;

/**
 * For generating png files for inspection or test purposes
 */
public final class Plotter extends BaseObject {

  public static final int PREFERRED_IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

  /**
   * Convenience method to construct a PaintBuilder
   */
  public static Paint.Builder paint() {
    return Paint.newBuilder();
  }

  /**
   * The JComponent class has a 'paint()' method which interferes with the
   * paint() method above, so provide a different name as well
   */
  public static Paint.Builder paintBuilder() {
    return paint();
  }

  public static final Font CONSOLE_FONT;
  public static final Font LABEL_FONT;
  public static final Font BIG_FONT;

  static {
    Font unscaledFont = new Font("Courier", Font.PLAIN, 16);
    CONSOLE_FONT = unscaledFont.deriveFont(24);
    unscaledFont = new Font("Helvetica", Font.PLAIN, 16);
    LABEL_FONT = unscaledFont.deriveFont(24);
    BIG_FONT = new Font("Helvetica", Font.BOLD, 24);
  }

  private Plotter() {
  }

  /**
   * Construct a new plotter
   */
  public static Plotter build() {
    return new Plotter();
  }

  /**
   * Specify directory that will contain the images
   */
  public Plotter directory(File directory) {
    mParentDirectory = checkNotNull(directory);
    return this;
  }

  /**
   * Discard any existing images from this one's family
   */
  public Plotter purge() {
    for (File imageFile : getImageList(getParentDirectory()))
      Files.S.deleteFile(imageFile);
    mNextImageIndex = 0;
    return this;
  }

  /**
   * Render into a blank canvas of particular size
   */
  public Plotter withCanvas(int width, int height) {
    return withLeftTopRightBottom(0, 0, width, height, false);
  }

  /**
   * Render into a blank canvas of particular size
   */
  public Plotter withCanvas(IPoint canvasSize) {
    return withLeftTopRightBottom(0, 0, canvasSize.x, canvasSize.y, false);
  }

  /**
   * Render into a blank canvas of particular size, with origin at bottom left
   * instead of the usual top left
   */
  public Plotter withCanvasFlipY(IPoint canvasSize) {
    return withLeftTopRightBottom(0, canvasSize.y, canvasSize.x, 0);
  }

  /**
   * Modify graphics transformation so effective origin is in the bottom left
   * 
   */
  public Plotter flipVert() {
    IPoint imageSize = ImgUtil.size(image());
    Matrix m = Matrix.multiply(Matrix.getScale(1, -1), Matrix.getTranslate(0, -imageSize.y));
    graphics().transform(m.toAffineTransform());
    return this;
  }

  /**
   * Render into a blank canvas with particular positions of sides; determines
   * the transformation matrix
   */
  public Plotter withLeftTopRightBottom(float left, float top, float right, float bottom) {
    return withLeftTopRightBottom(left, top, right, bottom, true);
  }

  private Plotter withLeftTopRightBottom(float left, float top, float right, float bottom,
      boolean scaleToFit) {
    assertNoCanvasDefined();

    float worldWidth = Math.abs(left - right);
    float worldHeight = Math.abs(top - bottom);
    float scaleFactor = 1;
    if (scaleToFit) {
      float maxDim = Math.max(worldWidth, worldHeight);
      scaleFactor = 800 / maxDim;
    }

    Matrix mTrans = Matrix.getTranslate(-left, -top);
    Matrix mScale = Matrix.getScale(scaleFactor * Math.signum(right - left),
        scaleFactor * Math.signum(bottom - top));
    Matrix combined = Matrix.multiply(mScale, mTrans);

    mTargetImage = ImgUtil.buildRGBImage(new IPoint(scaleFactor * worldWidth, scaleFactor * worldHeight));
    createGraphics();
    mGraphics.setColor(new Color(192, 192, 192));
    mGraphics.fillRect(0, 0, mTargetImage.getWidth(), mTargetImage.getHeight());

    mGraphics.transform(combined.toAffineTransform());

    if (left != 0 || (bottom != 0 && top != 0)) {
      float originSize = scaleFactor * 50;
      with(Color.gray).renderLine(-originSize, 0, originSize, 0);
      renderLine(0, -originSize, 0, originSize);
    }
    return with(DEFAULT_PAINT);
  }

  /**
   * Get the BufferedImage being generated
   */
  public BufferedImage image() {
    if (mTargetImage == null)
      throw badState("no BufferedImage defined");
    return mTargetImage;
  }

  public Graphics2D graphics() {
    if (mGraphics == null)
      throw badState("no graphics defined");
    return mGraphics;
  }

  /**
   * Override of finalize() method to warn if no call to write() was made
   */
  @Override
  @Deprecated // This method has been deprecated in Java 9, so mark it as such to avoid maven warnings
  protected void finalize() throws Throwable {
    if (mGraphics != null)
      ProblemReporter.sharedReporter(this, "no call made to UtilityPlotter.write()");
    super.finalize();
  }

  private void createGraphics() {
    mGraphics = image().createGraphics();
    mGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    withAntiAlias(true);
  }

  public Plotter withAntiAlias(boolean state) {
    graphics().setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        state ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    return this;
  }

  /**
   * Render into an image
   */
  public Plotter into(BufferedImage sourceImage) {
    checkArgumentsEqual(sourceImage.getType(), PREFERRED_IMAGE_TYPE);
    mTargetImage = sourceImage;
    createGraphics();
    with(DEFAULT_PAINT);
    return this;
  }

  /**
   * Render into a copy of an image
   */
  public Plotter with(BufferedImage sourceImage) {
    assertNoCanvasDefined();
    BufferedImage targetImage = ImgUtil.imageAsType(sourceImage, PREFERRED_IMAGE_TYPE);
    if (targetImage == sourceImage)
      targetImage = ImgUtil.deepCopy(sourceImage);
    return into(targetImage);
  }

  private void assertNoCanvasDefined() {
    checkState(mGraphics == null, "canvas has already been defined");
  }

  /**
   * Save image to filesystem
   */
  public Plotter write() {
    File imageFile = new File(getParentDirectory(),
        String.format("%02d.%s", mNextImageIndex, mImageExtension));
    log("target file:", imageFile);
    ImgUtil.writeImage(Files.S, image(), imageFile);

    mGraphics.dispose();
    mGraphics = null;
    mTargetImage = null;
    mNextImageIndex++;
    return this;
  }

  private File getParentDirectory() {
    if (mParentDirectory == null) {
      directory(Files.getDesktopFile("utility_plotter"));

      Files.S.mkdirs(mParentDirectory);

      // Determine index of next image
      List<File> files = getImageList(mParentDirectory);

      int highestIndex = -1;
      for (File f : files) {
        String name = Files.basename(f);
        int index = Integer.parseInt(name);
        highestIndex = Math.max(index, highestIndex);
      }
      mNextImageIndex = highestIndex + 1;
    }
    return mParentDirectory;
  }

  private List<File> getImageList(File directory) {
    DirWalk w = new DirWalk(directory).withExtensions(mImageExtension).withRecurse(false);
    return w.files();
  }

  public Plotter with(Paint paint) {
    paint.apply(graphics());
    return this;
  }

  public Plotter with(Color color) {
    return with(Paint.newBuilder().color(color).width(3.0f));
  }

  public Plotter withRed() {
    return with(Color.red);
  }

  public Plotter withBlue() {
    return with(Color.blue);
  }

  public Plotter withGreen() {
    return with(new Color(30, 128, 30));
  }

  public Plotter withPurple() {
    return with(new Color(0x99, 0x33, 0xff));
  }

  public Plotter withTeal() {
    return with(new Color(0, 204, 153));
  }

  public Plotter withOlive() {
    return with(new Color(153, 153, 102));
  }

  public Plotter withBrown() {
    return with(new Color(204, 102, 0));
  }

  public Plotter fillRect(float x, float y, float w, float h) {
    graphics().fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
    return this;
  }

  public Plotter fillRect(IRect r) {
    graphics().fillRect(r.x, r.y, r.width, r.height);
    return this;
  }

  public Plotter drawRect(IRect r) {
    graphics().drawRect(r.x, r.y, r.width, r.height);
    return this;
  }

  public Plotter drawRect(float x, float y, float w, float h) {
    return drawRect(new FRect(x, y, w, h).toIRect());
  }

  public Plotter with(Matrix tfm) {
    graphics().transform(tfm.toAffineTransform());
    return this;
  }

  private static final Color DEFAULT_COLOR = new Color(201, 86, 255);
  private static final Paint DEFAULT_PAINT = paintBuilder().color(DEFAULT_COLOR).width(2).build();

  public Plotter withClosedPolyline(Iterable<IPoint> points) {
    return auxPolyline(points, true);
  }

  public Plotter withPolyline(Iterable<IPoint> points) {
    return auxPolyline(points, false);
  }

  private Plotter auxPolyline(Iterable<IPoint> points, boolean closed) {
    Path2D.Float path = new Path2D.Float();
    IPoint prevPt = null;
    for (IPoint pt : points) {
      if (prevPt == null)
        path.moveTo(pt.x, pt.y);
      else
        path.lineTo(pt.x, pt.y);
      prevPt = pt;
    }
    if (closed)
      path.closePath();
    graphics().draw(path);
    return this;
  }

  public Plotter with(Polygon polygon) {
    if (polygon == null || !polygon.isWellDefined())
      return this;
    return withClosedPolyline(arrayList(polygon.vertices()));
  }

  private void renderLine(float x1, float y1, float x2, float y2) {
    Line2D.Float prim = new Line2D.Float(x1, y1, x2, y2);
    graphics().draw(prim);
  }

  /**
   * Get list of fairly distinct colors
   */
  public static List<Color> rgbColorList() {
    return sRGBColors;
  }

  private final static List<Color> sRGBColors;

  static {
    List<Color> a = arrayList();
    a.addAll(arrayList(Color.black, Color.blue.brighter(), Color.red, Color.green, Color.cyan, Color.gray,
        Color.yellow.darker(), Color.magenta, Color.orange));
    int n = a.size() - 1;
    for (int i = 0; i < n; i++)
      a.add(a.get(i + 1).darker());
    for (int i = 0; i < n; i++)
      a.add(a.get(i + 1).brighter());
    sRGBColors = a;
  }

  private File mParentDirectory;
  private int mNextImageIndex;
  private Graphics2D mGraphics;
  private BufferedImage mTargetImage;
  private String mImageExtension = ImgUtil.EXT_PNG;

}
