package tech.pegasys.artemis.util.collections;

import javax.annotation.Nullable;
import tech.pegasys.artemis.util.collections.WriteUnion.U2;
import tech.pegasys.artemis.util.collections.WriteUnion.U3;
import tech.pegasys.artemis.util.collections.WriteUnion.U4;
import tech.pegasys.artemis.util.collections.WriteUnion.U5;

public class UnionImpl implements WriteUnion, U2, U3, U4, U5 {

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

  @Override
  public String toString() {
    return "UnionImpl{" +
        "typeIndex=" + typeIndex +
        ", value=" + value +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnionImpl union = (UnionImpl) o;

    if (typeIndex != union.typeIndex) {
      return false;
    }
    return value != null ? value.equals(union.value) : union.value == null;
  }

  @Override
  public int hashCode() {
    int result = typeIndex;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
