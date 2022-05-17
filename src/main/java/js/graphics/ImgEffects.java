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

import java.awt.geom.*;
import java.awt.image.*;

public final class ImgEffects {

  public static BufferedImage applyTransform(BufferedImage sourceImage, AffineTransform affineTransform) {
    return applyTransform(sourceImage, affineTransform, null);
  }

  public static BufferedImage applyTransform(BufferedImage sourceImage, AffineTransform affineTransform,
      BufferedImage targetImageOrNull) {
    AffineTransformOp op = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
    return op.filter(sourceImage, targetImageOrNull);
  }

  public static BufferedImage flipVertically(BufferedImage sourceImage) {
    AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
    tx.translate(0, -sourceImage.getHeight());
    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    return op.filter(sourceImage, null);
  }

  public static BufferedImage flipHorizontally(BufferedImage sourceImage) {
    AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
    tx.translate(-sourceImage.getWidth(), 0);
    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    return op.filter(sourceImage, null);
  }

  /**
   * Construct a copy of a BufferedImage. Useful in case the source image's
   * pixels are not contiguous, i.e., they are a subimage view of a larger image
   */
  public static BufferedImage copy(BufferedImage img) {
    BufferedImage target = ImgUtil.build(ImgUtil.size(img), img.getType());
    img.copyData(target.getRaster());
    return target;
  }

  public static BufferedImage adjustBrightness(BufferedImage sourceImage, double scaleFactor) {
    RescaleOp op = new RescaleOp((float) scaleFactor, 0, null);
    return op.filter(sourceImage, null);
  }

  public static BufferedImage sharpen(BufferedImage sourceImage) {
    BufferedImageOp op = new ConvolveOp(sSharpenKernel);
    return op.filter(sourceImage, null);
  }

  public static BufferedImage blur(BufferedImage src) {
    BufferedImageOp op = new ConvolveOp(sBlurKernel);
    return op.filter(src, null);
  }

  /**
   * Scale an image, or return original if no scaling required
   */
  public static BufferedImage scale(BufferedImage img, double scaleFactor) {
    if (scaleFactor == 1.0)
      return img;
    AffineTransform atx = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
    return applyTransform(img, atx);
  }

  public static BufferedImage makeMonochrome3Channel(BufferedImage image) {
    BufferedImage result = ImgUtil.imageOfSameSize(image);
    byte[] bgrIn = ImgUtil.bgrPixels(image);
    byte[] bgrOut = ImgUtil.bgrPixels(result);
    for (int i = 0; i < bgrIn.length; i += 3) {
      float c0 = ((int) (bgrIn[i + 0] & 0xff)) * 0.114f;
      float c1 = ((int) (bgrIn[i + 1] & 0xff)) * 0.587f;
      float c2 = ((int) (bgrIn[i + 2] & 0xff)) * 0.299f;
      int gray = (int) (c0 + c1 + c2);
      if (gray > 255)
        gray = 255;
      bgrOut[i + 0] = bgrOut[i + 1] = bgrOut[i + 2] = (byte) gray;
    }
    return result;
  }

  @Deprecated // Use makeMonochrome3/1Channel
  public static BufferedImage makeMonochrome(BufferedImage image) {
    return makeMonochrome3Channel(image);
  }

  public static BufferedImage makeMonochrome1Channel(BufferedImage image) {
    BufferedImage result = ImgUtil.build(ImgUtil.size(image), BufferedImage.TYPE_BYTE_GRAY);
    byte[] bgrIn = ImgUtil.bgrPixels(image);
    byte[] grayOut = ImgUtil.gray8Pixels(result);
    int j = 0;
    for (int i = 0; i < bgrIn.length; i += 3) {
      float c0 = ((int) (bgrIn[i + 0] & 0xff)) * 0.114f;
      float c1 = ((int) (bgrIn[i + 1] & 0xff)) * 0.587f;
      float c2 = ((int) (bgrIn[i + 2] & 0xff)) * 0.299f;
      int gray = (int) (c0 + c1 + c2);
      if (gray > 255)
        gray = 255;
      grayOut[j++] = (byte) gray;
    }
    return result;
  }

  private static final Kernel sSharpenKernel = new Kernel(3, 3,
      new float[] { -1, -1, -1, -1, 9, -1, -1, -1, -1 });
  private static final Kernel sBlurKernel = new Kernel(3, 3,
      new float[] { 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f });

}
