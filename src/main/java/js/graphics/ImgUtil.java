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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.*;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import static js.base.Tools.*;

import js.file.Files;
import js.base.BasePrinter;
import js.base.Tools;
import js.data.DataUtil;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.geometry.MyMath;
import js.graphics.gen.MonoImage;
import js.json.JSList;
import js.json.JSMap;
import js.json.JSObject;

public final class ImgUtil {

  // ------------------------------------------------------------------
  // Image file extensions
  // ------------------------------------------------------------------

  public static final String EXT_JPEG = "jpg";
  public static final String EXT_PNG = "png";
  public static final String EXT_JMG = "jmg";
  public static final String EXT_RAX = "rax";

  public static final List<String> IMAGE_EXTENSIONS = arrayList(EXT_JPEG, EXT_PNG, "jpeg", EXT_RAX);
  public static final int PREFERRED_IMAGE_TYPE_COLOR = BufferedImage.TYPE_INT_RGB;

  // ------------------------------------------------------------------
  // Reading images
  // ------------------------------------------------------------------

  public static BufferedImage read(File src) {
    // If file is a custom format, treat appropriately
    String ext = Files.getExtension(src);
    if (ext.equals(EXT_RAX)) {
      MonoImage monoImage = readRax(src);
      return MonoImageUtil.toBufferedImage(monoImage);
    }
    return read(Files.openInputStream(src));
  }

  public static BufferedImage read(byte[] bytes) {
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    return read(input);
  }

  public static BufferedImage read(InputStream inputStream) {
    try {
      BufferedImage img = register(ImageIO.read(inputStream));
      inputStream.close();
      return img;
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  // ------------------------------------------------------------------
  // Support for .rax files
  // ------------------------------------------------------------------

  private static final int RAX_COMPRESS_HEADER_LENGTH = 6;
  private static final byte RAX_COMPRESS_FLAG = (byte) 0xfd;
  private static final int RAX_VERSION_1 = 1;

  /**
   * Read .rax from input stream
   */
  @Deprecated
  public static MonoImage readRax(InputStream inputStream) {
    byte[] content = Files.toByteArray(inputStream, "ImgUtil.readRax");
    return decompressRAX(content, null);
  }

  public static MonoImage readRax(File file) {
    byte[] content = Files.toByteArray(file, "ImgUtil.readRax");
    return decompressRAX(content, null);
  }

  private static final int RAX_DEFAULT_PIXEL_VALUE = 20000;
  private static final int FLIR_COMPRESS_FLAG = 0xfd;
  private static final int FLIR_VERSION_1 = 1;
  private static final byte JUMP_SIGNAL = -128;

  /**
   * Compress a 16-bit monochrome image to .rax format
   */
  public static byte[] compressRAX(MonoImage image) {
    return compressRAX(image.size(), image.pixels());
  }

  /**
   * Compress a 16-bit monochrome image to .rax format
   */
  public static byte[] compressRAX(IPoint imageSize, short[] iPixels) {
    int imageWidth = imageSize.x;
    int imageHeight = imageSize.y;
    ByteArrayOutputStream output = new ByteArrayOutputStream((imageSize.product() * 3) / 2);

    // Write header
    output.write(FLIR_COMPRESS_FLAG);
    output.write(FLIR_VERSION_1);
    output.write(imageWidth);
    output.write(imageWidth >> 8);
    output.write(imageHeight);
    output.write(imageHeight >> 8);

    int[] deltaValues = new int[imageWidth];

    int rowOffset = 0;
    for (int rowNumber = 0; rowNumber < imageHeight; rowNumber++, rowOffset += imageWidth) {
      int rowOffsetM1 = rowOffset - imageWidth;
      int rowOffsetM2 = rowOffsetM1 - imageWidth;
      int prevH1, prevH2;
      if (rowNumber == 0) {
        prevH1 = RAX_DEFAULT_PIXEL_VALUE;
        prevH2 = RAX_DEFAULT_PIXEL_VALUE;
        for (int x = 0; x < imageWidth; x++) {
          deltaValues[x] = v1Scale(prevH1 - prevH2) + prevH1;
          prevH2 = prevH1;
          prevH1 = iPixels[rowOffset + x];
        }
      } else if (rowNumber == 1) {
        prevH1 = iPixels[0];
        prevH2 = prevH1;
        for (int x = 0; x < imageWidth; x++) {
          int prevV1 = iPixels[rowOffsetM1 + x];
          deltaValues[x] = (v1Scale(prevH1 - prevH2) + (prevH1 + prevV1)) / 2;
          prevH2 = prevH1;
          prevH1 = iPixels[rowOffset + x];
        }
      } else {
        prevH1 = iPixels[rowOffsetM1];
        prevH2 = iPixels[rowOffsetM2];
        for (int x = 0; x < imageWidth; x++) {
          int prevV1 = iPixels[rowOffsetM1 + x];
          int prevV2 = iPixels[rowOffsetM2 + x];
          deltaValues[x] = (v1Scale((prevH1 - prevH2) + (prevV1 - prevV2)) + (prevH1 + prevV1)) / 2;
          prevH2 = prevH1;
          prevH1 = iPixels[rowOffset + x];
        }
      }

      for (int x = 0; x < imageWidth; x++) {
        int prediction = deltaValues[x];
        int pixel = iPixels[rowOffset + x];
        int error = pixel - prediction;

        if (error != (byte) error || error == JUMP_SIGNAL) {
          output.write(JUMP_SIGNAL);
          output.write(pixel);
          output.write(pixel >> 8);
        } else
          output.write(error);
      }
    }

    return output.toByteArray();
  }

  /**
   * Decompress .rax image
   */
  public static MonoImage decompressRAX(byte[] byteBuffer, short[] outputPixelsOrNull) {
    MonoImage.Builder monoImage = MonoImage.newBuilder();
    IPoint imageSize = looksLikeCompressedRawImage(byteBuffer);
    if (imageSize == null)
      throw new IllegalArgumentException("does not look like a compressed RawImage");
    monoImage.size(imageSize);
    byte version = byteBuffer[1];
    checkArgument(version == RAX_VERSION_1, "unexpected version: " + version);

    ByteArrayInputStream input = new ByteArrayInputStream(byteBuffer, RAX_COMPRESS_HEADER_LENGTH,
        byteBuffer.length - RAX_COMPRESS_HEADER_LENGTH);

    int imageWidth = imageSize.x;
    int imageHeight = imageSize.y;

    int expectedLength = imageWidth * imageHeight;
    short[] outputPixels = DataUtil.shortArray(expectedLength, outputPixelsOrNull);
    monoImage.pixels(outputPixels);
    int rowOffset = 0;
    for (int rowNumber = 0; rowNumber < imageHeight; rowNumber++, rowOffset += imageWidth) {
      int rowOffsetM2 = rowOffset - 2 * imageWidth;
      int rowOffsetM1 = rowOffset - imageWidth;
      int prevH1, prevH2;

      if (rowNumber == 0) {
        prevH1 = RAX_DEFAULT_PIXEL_VALUE;
        prevH2 = RAX_DEFAULT_PIXEL_VALUE;
        for (int x = 0; x < imageWidth; x++) {
          int prediction = v1Scale(prevH1 - prevH2) + prevH1;
          int pixel = extractIntegerFromStream(input, prediction);
          outputPixels[rowOffset + x] = (short) pixel;
          prevH2 = prevH1;
          prevH1 = pixel;
        }
      } else if (rowNumber == 1) {
        prevH1 = outputPixels[0];
        prevH2 = prevH1;
        for (int x = 0; x < imageWidth; x++) {
          int prevV1 = outputPixels[rowOffsetM1 + x];
          int prediction = (v1Scale(prevH1 - prevH2) + (prevH1 + prevV1)) / 2;
          int pixel = extractIntegerFromStream(input, prediction);
          outputPixels[rowOffset + x] = (short) pixel;
          prevH2 = prevH1;
          prevH1 = pixel;
        }
      } else {
        prevH1 = outputPixels[rowOffsetM1];
        prevH2 = outputPixels[rowOffsetM2];
        for (int x = 0; x < imageWidth; x++) {
          int prevV1 = outputPixels[rowOffsetM1 + x];
          int prevV2 = outputPixels[rowOffsetM2 + x];
          int prediction = (v1Scale((prevH1 - prevH2) + (prevV1 - prevV2)) + (prevH1 + prevV1)) / 2;
          int pixel = extractIntegerFromStream(input, prediction);
          outputPixels[rowOffset + x] = (short) pixel;
          prevH2 = prevH1;
          prevH1 = pixel;
        }
      }
    }
    return monoImage.build();
  }

  private static int v1Scale(int value) {
    // Get same effect as multiplying by 0.280f, but without using floating point
    return (value * 7) / 25;
  }

  private static int extractIntegerFromStream(ByteArrayInputStream stream, int prediction) {
    int result;
    byte value = (byte) stream.read();
    if (value == JUMP_SIGNAL) {
      int lb = stream.read();
      int hb = stream.read();
      int value2 = lb + (hb << 8);
      result = (value2 & 0xffff);
    } else {
      result = prediction + value;
    }
    return result;
  }

  /**
   * Determine if an array of bytes looks like an image in rax format. If so,
   * return the dimensions of the image; else, null
   */
  public static IPoint looksLikeCompressedRawImage(byte[] byteBuffer) {
    IPoint result = null;
    do {
      if (byteBuffer.length < RAX_COMPRESS_HEADER_LENGTH)
        break;
      if (byteBuffer[0] != RAX_COMPRESS_FLAG)
        break;
      int imageWidth = toInt(byteBuffer[2]) + (toInt(byteBuffer[3]) << 8);
      int imageHeight = toInt(byteBuffer[4]) + (toInt(byteBuffer[5]) << 8);
      if (imageWidth < 1 || imageWidth > 2048 || imageHeight < 1 || imageHeight > 2048)
        break;
      result = new IPoint(imageWidth, imageHeight);
    } while (false);
    return result;
  }

  /**
   * Interpret a byte as an unsigned 8-bit int
   */
  private static int toInt(byte b) {
    return ((int) (b & 0xff));
  }

  // ------------------------------------------------------------------
  // Encoding images for writing
  // ------------------------------------------------------------------

  public static byte[] toJPEG(BufferedImage img, Integer requiredTypeOrNull) {
    return toJPEG(img, requiredTypeOrNull, null);
  }

  public static byte[] toJPEG(BufferedImage img, Integer requiredTypeOrNull, Float requiredQualityOrNull) {
    if (requiredTypeOrNull != null)
      assertImageType(img, requiredTypeOrNull);

    ByteArrayOutputStream outStream = null;
    try {
      outStream = new ByteArrayOutputStream();
      if (requiredQualityOrNull == null) {
        boolean wrote = ImageIO.write(img, EXT_JPEG, outStream);
        if (!wrote)
          throw die("No writer found for image:", img);
      } else {
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        jpegParams.setProgressiveMode(JPEGImageWriteParam.MODE_DISABLED);
        jpegParams.setOptimizeHuffmanTables(true);

        checkArgument(requiredQualityOrNull > 0 && requiredQualityOrNull <= 1.0f);
        jpegParams.setCompressionQuality(requiredQualityOrNull);
        ImageOutputStream outputStream = ImageIO.createImageOutputStream(outStream);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(outputStream);
        writer.write(null, new IIOImage(img, null, null), jpegParams);
      }
      return outStream.toByteArray();
    } catch (IOException e) {
      throw Files.asFileException(e);
    } finally {
      Files.close(outStream);
    }
  }

  public static byte[] toPNG(BufferedImage img) {
    try {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      ImageIO.write(img, EXT_PNG, outStream);
      return outStream.toByteArray();
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  //------------------------------------------------------------------
  // Writing images
  // ------------------------------------------------------------------

  public static void writeImage(Files files, BufferedImage img, File dest) {
    String ext = Files.getExtension(dest);
    byte[] bytes = null;
    if (ext.equals(EXT_JPEG)) {
      int type = img.getType();
      if (type != BufferedImage.TYPE_3BYTE_BGR && type != BufferedImage.TYPE_INT_RGB)
        badArg("image has unexpected type:", type);
      bytes = toJPEG(img, null);
    } else if (ext.equals(EXT_PNG))
      bytes = toPNG(img);
    checkState(bytes != null, "Unsupported image type:", dest);
    files.write(toPNG(img), dest);
  }

  public static void writeJPG(Files files, BufferedImage img, File dest, Integer requiredTypeOrNull) {
    checkArgument(Files.getExtension(dest).equals(EXT_JPEG), "unsupported extension:", dest);
    files.write(toJPEG(img, requiredTypeOrNull), dest);
  }

  public static void writeRAX(Files files, MonoImage img, File dest) {
    checkArgumentsEqual(Files.getExtension(dest), ImgUtil.EXT_RAX);
    Files.S.write(ImgUtil.compressRAX(img), dest);
  }

  // ------------------------------------------------------------------
  // Image attributes
  // ------------------------------------------------------------------

  public static BufferedImage assertImageType(BufferedImage image, int expectedType) {
    if (image.getType() != expectedType)
      throw badArg("BufferedImage type", image.getType(), "is not expected type", expectedType);
    return image;
  }

  /**
   * Get size of BufferedImage
   */
  public static IPoint size(BufferedImage image) {
    return new IPoint(image.getWidth(), image.getHeight());
  }

  /**
   * Read size of an image from its file
   */
  public static IPoint size(File imageFile) {
    String suffix = Files.getExtension(imageFile);
    for (ImageReader reader : in(ImageIO.getImageReadersBySuffix(suffix))) {
      try {
        reader.setInput(new FileImageInputStream(imageFile));
        return new IPoint(reader.getWidth(reader.getMinIndex()), reader.getHeight(reader.getMinIndex()));
      } catch (Throwable t) {
        throw asRuntimeException(t);
      } finally {
        reader.dispose();
      }
    }
    throw badArg("no reader for suffix:", suffix, "file:", imageFile);
  }

  private static final Map<Integer, String> sImageTypeStringMap = concurrentHashMap();
  static {
    sImageTypeStringMap.put(BufferedImage.TYPE_INT_ARGB, "ARGB");
    sImageTypeStringMap.put(BufferedImage.TYPE_INT_RGB, "RGB");
    sImageTypeStringMap.put(BufferedImage.TYPE_USHORT_GRAY, "M_16");
    sImageTypeStringMap.put(BufferedImage.TYPE_INT_BGR, "BGR");
  }

  public static String imageTypeAsString(int imageType) {
    String s = sImageTypeStringMap.get(imageType);
    if (s == null)
      s = "?_" + imageType;
    return s;
  }

  // ------------------------------------------------------------------
  // Constructing BufferedImages
  // ------------------------------------------------------------------

  /**
   * Build BufferedImage with particular size and type
   */
  public static BufferedImage build(IPoint size, int bufferedImageType) {
    return register(new BufferedImage(size.x, size.y, bufferedImageType));
  }

  /**
   * Build BufferedImage.TYPE_INT_RGB image
   */
  public static BufferedImage buildRGBImage(IPoint size) {
    return build(size, BufferedImage.TYPE_INT_RGB);
  }

  /**
   * Build BufferedImage.TYPE_USHORT_GRAY (16-bit monochrome pixels)
   */
  public static BufferedImage build16BitGrayscaleImage(IPoint size) {
    return build(size, BufferedImage.TYPE_USHORT_GRAY);
  }

  /**
   * Build BufferedImage.TYPE_BYTE_GRAY image
   */
  public static BufferedImage build8BitGrayscaleImage(IPoint size) {
    return build(size, BufferedImage.TYPE_BYTE_GRAY);
  }

  /**
   * Build a BufferedImage with the same size of another but with a particular
   * type
   */
  public static BufferedImage imageOfSameSize(BufferedImage sourceImage, int desiredType) {
    return build(size(sourceImage), desiredType);
  }

  /**
   * Build a BufferedImage with the same size and type of another
   */
  public static BufferedImage imageOfSameSize(BufferedImage sourceImage) {
    return imageOfSameSize(sourceImage, sourceImage.getType());
  }

  /**
   * Return copy of image, with a new type, or the original if types are the
   * same
   */
  public static BufferedImage imageAsType(BufferedImage sourceImage, int targetType) {
    if (sourceImage.getType() == targetType)
      return sourceImage;
    BufferedImage targetImage = imageOfSameSize(sourceImage, targetType);
    Graphics g = targetImage.createGraphics();
    g.drawImage(sourceImage, 0, 0, null);
    g.dispose();
    return targetImage;
  }

  /**
   * Build a deep copy of a BufferedImage (so its pixels are contiguous within
   * memory)
   */
  public static BufferedImage deepCopy(BufferedImage sourceImage) {
    ColorModel cm = sourceImage.getColorModel();
    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
    WritableRaster raster = sourceImage.copyData(sourceImage.getRaster().createCompatibleWritableRaster());
    return register(new BufferedImage(cm, raster, isAlphaPremultiplied, null));
  }

  /**
   * Build a deep copy of a subimage of a BufferedImage (so its pixels are
   * contiguous within memory)
   */
  public static BufferedImage subimage(BufferedImage sourceImage, int x, int y, int w, int h) {
    return deepCopy(sourceImage.getSubimage(x, y, w, h));
  }

  /**
   * Build a deep copy of a subimage of a BufferedImage (so its pixels are
   * contiguous within memory)
   */
  public static BufferedImage subimage(BufferedImage sourceImage, IRect rect) {
    return subimage(sourceImage, rect.x, rect.y, rect.width, rect.height);
  }

  // ------------------------------------------------------------------
  // Reading pixels from images
  // ------------------------------------------------------------------

  public static byte[] gray8Pixels(BufferedImage gray8Image) {
    BufferedImage image = gray8Image;
    assertImageType(image, BufferedImage.TYPE_BYTE_GRAY);
    return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
  }

  public static int[] rgbPixels(BufferedImage bufferedImage_INT_RGB) {
    BufferedImage image = bufferedImage_INT_RGB;
    assertImageType(image, BufferedImage.TYPE_INT_RGB);
    int[] array = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    DataUtil.assertLength(array.length, image.getWidth() * image.getHeight(),
        "rgbPixels; did cropping occur?");
    return array;
  }

  public static byte[] bgrPixels(BufferedImage bufferedImage_3BYTE_BGR) {
    BufferedImage image = bufferedImage_3BYTE_BGR;
    assertImageType(image, BufferedImage.TYPE_3BYTE_BGR);
    byte[] array = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    DataUtil.assertLength(array.length, 3 * image.getWidth() * image.getHeight(),
        "3BYTE_BGR pixels; did cropping occur?");
    return array;
  }

  public static int[] argbPixels(BufferedImage bufferedImage_INT_ARGB) {
    BufferedImage image = bufferedImage_INT_ARGB;
    assertImageType(image, BufferedImage.TYPE_INT_ARGB);
    int[] array = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    DataUtil.assertLength(array.length, image.getWidth() * image.getHeight(),
        "argbPixels; did cropping occur?");
    return array;
  }

  /**
   * Convert image pixels to array of float; only some image types are supported
   * 
   * If numChannels is 1, and image is color, it uses only the green channel
   */
  public static float[] floatPixels(BufferedImage sourceImage, int numChannels, float[] destinationOrNull) {

    int pixCount = ImgUtil.size(sourceImage).product();

    float[] result;
    switch (sourceImage.getType()) {

    default:
      throw badArg("unsupported image type:", INDENT, sourceImage);

    case BufferedImage.TYPE_INT_RGB: {
      int[] sourcePixels = ImgUtil.rgbPixels(sourceImage);
      result = DataUtil.floatArray(pixCount * numChannels, destinationOrNull);
      if (numChannels == 1) {
        int j = 0;
        for (int pixel : sourcePixels) {
          int green = (pixel >> 8) & 0xff;
          result[j] = green * RGB_TO_FLOAT;
          j++;
        }
      } else {
        int[] rgb = new int[3];
        int j = 0;
        for (int pixel : sourcePixels) {
          parseRGB(pixel, rgb);
          result[j] = rgb[0] * RGB_TO_FLOAT;
          result[j + 1] = rgb[1] * RGB_TO_FLOAT;
          result[j + 2] = rgb[2] * RGB_TO_FLOAT;
          j += 3;
        }
      }
    }
      break;

    case BufferedImage.TYPE_3BYTE_BGR: {
      byte[] sourcePixels = ImgUtil.bgrPixels(sourceImage);
      result = DataUtil.floatArray(pixCount * numChannels, destinationOrNull);
      if (numChannels == 1) {
        int len = sourcePixels.length;
        int i = 0;
        for (int j = 0; j < len; j += 3) {
          float f = bytePixelValueToFloat(sourcePixels[j + 1]);
          result[i] = f;
          i++;
        }
      } else {
        int len = sourcePixels.length;
        for (int j = 0; j < len; j += 3) {
          result[j] = bytePixelValueToFloat(sourcePixels[j + 2]);
          result[j + 1] = bytePixelValueToFloat(sourcePixels[j + 1]);
          result[j + 2] = bytePixelValueToFloat(sourcePixels[j + 0]);
        }
      }
    }
      break;
    }

    return result;
  }

  private static float bytePixelValueToFloat(byte b) {
    return (((int) b) & 0xff) * RGB_TO_FLOAT;
  }

  // ------------------------------------------------------------------
  // Construct BufferedImage from pixels
  // ------------------------------------------------------------------

  /**
   * Construct a BufferedImage.TYPE_INT_BGR from an array of bytes
   */
  public static BufferedImage bytesToBGRImage(byte[] bgrIn, IPoint size) {
    if (bgrIn.length != size.product() * 3)
      badArg("wrong length for pixel bytes:", bgrIn.length, "for image of size:", size);
    BufferedImage result = ImgUtil.build(size, BufferedImage.TYPE_3BYTE_BGR);
    byte[] bgrOut = ImgUtil.bgrPixels(result);
    System.arraycopy(bgrIn, 0, bgrOut, 0, bgrIn.length);
    return result;
  }

  /**
   * Convert an image from an array of floats to a BufferedImage.TYPE_INT_RGB.
   * Assumes each float represents a color component (red, green, blue, or
   * grayscale) from 0...1
   */
  public static BufferedImage floatsToBufferedImage(float[] floats, IPoint imageSize, int imageChannels) {
    int expectedLength;
    int pixelCount = imageSize.product();
    if (imageChannels == 1)
      expectedLength = pixelCount;
    else if (imageChannels == 3)
      expectedLength = pixelCount * imageChannels;
    else
      throw badArg("unsupported imageChannels", imageChannels);

    DataUtil.assertLength(floats, expectedLength, "floatsToBufferedImage");

    BufferedImage result = build(imageSize, BufferedImage.TYPE_INT_RGB);
    int[] targetPixels = rgbPixels(result);
    if (imageChannels == 1) {
      for (int i = 0; i < floats.length; i++) {
        int gray = floatToClampInt(floats[i]);
        targetPixels[i] = compileRGB(gray, gray, gray);
      }
    } else {
      int j = 0;
      for (int i = 0; i < floats.length; i += 3, j++) {
        int red = floatToClampInt(floats[i]);
        int green = floatToClampInt(floats[i + 1]);
        int blue = floatToClampInt(floats[i + 2]);
        targetPixels[j] = compileRGB(red, green, blue);
      }
    }
    return result;
  }

  private static int floatToClampInt(float f) {
    f = MyMath.clamp(f * FLOAT_TO_RGB, 0, 255);
    return (int) (f + 0.5f);
  }

  /**
   * Convert image pixels to float array
   */
  public static float[] bufferedImageToFloat(BufferedImage sourceImage, int numChannels,
      float[] destinationOrNull) {

    int[] pixels;

    switch (sourceImage.getType()) {

    case BufferedImage.TYPE_INT_RGB:
      pixels = ImgUtil.rgbPixels(sourceImage);
      break;

    default:
      throw die("unsupported image type:", INDENT, toJson(sourceImage));
    }

    float[] destination = destinationOrNull;
    int requiredLength = pixels.length * numChannels;
    if (destination == null) {
      destination = new float[requiredLength];
    } else if (destination.length != requiredLength) {
      pr("sourceImage:", INDENT, ImgUtil.toJson(sourceImage));
      pr("numChannels:", numChannels);
      pr("destinationOrNull:", destinationOrNull);
      pr("pixels.length:", pixels.length);
      pr("requiredLength:", requiredLength);
      throw die("unexpected length", destination.length, "!=", requiredLength);
    }

    switch (sourceImage.getType()) {
    case BufferedImage.TYPE_INT_RGB: {
      if (numChannels == 1) {
        int j = 0;
        for (int pixel : pixels) {
          int green = (pixel >> 8) & 0xff;
          destination[j] = green * RGB_TO_FLOAT;
          j++;
        }
      } else {
        int[] rgb = new int[3];
        int j = 0;
        for (int pixel : pixels) {
          ImgUtil.parseRGB(pixel, rgb);
          destination[j] = rgb[0] * RGB_TO_FLOAT;
          destination[j + 1] = rgb[1] * RGB_TO_FLOAT;
          destination[j + 2] = rgb[2] * RGB_TO_FLOAT;
          j += 3;
        }
      }
    }
      break;

    default:
      throw die("unsupported");
    }

    return destination;
  }

  // ------------------------------------------------------------------
  // Conversion factors
  // ------------------------------------------------------------------

  private static final float RGB_TO_FLOAT = 1.0f / 255;
  private static final float FLOAT_TO_RGB = 1 / RGB_TO_FLOAT;

  // ------------------------------------------------------------------
  // Encoding and decoding pixels to color componenets
  // ------------------------------------------------------------------

  public static int[] parseRGBA(int rgba) {
    return parseRGBA(rgba, null);
  }

  public static int[] parseRGBA(int rgba, int[] destinationOrNull) {
    int[] dest = destinationOrNull;
    if (dest == null)
      dest = new int[4];
    // RED
    dest[0] = (rgba >> 24) & 0xff;
    // GREEN
    dest[1] = (rgba >> 16) & 0xff;
    // BLUE
    dest[2] = (rgba >> 8) & 0xff;
    // ALPHA
    dest[3] = (rgba) & 0xff;
    return dest;
  }

  public static int compileRGBA(int r, int g, int b, int a) {
    return ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff);
  }

  public static Color withAlpha(Color color, int alpha) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  public static int[] parseARGB(int rgba) {
    return parseARGB(rgba, new int[4]);
  }

  /**
   * Extract [A, R, G, B] from an integer
   */
  public static int[] parseARGB(int argb, int[] target) {
    target[0] = (argb >> 24) & 0xff;
    target[1] = (argb >> 16) & 0xff;
    target[2] = (argb >> 8) & 0xff;
    target[3] = (argb) & 0xff;
    return target;
  }

  public static int compileRGB(int r, int g, int b) {
    return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
  }

  public static int compileARGB(int a, int r, int g, int b) {
    return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
  }

  public static int[] parseRGB(int rgb) {
    return parseRGB(rgb, new int[3]);
  }

  public static int[] parseRGB(int rgb, int[] dest) {
    // RED
    dest[0] = (rgb >> 16) & 0xff;
    // GREEN
    dest[1] = (rgb >> 8) & 0xff;
    // BLUE
    dest[2] = (rgb) & 0xff;
    return dest;
  }

  /**
   * Convert a 16-bit monochrome to an 8-bit RGB BufferedImage
   */
  public static BufferedImage to8BitRGBBufferedImage(IPoint imageSize, short[] monoPixels) {
    BufferedImage bufferedImage = buildRGBImage(imageSize);
    int[] destPixels = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();
    short[] sourcePixels = monoPixels;
    for (int i = 0; i < sourcePixels.length; i++) {
      short source = sourcePixels[i];
      // We are converting a 16-bit (signed) pixel to an 8-bit (unsigned) one
      int gray = source >> 7;
      destPixels[i] = gray | (gray << 8) | (gray << 16);
    }
    return bufferedImage;
  }

  /**
   * Construct grayscale BufferedImage from monochrome pixels
   */
  @Deprecated // specify whether to extend to 16 bits from 15 bits
  public static BufferedImage to16BitGrayscaleBufferedImage(IPoint imageSize, short[] monoPixels) {
    BufferedImage bufferedImage = build16BitGrayscaleImage(imageSize);
    short[] destPixels = grayPixels(bufferedImage);
    System.arraycopy(monoPixels, 0, destPixels, 0, monoPixels.length);
    return bufferedImage;
  }

  public static short[] grayPixels(BufferedImage grayscaleImage) {
    assertImageType(grayscaleImage, BufferedImage.TYPE_USHORT_GRAY);
    WritableRaster raster = grayscaleImage.getRaster();
    short[] array = ((DataBufferUShort) raster.getDataBuffer()).getData();
    if (array.length != grayscaleImage.getWidth() * grayscaleImage.getHeight())
      throw new IllegalArgumentException(
          "pixel array has unexpected length; is source image a cropped view of another?");
    return array;
  }

  /**
   * Convert an 8-bit grayscale image to an 8-bit rgb one, with (fairly)
   * distinct colors for each shade of gray
   */
  public static BufferedImage grayscaleToRGB(BufferedImage gray8Image) {
    BufferedImage rgb8Image = ImgUtil.buildRGBImage(ImgUtil.size(gray8Image));
    int[] rgbPixels = ImgUtil.rgbPixels(rgb8Image);
    byte[] grayPixels = gray8Pixels(gray8Image);
    int i = -1;
    List<Color> colors = Plotter.rgbColorList();
    for (byte grayLevel : grayPixels) {
      i++;
      rgbPixels[i] = getMod(colors, grayLevel).getRGB();
    }
    return rgb8Image;
  }

  /**
   * Determine what type of image an encoded byte stream represents
   */
  public static String determineTypeOfEncodedImage(byte[] encodedImageBytes) {
    try {
      String formatName = "";
      ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(encodedImageBytes));
      Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
      while (readers.hasNext()) {
        ImageReader read = readers.next();
        formatName = read.getFormatName();
        break;
      }
      return formatName;
    } catch (IOException e) {
      throw Files.asFileException(e);
    }
  }

  // ------------------------------------------------------------------
  // Serializing graphical elements to/from JSON
  // ------------------------------------------------------------------

  public static Font FONT_DEFAULT = new Font("Courier", Font.PLAIN, 16);

  public static Color parseColor(JSMap json) {
    int r = json.getInt("r");
    int g = json.getInt("g");
    int b = json.getInt("b");
    int a = json.opt("a", 255);
    return new Color(r, g, b, a);
  }

  public static Font parseFont(JSMap json) {
    String name = json.opt("name", FONT_DEFAULT.getName());
    int size = json.opt("size", FONT_DEFAULT.getSize());
    int style = json.opt("style", FONT_DEFAULT.getStyle());
    return new Font(name, style, size);
  }

  public static JSMap toJson(Font font) {
    JSMap m = map();
    m.put("name", font.getName());
    m.put("size", font.getSize());
    m.put("style", font.getStyle());
    return m;
  }

  // ------------------------------------------------------------------
  // Logging
  // ------------------------------------------------------------------

  /**
   * Get a string describing a BufferedImage
   */
  public static JSMap toJson(BufferedImage img) {
    return toJson(img, 8, 8, 5);
  }

  public static JSMap toJson(BufferedImage img, int maxWidth, int maxHeight, int prWidth) {
    // TODO: elaborate on support for special printing types, e.g. 'X' for negative 
    JSMap m = mapWithClassName(img);
    m.put("size", new IPoint(img.getWidth(), img.getHeight()).toJson());
    m.put("type", img.getType());
    m.put("color model", toJson(img.getColorModel()));
    String[] pn = img.getPropertyNames();
    if (pn != null) {
      JSMap p = map();
      m.put("properties", p);
      for (int i = 0; i < pn.length; i++)
        p.put(pn[i], img.getProperty(pn[i]).toString());
    }

    if (img.getType() == BufferedImage.TYPE_USHORT_GRAY) {

      boolean special = false;
      String fmt;
      String zer;
      String mx;
      if (prWidth >= 1) {
        fmt = " %" + prWidth + "d";
        zer = " " + spaces(prWidth);
        mx = " !" + spaces(prWidth - 1);
      } else {
        special = true;
        int w = -prWidth;
        fmt = " %" + w + "d";
        zer = " " + spaces(w);
        mx = " !" + spaces(w - 1);
      }

      JSList subImage = list();
      int w = Math.min(maxWidth, img.getWidth());
      int h = Math.min(maxHeight, img.getHeight());
      int x0 = (img.getWidth() - w) / 2;
      int y0 = (img.getHeight() - h) / 2;
      short[] pixels = grayPixels(img);
      for (int y = y0; y < y0 + h; y++) {
        StringBuilder sb = new StringBuilder();
        for (int x = x0; x < x0 + w; x++) {
          short pix = pixels[y * img.getWidth() + x];
          String pixAsString;
          if (special) {
            if (pix >= 0)
              pixAsString = " ";
            else
              pixAsString = "X";
          } else if (pix == 0)
            pixAsString = zer;
          else if (pix == 0x7fff)
            pixAsString = mx;
          else
            pixAsString = String.format(fmt, pix);
          sb.append(pixAsString);
        }
        Tools.tab(sb, 48);
        subImage.add(sb.toString());
      }
      m.put("pix", subImage);
    }

    return m;

  }

  public static JSMap toJson(ColorModel colorModel) {
    JSMap m = map();
    m.put("num_components", colorModel.getNumComponents());
    m.put("pixel_size", colorModel.getPixelSize());
    m.put("alpha:", colorModel.hasAlpha());
    return m;
  }

  public static JSObject toJson(Color color) {
    JSMap m = map();
    m.put("r", color.getRed());
    m.put("g", color.getGreen());
    m.put("b", color.getBlue());
    int alpha = color.getAlpha();
    if (alpha != 255)
      m.put("a", alpha);
    return m;
  }

  public static BufferedImage errorImage() {
    return errorImage(new IPoint(256, 256));
  }

  public static BufferedImage errorImage(IPoint size) {
    return messageImage(size, "No Image Available!");
  }

  public static BufferedImage messageImage(IPoint size, String message) {
    Font BIG_FONT = new Font("Helvetica", Font.BOLD, 24);
    BufferedImage img = buildRGBImage(size);
    Graphics g = img.getGraphics();
    g.setColor(Color.darkGray);
    g.fillRect(0, 0, size.x, size.y);
    g.setColor(Color.red);
    g.setFont(BIG_FONT);
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.charsWidth(message.toCharArray(), 0, message.length());
    g.drawString(message, (size.x - textWidth) / 2, (size.y + fm.getHeight()) / 2);
    g.dispose();
    return img;
  }

  private static BufferedImage register(BufferedImage image) {
    mem().register(image);
    return image;
  }

  static {
    BasePrinter.registerClassHandler(Color.class, (x, p) -> toJson((Color) x).printTo(p));
    BasePrinter.registerClassHandler(BufferedImage.class, (x, p) -> toJson((BufferedImage) x).printTo(p));
    // A byte-wise hash function of image files is unreliable; e.g. the exact contents
    // differ whether unit tests are run within Eclipse vs the command line (Maven).  
    // So, for image files, we look only at a JSMap of some of the image properties
    Files.registerFiletypeHashFn(EXT_JPEG, (f) -> toJson(read(f)));
    Files.registerFiletypeHashFn(EXT_PNG, (f) -> toJson(read(f)));
  }

  public static boolean fontsEqual(Font a, Font b) {
    if (a == b)
      return true;
    if (a == null || b == null)
      return false;
    return a.getSize() == b.getSize() && a.getStyle() == b.getStyle() && a.getName().equals(b.getName());
  }

  public static BufferedImage devSave(BufferedImage img, String suffix) {
    String name = "_dev_" + suffix;
    if (Files.getExtension(name).isEmpty())
      name = Files.setExtension(name, EXT_JPEG);
    File path = Files.getDesktopFile(name);
    pr("...saving image to desktop:", path.getName());
    writeImage(Files.S, img, path);
    return img;
  }

}
