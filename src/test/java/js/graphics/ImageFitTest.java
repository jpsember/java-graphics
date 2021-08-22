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

import org.junit.Test;

import js.geometry.FRect;
import js.geometry.IPoint;
import js.geometry.IRect;
import js.graphics.gen.ImageFitOptions;
import js.graphics.gen.ImageFitType;
import js.json.JSMap;
import js.testutil.MyTestCase;
import static js.base.Tools.*;

public class ImageFitTest extends MyTestCase {

  @Test
  public void crop() {
    f.fitType(ImageFitType.CROP);
    perform();
  }

  @Test
  public void crop2() {
    f.fitType(ImageFitType.CROP);
    switchAspect();
    perform();
  }

  @Test
  public void letterBox() {
    f.fitType(ImageFitType.LETTERBOX);
    perform();
  }

  @Test
  public void letterBox2() {
    f.fitType(ImageFitType.LETTERBOX);
    switchAspect();
    perform();
  }

  @Test
  public void hybrid() {
    f.fitType(ImageFitType.HYBRID);
    perform();
  }

  @Test
  public void hybrid2() {
    f.fitType(ImageFitType.HYBRID);
    switchAspect();
    perform();
  }

  private JSMap performAux() {
    ImageFit imageFit = new ImageFit(f.build(), srcSize);
    JSMap m = f.toJson();
    m.put("source_size", srcSize);
    IRect tr = imageFit.transformedSourceRect();
    m.put("~target_rect", tr);
    m.put("~crop_nec", imageFit.cropNecessary());
    m.put("~matrix", imageFit.matrix());
    FRect tf = imageFit.transformedSourceRectF();
    float xErr = f.targetSize().x - tf.width;
    float yErr = f.targetSize().y - tf.height;
    m.put("~~x err", xErr);
    m.put("~~y err", yErr);
    m.put("~~~ err", xErr * xErr + yErr * yErr);
    return m;
  }

  private void switchAspect() {
    srcSize = srcSize.withY(140);
  }

  private void perform() {
    loadTools();
    assertMessage(performAux());
  }

  private ImageFitOptions.Builder f = ImageFitOptions.newBuilder().targetSize(new IPoint(1024, 600));
  private IPoint srcSize = new IPoint(320, 256);
}
