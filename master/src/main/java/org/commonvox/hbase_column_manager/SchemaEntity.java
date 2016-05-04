/*
 * Copyright (C) 2016 Daniel Vimont
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.commonvox.hbase_column_manager;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;

/**
 *
 * @author Daniel Vimont
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hbaseSchemaEntity")
class SchemaEntity implements Comparable<SchemaEntity> {

  // all simple fields are non-final Strings to facilitate straightforward JAXB marshal/unmarshal

  @XmlAttribute
  private String schemaEntityType;
  @XmlAttribute
  private String name;
  @XmlAttribute
  private String columnDefinitionsEnforced; // String instead of boolean so can be nullified for JAXB!

  private final Map<String, String> values = new HashMap<>(); // String entries for JAXB!
  private final Map<String, String> configurations = new HashMap<>();
  @XmlElementWrapper(name = "children")
  @XmlElement(name = "hbaseSchemaEntity")
  private Set<SchemaEntity> children = null;
  @XmlTransient
  private byte[] foreignKeyValue;

  public SchemaEntity() {
  }

  SchemaEntity(byte entityType, byte[] name) {
    this.schemaEntityType
            = SchemaEntityType.ENTITY_TYPE_BYTE_TO_ENUM_MAP.get(entityType).toString();
    this.name = Bytes.toString(name);
  }

  SchemaEntity(byte entityType, String name) {
    this(entityType, Bytes.toBytes(name));
  }

  SchemaEntity(MNamespaceDescriptor namespaceDescriptor) {
    this(namespaceDescriptor.getEntityRecordType(), namespaceDescriptor.getName());
    shallowClone(namespaceDescriptor);
  }

  SchemaEntity(SchemaEntity metadataEntity) {
    this(metadataEntity.getEntityRecordType(), metadataEntity.getName());
    shallowClone(metadataEntity);
  }

  private void shallowClone(SchemaEntity mEntity) {
    for (Map.Entry<String, String> valueEntry : mEntity.values.entrySet()) {
      this.values.put(valueEntry.getKey(), valueEntry.getValue());
    }
    for (Map.Entry<String, String> configEntry : mEntity.configurations.entrySet()) {
      this.configurations.put(configEntry.getKey(), configEntry.getValue());
    }
    this.columnDefinitionsEnforced = mEntity.columnDefinitionsEnforced;
  }

  SchemaEntity(MTableDescriptor mtd) {
    this(SchemaEntityType.TABLE.getRecordType(), mtd.getNameAsString());
    for (Map.Entry<ImmutableBytesWritable, ImmutableBytesWritable> valueEntry
            : mtd.getValues().entrySet()) {
      this.values.put(Bytes.toString(valueEntry.getKey().copyBytes()),
              Bytes.toString(valueEntry.getValue().copyBytes()));
    }
    for (Map.Entry<String, String> configEntry : mtd.getConfiguration().entrySet()) {
      this.configurations.put(configEntry.getKey(), configEntry.getValue());
    }
  }

  SchemaEntity(MColumnDescriptor mcd) {
    this(SchemaEntityType.COLUMN_FAMILY.getRecordType(), mcd.getNameAsString());
    for (Map.Entry<ImmutableBytesWritable, ImmutableBytesWritable> valueEntry
            : mcd.getValues().entrySet()) {
      this.values.put(Bytes.toString(valueEntry.getKey().copyBytes()),
              Bytes.toString(valueEntry.getValue().copyBytes()));
    }
    for (Map.Entry<String, String> configEntry : mcd.getConfiguration().entrySet()) {
      this.configurations.put(configEntry.getKey(), configEntry.getValue());
    }
    this.setColumnDefinitionsEnforced(mcd.columnDefinitionsEnforced());
  }

  byte getEntityRecordType() {
    return SchemaEntityType.ENTITY_TYPE_LABEL_TO_BYTE_MAP.get(this.schemaEntityType);
  }

  /**
   * Get the Name of this entity.
   *
   * @return name as byte-array
   */
  byte[] getName() {
    return name.getBytes();
  }

  /**
   * Get the Name of this entity as a String.
   *
   * @return name as String
   */
  String getNameAsString() {
    return name;
  }

  /**
   * Get this object's values map.
   *
   * @return this object's values map
   */
  Map<ImmutableBytesWritable, ImmutableBytesWritable> getValues() {
    Map<ImmutableBytesWritable, ImmutableBytesWritable> valuesImmutable = new HashMap<>();
    for (Entry<String, String> entry : values.entrySet()) {
      valuesImmutable.put(new ImmutableBytesWritable(entry.getKey().getBytes()),
              new ImmutableBytesWritable(entry.getValue().getBytes()));
    }
    return valuesImmutable;
  }

  /**
   * Get the value for a specific Value entry.
   *
   * @param key the key
   * @return the value
   */
  byte[] getValue(byte[] key) {
    String valueAsString = values.get(Bytes.toString(key));
    return (valueAsString == null) ? null : valueAsString.getBytes();
  }

  /**
   * Get the value for a specific Value entry.
   *
   * @param key the key
   * @return the value as String
   */
  String getValue(String key) {
    byte[] value = getValue(Bytes.toBytes(key));
    return (value == null) ? null : Bytes.toString(value);
  }

  /**
   * Remove a value entry from value map.
   *
   * @param key the key of entry to remove
   */
  void remove(final byte[] key) {
    values.remove(new ImmutableBytesWritable(key));
  }

  /**
   * Setter for adding value entry to value map.
   *
   * @param key Value key
   * @param value Value value
   * @return this object to allow method chaining
   */
  SchemaEntity setValue(String key, String value) {
    if (value == null) {
      remove(Bytes.toBytes(key));
    } else {
      setValue(Bytes.toBytes(key), Bytes.toBytes(value));
    }
    return this;
  }

  SchemaEntity setValue(byte[] key, byte[] value) {
    values.put(Bytes.toString(key), Bytes.toString(value));
    return this;
  }

  SchemaEntity setValue(final ImmutableBytesWritable key, final ImmutableBytesWritable value) {
    values.put(Bytes.toString(key.copyBytes()), Bytes.toString(value.copyBytes()));
    return this;
  }

  /**
   * Get this object's configuration map.
   *
   * @return this object's configuration map
   */
  Map<String, String> getConfiguration() {
    return configurations;
  }

  /**
   * Get the value for a specific Configuration entry.
   *
   * @param key the key
   * @return the value
   */
  String getConfigurationValue(String key) {
    return configurations.get(key);
  }

  /**
   * Setter for adding configuration entry to configuration map.
   *
   * @param key Value key
   * @param value Value value
   * @return this object to allow method chaining
   */
  SchemaEntity setConfiguration(String key, String value) {
    if (value == null) {
      removeConfiguration(key);
    } else {
      configurations.put(key, value);
    }
    return this;
  }

  /**
   * Remove a configuration entry from configuration map.
   *
   * @param key the key of entry to remove
   */
  void removeConfiguration(String key) {
    configurations.remove(key);
  }

  byte[] getForeignKey() {
    return foreignKeyValue;
  }

  void setForeignKey(byte[] foreignKeyValue) {
    this.foreignKeyValue = foreignKeyValue;
  }

  boolean getColumnDefinitionsEnforced() {
    return Boolean.parseBoolean(this.columnDefinitionsEnforced);
  }

  void setColumnDefinitionsEnforced(boolean enforced) {
    this.columnDefinitionsEnforced = Boolean.toString(enforced);
  }

  void addChild(SchemaEntity child) {
    if (children == null) {
      children = new LinkedHashSet<>();
    }
    children.add(child);
  }

  Set<SchemaEntity> getChildren() {
    return children;
  }

  @Override
  public int compareTo(SchemaEntity other) {
    int result = this.name.compareTo(other.name);
    if (result == 0) {
      result = this.values.hashCode() - other.values.hashCode();
      if (result < 0) {
        result = -1;
      } else if (result > 0) {
        result = 1;
      }
    }
    if (result == 0) {
      result = this.configurations.hashCode() - other.configurations.hashCode();
      if (result < 0) {
        result = -1;
      } else if (result > 0) {
        result = 1;
      }
    }
    return result;
  }

  /**
   * Returns a String in the format "{@link SchemaEntityType}: EntityName"
   * (e.g., "{@code ColumnFamily: myColumnFamily}").
   *
   * @return formatted String
   */
  @Override
  public String toString() {
    return schemaEntityType + ": " + this.name;
  }
}