package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;
import java.util.Map;

/** Tree hash test case with container */
public class TreeHashContainerTestCase implements TestCase {
  private Map<String, String> type;
  private String root;
  private List<String> tags;
  private Map<String, String> value;

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public Map<String, String> getType() {
    return type;
  }

  public void setType(Map<String, String> type) {
    this.type = type;
  }

  public Map<String, String> getValue() {
    return value;
  }

  public void setValue(Map<String, String> value) {
    this.value = value;
  }
}
