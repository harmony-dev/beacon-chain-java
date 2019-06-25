package tech.pegasys.artemis.util.collections;

import javax.annotation.Nullable;

public class UnionImpl implements WriteUnion {

  private int typeIndex = 0;
  private Object value;

  @Override
  public <C> void setValue(int index, @Nullable C value) {
    typeIndex = index;
    this.value = value;
  }

  @Override
  public int getTypeIndex() {
    return typeIndex;
  }

  @Nullable
  @Override
  public <C> C getValue() {
    return (C) value;
  }
}
