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
import js.graphics.gen.CompressParam;
import js.graphics.gen.JImage;
import js.graphics.ImgUtil;

import static js.base.Tools.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.util.Arrays;

import js.data.BitReader;
import js.data.BitWriter;
import js.data.DataUtil;

public final class JImageUtil {

  /**
   * Compress a JImage using a custom compression technique (based on the FELICS
   * algorithm)
   * 
   * Where a PNG implementation is available, that may be preferable.
   */
  public static int[] encode(JImage image, CompressParam param) {
    if (image.depth() != 1)
      throw badArg("unsupported depth:", INDENT, strip(image));
    int[] encoded;
    if (image.wPixels() != null) {
      encoded = performEncode(image.size(), expandPixels(image.wPixels()), Short.SIZE, param);
    } else {
      encoded = performEncode(image.size(), expandPixels(image.bPixels()), Byte.SIZE, param);
    }
    return encoded;
  }

  /**
   * Utility method to strip the pixels from a JImage for more succinct logging
   */
  public static JImage strip(JImage image) {
    return image.build().toBuilder().bPixels(null).wPixels(null).build();
  }

  private static int[] performEncode(IPoint imageSize, int[] pixels, int componentSize, CompressParam param) {

    if (param == null)
      param = CompressParam.DEFAULT_INSTANCE;

    BitWriter w = new BitWriter();
    int width = imageSize.x;
    int height = imageSize.y;

    w.write(BITS_WIDTH_OR_HEIGHT, width);
    w.write(BITS_WIDTH_OR_HEIGHT, height);
    w.write(BITS_VERSION, VERSION);
    w.write(BITS_GOLOMB_M, param.golomb());
    w.write(BITS_PADDING, (int) (param.padding() * 100));
    w.write(BITS_DEPTH, 1);
    w.write(BITS_COMPONENT_SIZE, componentSize);
    w.write(BITS_FIRST_PIXEL, pixels[0]);

    int intervalPadding = calcIntervalPadding(param);

    int s = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++, s++) {

        int aPixel;
        int bPixel;
        if (y != 0) {
          if (x == 0) {
            aPixel = pixels[s - width];
            bPixel = pixels[s - width + 1];
          } else {
            aPixel = pixels[s - 1];
            bPixel = pixels[s - width];
          }
        } else {
          if (x == 0) {
            aPixel = bPixel = pixels[0];
          } else if (x == 1) {
            aPixel = bPixel = pixels[0];
          } else {
            aPixel = pixels[s - 2];
            bPixel = pixels[s - 1];
          }
        }

        int sPixel = pixels[s];
        int low, high;
        if (aPixel < bPixel) {
          low = aPixel;
          high = bPixel;
        } else {
          low = bPixel;
          high = aPixel;
        }

        // Expand a, b limits a bit
        low = low - intervalPadding;
        high = high + intervalPadding;

        if (sPixel < low) {
          int g = (low - 1) - sPixel;
          w.write(2, 0);
          w.writeGolomb(param.golomb(), g);
        } else if (sPixel > high) {
          int g = sPixel - (high + 1);
          w.write(2, 1);
          w.writeGolomb(param.golomb(), g);
        } else {
          w.write(1, 1);
          // If low == high, there is only one possible value, so don't attempt to encode one
          if (low != high) {
            int centered = mapPixelToCenter(sPixel, low, high);
            w.writeTruncated(high + 1 - low, centered);
          }
        }
      }
    }
    return w.result();
  }

  public static JImage decode(byte[] bytes) {
    return JImageUtil.decode(DataUtil.bytesToIntsBigEndian(bytes));
  }

  public static JImage decode(int[] compressed) {
    BitReader r = new BitReader(compressed);

    CompressParam.Builder param = CompressParam.newBuilder();

    int width = r.read(BITS_WIDTH_OR_HEIGHT);
    int height = r.read(BITS_WIDTH_OR_HEIGHT);
    checkArgument(width > 0 && height > 0);

    JImage.Builder img = JImage.newBuilder();
    img.size(new IPoint(width, height));

    int version = r.read(BITS_VERSION);
    if (version != VERSION) {
      if (version != 40)
        badArg("unexpected version:", version, "!=", VERSION);
    }
    param.golomb(r.read(BITS_GOLOMB_M));
    param.padding(r.read(BITS_PADDING) / 100f);

    img.depth(r.read(BITS_DEPTH));
    if (img.depth() != 1)
      throw badArg("unexpected image depth:", img.depth());
    int componentSize = r.read(BITS_COMPONENT_SIZE);
    int firstPixel = r.read(BITS_FIRST_PIXEL);

    int gExp = calcIntervalPadding(param);

    int[] iPixels = new int[width * height];
    int s = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++, s++) {
        // Determine the values of the high and low neighbors
        //
        int aPixel;
        int bPixel;
        if (y != 0) {
          if (x == 0) {
            aPixel = iPixels[s - width];
            bPixel = iPixels[s - width + 1];
          } else {
            aPixel = iPixels[s - 1];
            bPixel = iPixels[s - width];
          }
        } else {
          if (x == 0) {
            aPixel = bPixel = firstPixel;
          } else if (x == 1) {
            aPixel = bPixel = iPixels[0];
          } else {
            aPixel = iPixels[x - 2];
            bPixel = iPixels[x - 1];
          }
        }
        int low, high;
        if (aPixel < bPixel) {
          low = aPixel;
          high = bPixel;
        } else {
          low = bPixel;
          high = aPixel;
        }

        // Expand a, b limits a bit
        low = low - gExp;
        high = high + gExp;

        int pixel;

        int bit = r.read(1);
        if (bit == 1) {
          if (low != high) {
            pixel = r.readTruncated(high + 1 - low);
            pixel = invMapPixelToCenter(pixel, low, high);
          } else {
            pixel = low;
          }
        } else {
          bit = r.read(1);
          if (bit == 1) {
            // We're in the 'high' range
            int t = r.readGolomb(param.golomb());
            pixel = t + (high + 1);
          } else {
            // We're in the 'low' range
            int t = r.readGolomb(param.golomb());
            pixel = (low - 1) - t;
          }
        }
        iPixels[s] = pixel;
      }
    }

    if (componentSize == 8) {
      img.bPixels(reducePixelToByte(iPixels));
    } else if (componentSize == 16) {
      img.wPixels(reducePixelToShort(iPixels));
    } else
      throw badArg("unsupported component size", componentSize);
    return img.build();
  }

  private static int mapPixelToCenter(int pixel, int low, int high) {
    int middle = (low + high) >> 1;
    int j;
    if (pixel <= middle) {
      j = (middle - pixel) << 1;
    } else {
      j = ((pixel - middle) << 1) - 1;
    }
    return j;
  }

  private static int invMapPixelToCenter(int pixel, int low, int high) {
    int middle = (low + high) >> 1;
    int j;
    if ((pixel & 1) == 0) {
      j = middle - (pixel >> 1);
    } else {
      j = middle + ((pixel + 1) >> 1);
    }
    return j;
  }

  /**
   * Determine an amount of padding to expand the low...high range, so values
   * not quite within the bounds will be included. This improves the compression
   * ratio slightly; from the wikipedia article:
   * 
   * "For instance, Howard and Vitter's article recognizes that relatively flat
   * areas (with small Δ, especially where L = H) may have some noise, and
   * compression performance in these areas improves by widening the interval,
   * increasing the effective Δ."
   * 
   */
  private static int calcIntervalPadding(CompressParam param) {
    return (int) (param.golomb() * param.padding());
  }

  /**
   * <pre>
   * Format of encoded image:
   * 
   * [bits] [field description]
   * 
   * [16] image width 
   * [16] image height 
   * [8]  version number 
   * [8]  padding parameter (for expanding the L...H range) 
   * [16] golomb factor 'M' 
   * [8]  number of bits occupied by each pixel component (e.g. 8 for bytes, 16 for shorts)
   * [32] value of first pixel in image
   * 
   * </pre>
   * 
   */
  private static final int BITS_WIDTH_OR_HEIGHT = 16;
  private static final int BITS_VERSION = 8;
  private static final int BITS_PADDING = 8;
  private static final int BITS_GOLOMB_M = 16;
  private static final int BITS_DEPTH = 2;
  private static final int BITS_FIRST_PIXEL = 32;
  private static final int BITS_COMPONENT_SIZE = 8;
  private static final int VERSION = 41;

  /**
   * Convert an array of short pixels to integers
   */
  private static int[] expandPixels(short[] shortPixels) {
    int size = shortPixels.length;
    int[] r = new int[size];
    for (int i = 0; i < size; i++) {
      r[i] = ((int) shortPixels[i]) & 0xffff;
    }
    return r;
  }

  /**
   * Convert an array of bytes pixels to integers
   */
  private static int[] expandPixels(byte[] bytePixels) {
    int size = bytePixels.length;
    int[] r = new int[size];
    for (int i = 0; i < size; i++) {
      r[i] = ((int) bytePixels[i]) & 0xff;
    }
    return r;
  }

  /**
   * Convert an array of integer pixels to shorts, and check for overflow
   */
  private static short[] reducePixelToShort(int[] pixels) {
    int size = pixels.length;
    short[] r = new short[size];
    for (int i = 0; i < size; i++) {
      int p = pixels[i];
      if ((p & ~0xffff) != 0)
        throw badArg("pixel value", p, "doesn't fit in short");
      r[i] = (short) p;
    }
    return r;
  }

  /**
   * Convert an array of integer pixels to bytes, and check for overflow
   */
  private static byte[] reducePixelToByte(int[] pixels) {
    int size = pixels.length;
    byte[] r = new byte[size];
    for (int i = 0; i < size; i++) {
      int p = pixels[i];
      if ((p & ~0xff) != 0)
        throw badArg("pixel value", p, "doesn't fit in byte");
      r[i] = (byte) p;
    }
    return r;
  }

  public static BufferedImage toBufferedImage(JImage source) {
    if (source.depth() == 1 && source.wPixels() != null) {
      BufferedImage bufferedImage = ImgUtil.build(source.size(), BufferedImage.TYPE_USHORT_GRAY);
      short[] array = ((DataBufferUShort) bufferedImage.getRaster().getDataBuffer()).getData();
      short[] srcPix = source.wPixels();
      checkArgument(srcPix.length == array.length);
      System.arraycopy(srcPix, 0, array, 0, srcPix.length);
      return bufferedImage;
    } else if (source.depth() == 1 && source.bPixels() != null) {
      BufferedImage bufferedImage = ImgUtil.build(source.size(), BufferedImage.TYPE_BYTE_GRAY);
      byte[] array = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
      byte[] srcPix = source.bPixels();
      checkArgument(srcPix.length == array.length);
      System.arraycopy(srcPix, 0, array, 0, srcPix.length);
      return bufferedImage;
    } else
      throw notSupported("unsupported JImage format:", INDENT, strip(source));
  }

  public static JImage from(BufferedImage img) {
    JImage result;
    switch (img.getType()) {
    default:
      throw notSupported("unsupported BufferedImage format:", INDENT, ImgUtil.toJson(img));

    case BufferedImage.TYPE_USHORT_GRAY: {
      short[] array = ((DataBufferUShort) img.getRaster().getDataBuffer()).getData();
      result = JImage.newBuilder()//
          .depth(1)//
          .size(ImgUtil.size(img))//
          .wPixels(Arrays.copyOf(array, array.length))//
          .build();
    }
      break;

    case BufferedImage.TYPE_BYTE_GRAY: {
      byte[] array = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
      result = JImage.newBuilder()//
          .depth(1)//
          .size(ImgUtil.size(img))//
          .bPixels(Arrays.copyOf(array, array.length))//
          .build();
    }
      break;
    }
    return result;
  }

}
