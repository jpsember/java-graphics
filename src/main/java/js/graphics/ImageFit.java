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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import js.base.BaseObject;
import js.geometry.FRect;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.Matrix;
import js.graphics.gen.ImageFitOptions;

/**
 * Apply heuristics to choose a 'best fit' of a source image to a target
 * rectangle. Doesn't actually involve images or pixels, just mathematical
 * rectangles
 */
@Deprecated
public final class ImageFit extends BaseObject {

  public ImageFit(ImageFitOptions opt, IPoint sourceSize) {
    mOptions = opt.build();
    mSourceRectangleSize = sourceSize;
  }

  /**
   * Construct an appropriate ImageFit instance, recycling existing one if
   * exists and is appropriate
   */
  public static ImageFit constructForSize(ImageFit existing, ImageFitOptions options,
      IPoint sourceImageSize) {
    if (existing == null || !existing.sourceSize().equals(sourceImageSize))
      existing = new ImageFit(options, sourceImageSize);
    return existing;
  }

  public ImageFitOptions options() {
    return mOptions;
  }

  public IPoint sourceSize() {
    return mSourceRectangleSize;
  }

  public IRect transformedSourceRect() {
    if (mTargetRectangle != null)
      return mTargetRectangle;

    if (!mOptions.scaleUp() || !mOptions.scaleDown())
      throw notSupported("Scale up, scale down must be true");

    float w = mSourceRectangleSize.x;
    float h = mSourceRectangleSize.y;
    float u = mOptions.targetSize().x;
    float v = mOptions.targetSize().y;

    float lambdaCrop = 1f;
    float lambdaLbox = 1f;

    switch (mOptions.fitType()) {
    default:
      throw notSupported(mOptions.fitType());
    case CROP:
      lambdaLbox = 0f;
      break;
    case LETTERBOX:
      lambdaCrop = 0f;
      break;
    case HYBRID:
      break;
    }

    float sourceAspect = h / w;
    float targetAspect = v / u;
    if (sourceAspect < targetAspect) {
      float temp = lambdaCrop;
      lambdaCrop = lambdaLbox;
      lambdaLbox = temp;
    }

    // I apply a cost function c as a function of the scale factor s:
    //
    //  c(s)   L_c(u - sw)^2 + L_l(v - sh)^2
    //
    // and take the derivative to find when c(s) is minimized, to yield optimal scale s*:
    //
    //  s* = L_c(wu) + L_l(hv)
    //       -------------------
    //       L_c(w^2) + L_l(h^2)
    //
    float s = (lambdaCrop * w * u + lambdaLbox * h * v) / (lambdaCrop * w * w + lambdaLbox * h * h);

    float resultWidth = s * w;
    float resultHeight = s * h;

    mTargetRectangleF = new FRect((u - resultWidth) * .5f, (v - resultHeight) * .5f, resultWidth,
        resultHeight);
    mTargetRectangle = mTargetRectangleF.toIRect();
    return mTargetRectangle;
  }

  public FRect transformedSourceRectF() {
    transformedSourceRect();
    return mTargetRectangleF;
  }

  public boolean cropNecessary() {
    return !transformedSourceRect().equals(new IRect(sourceSize()));
  }

  /**
   * Get matrix that transforms source image points to target image
   */
  public Matrix matrix() {
    if (mMatrix == null) {
      IRect r = transformedSourceRect();
      mMatrix = new Matrix(r.width / (float) mSourceRectangleSize.x, 0, 0,
          r.height / (float) mSourceRectangleSize.y, r.x, r.y);
    }
    return mMatrix;
  }

  public Matrix inverse() {
    if (mMatrixInv == null)
      mMatrixInv = matrix().invert();
    return mMatrixInv;
  }

  /**
   * Apply ImageFit to a BufferedImage
   * 
   * @param sourceImage
   * @param targetImageType
   *          type of BufferedImage to return
   */
  public BufferedImage apply(BufferedImage sourceImage, int targetImageType) {
    IRect destRect = transformedSourceRect();
    IPoint targetSize = options().targetSize();
    BufferedImage resultImage = ImgUtil.build(targetSize, targetImageType);
    Graphics g = resultImage.getGraphics();
    IPoint sourceSize = ImgUtil.size(sourceImage);
    g.drawImage(sourceImage, destRect.x, destRect.y, destRect.endX(), destRect.endY(), 0, 0, sourceSize.x,
        sourceSize.y, null);
    g.dispose();
    return resultImage;
  }

  private final ImageFitOptions mOptions;
  private final IPoint mSourceRectangleSize;
  private IRect mTargetRectangle;
  private FRect mTargetRectangleF;
  private Matrix mMatrix;
  private Matrix mMatrixInv;

}
