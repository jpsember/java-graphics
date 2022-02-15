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

import java.awt.Font;
import java.awt.image.BufferedImage;

import js.base.BasePrinter;
import js.json.JSMap;
import js.testutil.MyTestCase;

import org.junit.Test;

import static js.base.Tools.*;

public class ImgUtilTest extends MyTestCase {

  @Test
  public void readPNG() {
    assertMessage(ImgUtil.toJson(readImage("batman.png")));
  }

  @Test
  public void mono16bit() {
    BufferedImage img = readImage("mono16bit.png");
    assertMessage(ImgUtil.toJson(img));
  }

  @Test
  public void imageAsType() {
    BufferedImage img = readImage("batman.png");
    img = ImgUtil.imageAsType(img, BufferedImage.TYPE_INT_ARGB);
    assertMessage(ImgUtil.toJson(img));
  }

  @Test
  public void printImageToBuffer() {
    BufferedImage img = readImage("batman.png");
    assertMessage(BasePrinter.toString(img));
  }

  @Test
  public void fontSerialization() {
    JSMap m = ImgUtil.toJson(ImgUtil.FONT_DEFAULT);
    Font f = ImgUtil.parseFont(m);
    JSMap m2 = map();
    m2.put("orig",m);
    m2.put("parsed",ImgUtil.toJson(f));
    assertMessage(m2);
  }
  
  private BufferedImage readImage(String name) {
    return ImgUtil.read(testFile(name));
  }

}
