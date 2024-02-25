package js.graphics.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class CompressParam implements AbstractData {

  public int golomb() {
    return mGolomb;
  }

  public float padding() {
    return mPadding;
  }

  public float ratio() {
    return mRatio;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "golomb";
  protected static final String _1 = "padding";
  protected static final String _2 = "ratio";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    m.putUnsafe(_0, mGolomb);
    m.putUnsafe(_1, mPadding);
    m.putUnsafe(_2, mRatio);
    return m;
  }

  @Override
  public CompressParam build() {
    return this;
  }

  @Override
  public CompressParam parse(Object obj) {
    return new CompressParam((JSMap) obj);
  }

  private CompressParam(JSMap m) {
    mGolomb = m.opt(_0, 180);
    mPadding = m.opt(_1, 0.25f);
    mRatio = m.opt(_2, 0f);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof CompressParam))
      return false;
    CompressParam other = (CompressParam) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mGolomb == other.mGolomb))
      return false;
    if (!(mPadding == other.mPadding))
      return false;
    if (!(mRatio == other.mRatio))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      r = r * 37 + mGolomb;
      r = r * 37 + (int)mPadding;
      r = r * 37 + (int)mRatio;
      m__hashcode = r;
    }
    return r;
  }

  protected int mGolomb;
  protected float mPadding;
  protected float mRatio;
  protected int m__hashcode;

  public static final class Builder extends CompressParam {

    private Builder(CompressParam m) {
      mGolomb = m.mGolomb;
      mPadding = m.mPadding;
      mRatio = m.mRatio;
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
    public CompressParam build() {
      CompressParam r = new CompressParam();
      r.mGolomb = mGolomb;
      r.mPadding = mPadding;
      r.mRatio = mRatio;
      return r;
    }

    public Builder golomb(int x) {
      mGolomb = x;
      return this;
    }

    public Builder padding(float x) {
      mPadding = x;
      return this;
    }

    public Builder ratio(float x) {
      mRatio = x;
      return this;
    }

  }

  public static final CompressParam DEFAULT_INSTANCE = new CompressParam();

  private CompressParam() {
    mGolomb = 180;
    mPadding = 0.25f;
  }

}
