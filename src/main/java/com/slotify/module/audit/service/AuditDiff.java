package com.slotify.module.audit.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the {@code diff} payload of an audit log entry from before/after snapshots.
 *
 * <p>Snapshots are plain maps of field name to value (use {@link #snapshot} to build them in a
 * readable way). {@link #between} keeps only the fields whose value changed, as {@code {field:
 * {"from": old, "to": new}}}, so log lines stay small and easy to scan.
 */
public final class AuditDiff {

  /** Key of the previous value inside a field entry. */
  public static final String FROM = "from";

  /** Key of the new value inside a field entry. */
  public static final String TO = "to";

  private AuditDiff() {}

  /**
   * Creates an ordered snapshot from alternating {@code key, value} arguments, tolerating {@code
   * null} values (unlike {@code Map.of}).
   */
  public static Map<String, Object> snapshot(Object... keysAndValues) {
    if (keysAndValues.length % 2 != 0) {
      throw new IllegalArgumentException("snapshot expects key/value pairs");
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
    }
    return map;
  }

  /** Returns only the fields of {@code after} whose value differs from {@code before}. */
  public static Map<String, Object> between(Map<String, Object> before, Map<String, Object> after) {
    Map<String, Object> diff = new LinkedHashMap<>();
    after.forEach(
        (field, newValue) -> {
          Object oldValue = before.get(field);
          if (!Objects.equals(normalise(oldValue), normalise(newValue))) {
            diff.put(field, change(oldValue, newValue));
          }
        });
    return diff;
  }

  /** A single {@code {from, to}} entry, e.g. for status transitions. */
  public static Map<String, Object> change(Object from, Object to) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put(FROM, from);
    entry.put(TO, to);
    return entry;
  }

  /** Compares by string form so enums, numbers and their JSON representations match. */
  private static Object normalise(Object value) {
    return value == null ? null : value.toString();
  }
}
