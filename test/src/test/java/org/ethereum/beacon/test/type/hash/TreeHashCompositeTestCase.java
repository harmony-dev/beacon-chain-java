package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;
import java.util.Map;

/**
 * Tree hash test case
 */
public class TreeHashCompositeTestCase implements TestCase {
  private Object type;
  private String root;
  private List<String> tags;
  private Object value;

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

  public Object getType() {
    return type;
  }

  public void setType(Object type) {
    this.type = type;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }
}
