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

import js.geometry.IPoint;
import js.geometry.IRect;
import js.json.JSMap;
import js.testutil.MyTestCase;
import static js.base.Tools.*;

public class ImageFitTest extends MyTestCase {

  @Test
  public void crop() {
    mPadVsCropBias = 1;
    perform();
  }

  @Test
  public void crop2() {
    mPadVsCropBias = 1;
    switchAspect();
    perform();
  }

  @Test
  public void letterBox() {
    mPadVsCropBias = 0;
    perform();
  }

  @Test
  public void letterBox2() {
    mPadVsCropBias = 0;
    switchAspect();
    perform();
  }

  @Test
  public void hybrid() {
    mPadVsCropBias = .5f;
    perform();
  }

  @Test
  public void hybrid2() {
    mPadVsCropBias = .5f;
    switchAspect();
    perform();
  }

  private JSMap performAux() {
    IPoint targetSize = new IPoint(1024, 600);
    JSMap m = map();
    m.put("source_size", mSrcSize);
    IRect tf = IRect.FitRectToRect(mSrcSize, targetSize, mPadVsCropBias, 0, 0);
    m.put("~target_rect",tf);
    int xErr = targetSize.x - tf.width;
    int yErr = targetSize.y - tf.height;
    m.put("~~x err", xErr);
    m.put("~~y err", yErr);
    m.put("~~~ err", xErr * xErr + yErr * yErr);
    return m;
  }

  private void switchAspect() {
    mSrcSize = mSrcSize.withY(140);
  }

  private void perform() {
    loadTools();
    JSMap m = performAux();
    log(m);
    assertMessage(m);
  }

  private float mPadVsCropBias;
  private IPoint mSrcSize = new IPoint(320, 256);
}
