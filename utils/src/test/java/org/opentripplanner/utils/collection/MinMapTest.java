package org.opentripplanner.utils.collection;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Comparator;
import org.junit.jupiter.api.Test;

class MinMapTest {

  private static final String KEY = "key";

  @Test
  void putMinAndGet() {
    MinMap<String, String> subject = MinMap.of();
    assertNull(subject.get(KEY));

    subject.putMin(KEY, "orange");
    assertEquals("orange", subject.get(KEY));

    subject.putMin(KEY, "apple");
    assertEquals("apple", subject.get(KEY));

    subject.putMin(KEY, "banana");
    assertEquals("apple", subject.get(KEY));
  }

  @Test
  void values() {
    MinMap<String, String> subject = MinMap.of();
    subject.putMin("key1", "orange");
    assertThat(subject.values()).containsExactly("orange");

    subject.putMin("key2", "apple");
    assertThat(subject.values()).containsExactly("apple", "orange");

    subject.putMin("key3", "banana");
    assertThat(subject.values()).containsExactly("apple", "banana", "orange");
  }

  @Test
  void customComparator() {
    MinMap<String, String> subject = MinMap.of(Comparator.comparingInt(String::length));

    subject.putMin(KEY, "AAA");
    assertEquals("AAA", subject.get(KEY));

    subject.putMin(KEY, "BBB");
    assertEquals("AAA", subject.get(KEY));

    subject.putMin(KEY, "BB");
    assertEquals("BB", subject.get(KEY));

    subject.putMin(KEY, "BBBB");
    assertEquals("BB", subject.get(KEY));
  }
}
