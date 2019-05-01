package org.ethereum.beacon.test.type.ssz;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.test.type.TestCase;

/**
 * Ssz static tests with known containers.
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md</a>
 */
public class SszStaticCase implements TestCase {
  @JsonProperty("type_name")
  private String typeName;

  private Object value;
  private String serialized;
  private String root;

  @JsonProperty("signing_root")
  private String signingRoot;

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public String getSerialized() {
    return serialized;
  }

  public void setSerialized(String serialized) {
    this.serialized = serialized;
  }

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public String getSigningRoot() {
    return signingRoot;
  }

  public void setSigningRoot(String signingRoot) {
    this.signingRoot = signingRoot;
  }
}
