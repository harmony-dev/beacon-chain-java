package org.ethereum.beacon.test.type.ssz;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.ethereum.beacon.test.type.TestCase;

import java.util.Map;

/**
 * Ssz static tests with known containers.
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md</a>
 */
public class SszStaticCase implements TestCase {
  private String typeName;

  private Object value;
  private String serialized;
  private String root;
  private String signingRoot;

  @JsonCreator
  public SszStaticCase(Map<String, Object> map) {
    this.typeName = (String) map.keySet().toArray()[0];
    Map<String, Object> obj = (Map<String, Object>) map.get(typeName);
    this.value = obj.get("value");
    this.serialized = (String) obj.get("serialized");
    this.root = (String) obj.get("root");
    if (obj.containsKey("signing_root")) {
      this.signingRoot = (String) obj.get("signing_root");
    }
  }

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
