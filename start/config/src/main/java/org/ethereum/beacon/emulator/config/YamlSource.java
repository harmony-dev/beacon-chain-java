package org.ethereum.beacon.emulator.config;

/** Source of config from yaml data presented as text */
public class YamlSource implements ConfigSource {
  final Type type = Type.YAML;
  final String data;

  public YamlSource(String data) {
    this.data = data;
  }

  @Override
  public Type getType() {
    return type;
  }

  public String getData() {
    return data;
  }

  @Override
  public String toString() {
    return "YamlSource{" + "data=" + data + '}';
  }
}
