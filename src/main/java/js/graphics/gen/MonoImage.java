package js.graphics.gen;

import java.util.Arrays;
import js.data.AbstractData;
import js.data.DataUtil;
import js.geometry.IPoint;
import js.json.JSMap;

public class MonoImage implements AbstractData {

  public IPoint offset() {
    return mOffset;
  }

  public IPoint size() {
    return mSize;
  }

  public short[] pixels() {
    return mPixels;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "offset";
  protected static final String _1 = "size";
  protected static final String _2 = "pixels";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mOffset.toJson());
    m.putUnsafe(_1, mSize.toJson());
    m.putUnsafe(_2, DataUtil.encodeBase64Maybe(mPixels));
    return m;
  }

  @Override
  public MonoImage build() {
    return this;
  }

  @Override
  public MonoImage parse(Object obj) {
    return new MonoImage((JSMap) obj);
  }

  private MonoImage(JSMap m) {
    {
      mOffset = IPoint.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_0);
      if (x != null) {
        mOffset = IPoint.DEFAULT_INSTANCE.parse(x);
      }
    }
    {
      mSize = IPoint.DEFAULT_INSTANCE;
      Object x = m.optUnsafe(_1);
      if (x != null) {
        mSize = IPoint.DEFAULT_INSTANCE.parse(x);
      }
    }
    {
      mPixels = DataUtil.EMPTY_SHORT_ARRAY;
      Object x = m.optUnsafe(_2);
      if (x != null) {
        mPixels = DataUtil.parseShortsFromArrayOrBase64(x);
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
    if (object == null || !(object instanceof MonoImage))
      return false;
    MonoImage other = (MonoImage) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mOffset.equals(other.mOffset)))
      return false;
    if (!(mSize.equals(other.mSize)))
      return false;
    if (!(Arrays.equals(mPixels, other.mPixels)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mOffset.hashCode();
      r = r * 37 + mSize.hashCode();
      r = r * 37 + Arrays.hashCode(mPixels);
      m__hashcode = r;
    }
    return r;
  }

  protected IPoint mOffset;
  protected IPoint mSize;
  protected short[] mPixels;
  protected int m__hashcode;

  public static final class Builder extends MonoImage {

    private Builder(MonoImage m) {
      mOffset = m.mOffset;
      mSize = m.mSize;
      mPixels = m.mPixels;
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
    public MonoImage build() {
      MonoImage r = new MonoImage();
      r.mOffset = mOffset;
      r.mSize = mSize;
      r.mPixels = mPixels;
      return r;
    }

    public Builder offset(IPoint x) {
      mOffset = (x == null) ? IPoint.DEFAULT_INSTANCE : x.build();
      return this;
    }

    public Builder size(IPoint x) {
      mSize = (x == null) ? IPoint.DEFAULT_INSTANCE : x.build();
      return this;
    }

    public Builder pixels(short[] x) {
      mPixels = (x == null) ? DataUtil.EMPTY_SHORT_ARRAY : x;
      return this;
    }

  }

  public static final MonoImage DEFAULT_INSTANCE = new MonoImage();

  private MonoImage() {
    mOffset = IPoint.DEFAULT_INSTANCE;
    mSize = IPoint.DEFAULT_INSTANCE;
    mPixels = DataUtil.EMPTY_SHORT_ARRAY;
  }

}
