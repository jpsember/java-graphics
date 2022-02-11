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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import js.data.AbstractData;

/**
 * Encapsulates rendering properties; implements AbstractData interface (builder
 * etc). Doesn't provide 'proper' hashcode/equals
 */
public class Paint implements AbstractData {

  public static final Paint DEFAULT_INSTANCE = new Paint();

  @Override
  public Paint build() {
    return this;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  public final float width() {
    return mStrokeWidth;
  }

  public final Font font() {
    return mFont;
  }

  public final Paint apply(Graphics2D g) {
    if (!isFill()) {
      g.setStroke(new BasicStroke(mStrokeWidth));
    }
    if (mColor != null)
      g.setColor(mColor);
    if (mFont != null)
      g.setFont(mFont);
    return this;
  }

  /**
   * For debug purposes only
   */
  public Color color() {
    return mColor;
  }

  public final boolean isFill() {
    return mStrokeWidth == 0;
  }

  private Paint() {
  }

  protected float mStrokeWidth;
  protected Color mColor = Color.BLUE;
  protected Font mFont;

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

  public static final Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public Paint parse(Object object) {
    throw new UnsupportedOperationException();
  }

  public static final class Builder extends Paint {

    public Builder font(Font font, float scaleFactor) {
      if (scaleFactor != 1.0f) {
        font = font.deriveFont(scaleFactor * font.getSize2D());
      }
      mFont = font;
      return this;
    }

    public Builder color(Color color) {
      mColor = color;
      return this;
    }

    /**
     * Make paint operate in STROKE mode, with a particular width.
     * 
     * If width is zero, paint operates in FILL mode.
     */
    public Builder width(float width) {
      mStrokeWidth = width;
      return this;
    }

    @Override
    public Paint build() {
      Paint x = new Paint();
      x.mColor = mColor;
      x.mFont = mFont;
      x.mStrokeWidth = mStrokeWidth;
      return x;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    private Builder(Paint x) {
      mColor = x.mColor;
      mFont = x.mFont;
      mStrokeWidth = x.mStrokeWidth;
    }

    public Builder fill() {
      width(0);
      return this;
    }

    /**
     * Select default (proportionally spaced) font, scaled from default size
     */
    public Builder font(float scale) {
      return font(LABEL_FONT, scale);
    }

    public Builder bigFont(float scale) {
      return font(BIG_FONT, scale);
    }

    /**
     * Select monospaced font, with default size
     */
    public Builder monoFont() {
      return monoFont(1);
    }

    /**
     * Select monospaced font, scaled from default size
     */
    public Builder monoFont(float scale) {
      return font(CONSOLE_FONT, scale);
    }

    public Builder color(int red, int green, int blue) {
      return color(new Color(red, green, blue));
    }

    public Builder color(int red, int green, int blue, int alpha) {
      return color(new Color(red, green, blue, alpha));
    }

    public Builder color(float red, float green, float blue, float alpha) {
      return color((int) red, (int) green, (int) blue, (int) alpha);
    }

    public Builder color(float red, float green, float blue) {
      return color((int) red, (int) green, (int) blue, 255);
    }

    public Builder alpha(float alpha) {
      return color(mColor.getRed(), mColor.getGreen(), mColor.getBlue(), (int) alpha);
    }

  }

}
