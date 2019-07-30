package org.ethereum.beacon.ssz.visitor;

import java.util.function.BiFunction;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.list.SSZListType;
import org.ethereum.beacon.ssz.type.SSZUnionType;

/**
 * Abstract Visitor interface for modified Visitor Pattern
 * This pattern modification leaves the children visiting under the Visitor control
 * by passing {@link ChildVisitor} instance for composite values
 */
public interface SSZVisitor<ResultType, ParamType> {

  /**
   *  The interface for visitor control on its children visiting
   */
  interface ChildVisitor<ParamType, ResultType>
      extends BiFunction<Integer, ParamType, ResultType> {}

  /**
   * Invoked on the SSZ basic value in the type hierarchy
   */
  ResultType visitBasicValue(SSZBasicType sszType, ParamType param);

  /**
   * Invoked on the SSZ List in the type hierarchy
   *
   * NOTE: you should either implement {@link #visitComposite(SSZCompositeType, Object, ChildVisitor)}
   * method or  both {@link #visitList(SSZListType, Object, ChildVisitor)} and
   * {@link #visitContainer(SSZContainerType, Object, ChildVisitor)} method
   */
  default ResultType visitList(
      SSZListType type, ParamType param, ChildVisitor<ParamType, ResultType> childVisitor) {
    return visitComposite(type, param, childVisitor);
  }

  default ResultType visitSubList(
      SSZListType type,
      ParamType param,
      int startIdx,
      int len,
      ChildVisitor<ParamType, ResultType> childVisitor) {
    throw new UnsupportedOperationException();
  }

    /**
     * Invoked on the SSZ Container in the type hierarchy
     *
     * NOTE: you should either implement {@link #visitComposite(SSZCompositeType, Object, ChildVisitor)}
     * method or this method
     */
  default ResultType visitContainer(
      SSZContainerType type, ParamType param, ChildVisitor<ParamType, ResultType> childVisitor) {
    return visitComposite(type, param, childVisitor);
  }

  /**
   * Invoked on the SSZ Union in the type hierarchy
   *
   * NOTE: you should either implement {@link #visitComposite(SSZCompositeType, Object, ChildVisitor)}
   * method or this method
   */
  default ResultType visitUnion(
      SSZUnionType type, ParamType param, ChildVisitor<ParamType, ResultType> childVisitor) {
    return visitComposite(type, param, childVisitor);
  }

  /**
   * Invoked on either SSZ Container or SSZ List in the type hierarchy
   *
   * NOTE: you should either implement {@link #visitComposite(SSZCompositeType, Object, ChildVisitor)}
   * or all methods delegating to it by default
   */
  default ResultType visitComposite(
      SSZCompositeType type, ParamType param, ChildVisitor<ParamType, ResultType> childVisitor) {
    throw new UnsupportedOperationException(
        "You should either implement visitComposite() or visitList() + visitContainer()");
  }
}
