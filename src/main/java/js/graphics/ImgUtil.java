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
import js.json.JSList;
import js.json.JSMap;
import js.json.JSObject;

public final class ImgUtil {

  // ------------------------------------------------------------------
  // Image file extensions
  // ------------------------------------------------------------------

  public static final String JPEG_EXT = "jpg";
  public static final String PNG_EXT = "png";
  public static final String JMG_EXT = "jmg";
  public static final String RAX_EXT = "rax";

  public static final List<String> IMAGE_EXTENSIONS = arrayList(JPEG_EXT, PNG_EXT, "jpeg", RAX_EXT);
  public static final int PREFERRED_IMAGE_TYPE_COLOR = BufferedImage.TYPE_INT_RGB;

  // ------------------------------------------------------------------
  // Reading images
  // ------------------------------------------------------------------

  public static BufferedImage read(File src) {
    // If file is a custom format, treat appropriately
    String ext = Files.getExtension(src);
    if (ext.equals(RAX_EXT)) {
      return readRax(Files.openInputStream(src));
    }
    return read(Files.openInputStream(src));
  }

  public static BufferedImage read(byte[] bytes) {
    ByteArrayInputStream input = new ByteArrayInputStream(bytes);
    return read(input);
  }

  public static BufferedImage read(InputStream inputStream) {
    try {
      BufferedImage img = ImageIO.read(inputStream);
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
  private static final int RAX_COMPRESS_FLAG = 0xfd;
  private static final int RAX_VERSION_1 = 1;

  /**
   * Read .rax from input stream
   * 
   * @param dimensions
   *          this must be an array to hold a single IPoint; the dimensions are
   *          returned here
   * @return pixels
   */
  public static short[] readRax(InputStream inputStream, IPoint[] dimensions) {
    byte[] content = Files.toByteArray(inputStream);
    short[] pixels = decompressRAX(content, dimensions, null);
    return pixels;
  }

  public static BufferedImage readRax(InputStream inputStream) {
    try {
      IPoint[] receivedSize = new IPoint[1];
      short[] pixels = readRax(inputStream, receivedSize);
      IPoint size = receivedSize[0];
      return toBufferedImage(pixels, size);
    } catch (Throwable t) {
      throw Files.asFileException(t);
    } finally {
      Files.closePeacefully(inputStream);
    }
  }

  public static BufferedImage toBufferedImage(short[] pixels, IPoint size) {
    BufferedImage bufferedImage = buildGrayscaleImage(size);
    short[] destPixels = grayPixels(bufferedImage);
    System.arraycopy(pixels, 0, destPixels, 0, destPixels.length);
    return bufferedImage;
  }

  /**
   * Decompress RAX pixels
   */
  public static short[] decompressRAX(byte[] byteBuffer, IPoint[] outputSizeOrNull,
      short[] outputPixelsOrNull) {
    IPoint imageSize = looksLikeCompressedRawImage(byteBuffer);
    if (imageSize == null)
      throw new IllegalArgumentException("does not look like a compressed RawImage");
    byte version = byteBuffer[1];
    checkArgument(version == RAX_VERSION_1, "unexpected version: " + version);

    ByteArrayInputStream input = new ByteArrayInputStream(byteBuffer, RAX_COMPRESS_HEADER_LENGTH,
        byteBuffer.length - RAX_COMPRESS_HEADER_LENGTH);

    if (outputSizeOrNull != null)
      outputSizeOrNull[0] = imageSize;
    int imageWidth = imageSize.x;
    int imageHeight = imageSize.y;

    int expectedLength = imageWidth * imageHeight;
    short[] outputPixels = DataUtil.shortArray(expectedLength, outputPixelsOrNull);

    int rowOffset = 0;
    for (int rowNumber = 0; rowNumber < imageHeight; rowNumber++, rowOffset += imageWidth) {
      int rowOffsetM2 = rowOffset - 2 * imageWidth;
      int rowOffsetM1 = rowOffset - imageWidth;
      int prevH1, prevH2;

      if (rowNumber == 0) {
        prevH1 = 20000;
        prevH2 = 20000;
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
    return outputPixels;
  }

  private static int extractIntegerFromStream(ByteArrayInputStream stream, int prediction) {
    final byte JUMP_SIGNAL = -128;
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

  private static int v1Scale(int value) {
    // Get same effect as multiplying by 0.280f, but without using floating point
    return (value * 7) / 25;
  }

  /**
   * Determine if an array of bytes looks like a compressed RawImage. If so,
   * returns the presumed dimensions of the image; otherwise, null
   */
  public static IPoint looksLikeCompressedRawImage(byte[] byteBuffer) {
    IPoint result = null;
    do {
      if (byteBuffer.length < RAX_COMPRESS_HEADER_LENGTH)
        break;
      if (byteBuffer[0] != (byte) RAX_COMPRESS_FLAG)
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
        boolean wrote = ImageIO.write(img, JPEG_EXT, outStream);
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
      ImageIO.write(img, PNG_EXT, outStream);
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
    if (ext.equals(JPEG_EXT)) {
      int type = img.getType();
      if (type != BufferedImage.TYPE_3BYTE_BGR && type != BufferedImage.TYPE_INT_RGB)
        badArg("image has unexpected type:", type);
      bytes = toJPEG(img, null);
    } else if (ext.equals(PNG_EXT))
      bytes = toPNG(img);
    checkState(bytes != null, "Unsupported image type:", dest);
    files.write(toPNG(img), dest);
  }

  public static void writeJPG(Files files, BufferedImage img, File dest, Integer requiredTypeOrNull) {
    checkArgument(Files.getExtension(dest).equals(JPEG_EXT), "unsupported extension:", dest);
    files.write(toJPEG(img, requiredTypeOrNull), dest);
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
    return new BufferedImage(size.x, size.y, bufferedImageType);
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

  @Deprecated // Use build16BitGrayscaleImage
  public static BufferedImage buildGrayscaleImage(IPoint size) {
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
    return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
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
    return DataUtil.assertLength(array, image.getWidth() * image.getHeight(),
        "rgbPixels; did cropping occur?");
  }

  public static int[] argbPixels(BufferedImage bufferedImage_INT_ARGB) {
    BufferedImage image = bufferedImage_INT_ARGB;
    assertImageType(image, BufferedImage.TYPE_INT_ARGB);
    int[] array = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    return DataUtil.assertLength(array, image.getWidth() * image.getHeight(),
        "argbPixels; did cropping occur?");
  }

  /**
   * Convert image pixels to array of float; only some image types are supported
   * 
   * If numChannels is 1, and image is color, it uses only the green channel
   */
  public static float[] floatPixels(BufferedImage sourceImage, int numChannels, float[] destinationOrNull) {

    int[] sourcePixels;

    switch (sourceImage.getType()) {

    default:
      throw badArg("unsupported image type:", INDENT, sourceImage);

    case BufferedImage.TYPE_INT_RGB:
      sourcePixels = ImgUtil.rgbPixels(sourceImage);
      break;
    }

    float[] result = DataUtil.floatArray(sourcePixels.length * numChannels, destinationOrNull);

    switch (sourceImage.getType()) {

    default:
      throw badArg("unsupported image type:", INDENT, sourceImage);

    case BufferedImage.TYPE_INT_RGB: {
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
    }

    return result;
  }

  // ------------------------------------------------------------------
  // Construct BufferedImage from pixels
  // ------------------------------------------------------------------

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
  public static BufferedImage to16BitGrayscaleBufferedImage(IPoint imageSize, short[] monoPixels) {
    BufferedImage bufferedImage = buildGrayscaleImage(imageSize);
    short[] destPixels = grayPixels(bufferedImage);
    System.arraycopy(monoPixels, 0, destPixels, 0, monoPixels.length);
    return bufferedImage;
  }

  public static short[] grayPixels(BufferedImage grayscaleImage) {
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
    m.put("a", color.getAlpha());
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

  static {
    BasePrinter.registerClassHandler(Color.class, (x, p) -> toJson((Color) x).printTo(p));
    BasePrinter.registerClassHandler(BufferedImage.class, (x, p) -> toJson((BufferedImage) x).printTo(p));
    // A byte-wise hash function of image files is unreliable; e.g. the exact contents
    // differ whether unit tests are run within Eclipse vs the command line (Maven).  
    // So, for image files, we look only at a JSMap of some of the image properties
    Files.registerFiletypeHashFn(JPEG_EXT, (f) -> toJson(read(f)));
    Files.registerFiletypeHashFn(PNG_EXT, (f) -> toJson(read(f)));
  }
}
