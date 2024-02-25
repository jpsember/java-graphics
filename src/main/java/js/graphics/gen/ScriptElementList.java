package js.graphics.gen;

import java.util.List;
import js.data.AbstractData;
import js.data.DataUtil;
import js.graphics.ScriptElement;
import js.json.JSList;
import js.json.JSMap;

public class ScriptElementList implements AbstractData {

  public List<ScriptElement> elements() {
    return mElements;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  protected static final String _0 = "elements";

  @Override
  public String toString() {
    return toJson().prettyPrint();
  }

  @Override
  public JSMap toJson() {
    JSMap m = new JSMap();
    {
      JSList j = new JSList();
      for (ScriptElement x : mElements)
        j.add(x.toJson());
      m.put(_0, j);
    }
    return m;
  }

  @Override
  public ScriptElementList build() {
    return this;
  }

  @Override
  public ScriptElementList parse(Object obj) {
    return new ScriptElementList((JSMap) obj);
  }

  private ScriptElementList(JSMap m) {
    mElements = DataUtil.parseListOfObjects(ScriptElement.DEFAULT_INSTANCE, m.optJSList(_0), false);
  }

  public static Builder newBuilder() {
    return new Builder(DEFAULT_INSTANCE);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object)
      return true;
    if (object == null || !(object instanceof ScriptElementList))
      return false;
    ScriptElementList other = (ScriptElementList) object;
    if (other.hashCode() != hashCode())
      return false;
    if (!(mElements.equals(other.mElements)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int r = m__hashcode;
    if (r == 0) {
      r = 1;
      for (ScriptElement x : mElements)
        if (x != null)
          r = r * 37 + x.hashCode();
      m__hashcode = r;
    }
    return r;
  }

  protected List<ScriptElement> mElements;
  protected int m__hashcode;

  public static final class Builder extends ScriptElementList {

    private Builder(ScriptElementList m) {
      mElements = DataUtil.immutableCopyOf(m.mElements) /*DEBUG*/ ;
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
    public ScriptElementList build() {
      ScriptElementList r = new ScriptElementList();
      r.mElements = mElements;
      return r;
    }

    public Builder elements(List<ScriptElement> x) {
      mElements = DataUtil.immutableCopyOf((x == null) ? DataUtil.emptyList() : x) /*DEBUG*/ ;
      return this;
    }

  }

  public static final ScriptElementList DEFAULT_INSTANCE = new ScriptElementList();

  private ScriptElementList() {
    mElements = DataUtil.emptyList();
  }

}
