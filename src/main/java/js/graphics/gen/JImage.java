package js.graphics.gen;

import java.util.Arrays;
import js.data.AbstractData;
import js.data.DataUtil;
import js.geometry.IPoint;
import js.json.JSMap;

public class JImage implements AbstractData {

  public IPoint size() {
    return mSize;
  }

  public int depth() {
    return mDepth;
  }

  public byte[] bPixels() {
    return mBPixels;
  }

  public short[] wPixels() {
    return mWPixels;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "size";
  protected static final String _1 = "depth";
  protected static final String _2 = "b_pixels";
  protected static final String _3 = "w_pixels";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mSize.toJson());
    m.putUnsafe(_1, mDepth);
    if (mBPixels != null) {
      m.putUnsafe(_2, DataUtil.encodeBase64Maybe(mBPixels));
    }
    if (mWPixels != null) {
      m.putUnsafe(_3, DataUtil.encodeBase64Maybe(mWPixels));
    }
    return m;
  }

  @Override
  public JImage build() {
    return this;
  }

  @Override
  public JImage parse(Object obj) {
    return new JImage((JSMap) obj);
  }

  private JImage(JSMap m) {
    {
      mSize = IPoint.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_0);
      if (x != null) {
        mSize = IPoint.DEFAULT_INSTANCE.parse(x);
      }
    }
    mDepth = m.opt(_1, 0);
    {
      Object x = m.optUnsafe(_2);
      if (x != null) {
        mBPixels = DataUtil.parseBytesFromArrayOrBase64(x);
      }
    }
    {
      Object x = m.optUnsafe(_3);
      if (x != null) {
        mWPixels = DataUtil.parseShortsFromArrayOrBase64(x);
      }
    }
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof JImage))
      return false;
    JImage other = (JImage) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mSize.equals(other.mSize)))
      return false;
    if (!(mDepth == other.mDepth))
      return false;
    if ((mBPixels == null) ^ (other.mBPixels == null))
      return false;
    if (mBPixels != null) {
      if (!(Arrays.equals(mBPixels, other.mBPixels)))
        return false;
    }
    if ((mWPixels == null) ^ (other.mWPixels == null))
      return false;
    if (mWPixels != null) {
      if (!(Arrays.equals(mWPixels, other.mWPixels)))
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mSize.hashCode();
      r = r * 37 + mDepth;
      if (mBPixels != null) {
        r = r * 37 + Arrays.hashCode(mBPixels);
      }
      if (mWPixels != null) {
        r = r * 37 + Arrays.hashCode(mWPixels);
      }
      m__hashcode = r;
    }
    return r;
  }

  protected IPoint mSize;
  protected int mDepth;
  protected byte[] mBPixels;
  protected short[] mWPixels;
  protected int m__hashcode;

  public static final class Builder extends JImage {

    private Builder(JImage m) {
      mSize = m.mSize;
      mDepth = m.mDepth;
      mBPixels = m.mBPixels;
      mWPixels = m.mWPixels;
    }

    @Override
    public Builder toBuilder() {
      return this;
    }

    @Override
    public int hashCode() {
      m__hashcode = 0;
      return super.hashCode();
    }

    @Override
    public JImage build() {
      JImage r = new JImage();
      r.mSize = mSize;
      r.mDepth = mDepth;
      r.mBPixels = mBPixels;
      r.mWPixels = mWPixels;
      return r;
    }

    public Builder size(IPoint x) {
      mSize = (x == null) ? IPoint.DEFAULT_INSTANCE : x.build();
      return this;
    }

    public Builder depth(int x) {
      mDepth = x;
      return this;
    }

    public Builder bPixels(byte[] x) {
      mBPixels = (x == null) ? null : x;
      return this;
    }

    public Builder wPixels(short[] x) {
      mWPixels = (x == null) ? null : x;
      return this;
    }

  }

  public static final JImage DEFAULT_INSTANCE = new JImage();

  private JImage() {
    mSize = IPoint.DEFAULT_INSTANCE;
  }

}
