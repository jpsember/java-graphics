package js.graphics.gen;

import js.data.AbstractData;
import js.json.JSMap;

public class ElementProperties implements AbstractData {

  public Integer rotation() {
    return mRotation;
  }

  public Integer confidence() {
    return mConfidence;
  }

  public Integer category() {
    return mCategory;
  }

  public Integer anchor() {
    return mAnchor;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "rotation";
  protected static final String _1 = "confidence";
  protected static final String _2 = "category";
  protected static final String _3 = "anchor";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    if (mRotation != null) {
      m.putUnsafe(_0, mRotation);
    }
    if (mConfidence != null) {
      m.putUnsafe(_1, mConfidence);
    }
    if (mCategory != null) {
      m.putUnsafe(_2, mCategory);
    }
    if (mAnchor != null) {
      m.putUnsafe(_3, mAnchor);
    }
    return m;
  }

  @Override
  public ElementProperties build() {
    return this;
  }

  @Override
  public ElementProperties parse(Object obj) {
    return new ElementProperties((JSMap) obj);
  }

  private ElementProperties(JSMap m) {
    mRotation = m.optInt(_0);
    mConfidence = m.optInt(_1);
    mCategory = m.optInt(_2);
    mAnchor = m.optInt(_3);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ElementProperties))
      return false;
    ElementProperties other = (ElementProperties) object;
    if (other.hashCode() != hashCode())
      return false;
    if ((mRotation == null) ^ (other.mRotation == null))
      return false;
    if (mRotation != null) {
      if (!(mRotation.equals(other.mRotation)))
        return false;
    }
    if ((mConfidence == null) ^ (other.mConfidence == null))
      return false;
    if (mConfidence != null) {
      if (!(mConfidence.equals(other.mConfidence)))
        return false;
    }
    if ((mCategory == null) ^ (other.mCategory == null))
      return false;
    if (mCategory != null) {
      if (!(mCategory.equals(other.mCategory)))
        return false;
    }
    if ((mAnchor == null) ^ (other.mAnchor == null))
      return false;
    if (mAnchor != null) {
      if (!(mAnchor.equals(other.mAnchor)))
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      if (mRotation != null) {
        r = r * 37 + mRotation;
      }
      if (mConfidence != null) {
        r = r * 37 + mConfidence;
      }
      if (mCategory != null) {
        r = r * 37 + mCategory;
      }
      if (mAnchor != null) {
        r = r * 37 + mAnchor;
      }
      m__hashcode = r;
    }
    return r;
  }

  protected Integer mRotation;
  protected Integer mConfidence;
  protected Integer mCategory;
  protected Integer mAnchor;
  protected int m__hashcode;

  public static final class Builder extends ElementProperties {

    private Builder(ElementProperties m) {
      mRotation = m.mRotation;
      mConfidence = m.mConfidence;
      mCategory = m.mCategory;
      mAnchor = m.mAnchor;
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
    public ElementProperties build() {
      ElementProperties r = new ElementProperties();
      r.mRotation = mRotation;
      r.mConfidence = mConfidence;
      r.mCategory = mCategory;
      r.mAnchor = mAnchor;
      return r;
    }

    public Builder rotation(Integer x) {
      mRotation = x;
      return this;
    }

    public Builder confidence(Integer x) {
      mConfidence = x;
      return this;
    }

    public Builder category(Integer x) {
      mCategory = x;
      return this;
    }

    public Builder anchor(Integer x) {
      mAnchor = x;
      return this;
    }

  }

  public static final ElementProperties DEFAULT_INSTANCE = new ElementProperties();

  private ElementProperties() {
  }

}
