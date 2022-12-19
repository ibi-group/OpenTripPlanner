package org.opentripplanner.framework.lang;

import javax.annotation.Nonnull;

/**
 * OTP String utils extending the Java lang String...
 */
public class StringUtils {

  private StringUtils() {}

  /** true if the given text is not {@code null} or has at least one none white-space character. */
  public static boolean hasValue(String text) {
    return text != null && !text.isBlank();
  }

  /**
   * Verify String value is NOT {@code null}, empty or only whitespace.
   *
   * @throws IllegalArgumentException if given value is {@code null}, empty or only whitespace.
   */
  public static String assertHasValue(String value) {
    return assertHasValue(value, "");
  }

  /**
   * Verify String value is NOT {@code null}, empty or only whitespace.
   * @param errorMessage optional custom message to be displayed as the exception message.
   * @param placeholders optional placeholders used in the error message format.
   * @throws IllegalArgumentException if given value is {@code null}, empty or only whitespace.
   */
  public static String assertHasValue(String value, String errorMessage, Object... placeholders) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
        errorMessage.formatted(placeholders) +
        " [Value cannot be null, empty or just whitespace: " +
        (value == null ? "null]" : "'" + value + "']")
      );
    }
    return value;
  }

  /**
   * Add the given number of characters to the buffer.
   * @param buffer the buffer to append to.
   * @param ch the character to add to the buffer.
   * @param count the number of characters to add. If 0 or negative nothing is added.
   * @return the given buffer input for convenient chaining
   */
  public static StringBuilder append(StringBuilder buffer, char ch, int count) {
    while (count > 0) {
      buffer.append(ch);
      --count;
    }
    return buffer;
  }

  public static String padLeft(String value, char ch, int width) {
    if (value == null) {
      return String.valueOf(ch).repeat(width);
    }
    if (value.length() >= width) {
      return value;
    }
    return StringUtils
      .append(new StringBuilder(), ch, width - value.length())
      .append(value)
      .toString();
  }

  public static String padBoth(String value, char ch, int width) {
    if (value == null) {
      return String.valueOf(ch).repeat(width);
    }
    if (value.length() >= width) {
      return value;
    }
    var buf = new StringBuilder();
    StringUtils.append(buf, ch, (width + 1 - value.length()) / 2);
    buf.append(value);
    StringUtils.append(buf, ch, width - buf.length());
    return buf.toString();
  }

  public static String padRight(String value, char ch, int width) {
    if (value == null) {
      return String.valueOf(ch).repeat(width);
    }
    if (value.length() >= width) {
      return value;
    }
    return StringUtils.append(new StringBuilder(value), ch, width - value.length()).toString();
  }

  /** Replace single quotes with double quotes.  */
  public static String quoteReplace(@Nonnull String text) {
    return text.replace('\'', '\"');
  }
}
