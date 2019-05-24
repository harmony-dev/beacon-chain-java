package org.ethereum.beacon.test.type.ssz;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/**
 * SSZ test case
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszGenericCase implements TestCase {
  private String type;
  private String valid;
  private String ssz;
  private List<String> tags;
  private String value;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getValid() {
    return valid;
  }

  public void setValid(String valid) {
    this.valid = valid;
  }

  public String getSsz() {
    return ssz;
  }

  public void setSsz(String ssz) {
    this.ssz = ssz;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
