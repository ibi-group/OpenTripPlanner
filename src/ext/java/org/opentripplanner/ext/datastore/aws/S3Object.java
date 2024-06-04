package org.opentripplanner.ext.datastore.aws;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record S3Object(String bucket, String name) {
  static final String S3_SCHEMA_PREFIX = "s3";

  // Regex pattern to match S3 URLs
  private static final Pattern S3_URL_PATTERN = Pattern.compile(
    "//([\\p{Lower}\\d.-]{3,63})/([^\\p{Cntrl}]+)?"
  );

  // Regex pattern to validate S3 bucket names
  private static final Pattern S3_BUCKET_NAME_PATTERN = Pattern.compile("^[a-z0-9.-]{3,63}$");
  public String toUriString() {
    return toUriString(bucket, name);
  }

  public S3Object child(String childName) {
    return new S3Object(bucket, name + '/' + childName);
  }

  static String toUriString(String bucket, String objectName) {
    return S3_SCHEMA_PREFIX + "://" + bucket + "/" + objectName;
  }

  static URI toUri(String bucket, String objectName) {
    try {
      return new URI(toUriString(bucket, objectName));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getLocalizedMessage(), e);
    }
  }

  boolean isRoot() {
    return "".equals(name);
  }

  /**
   * S3 URL pattern for the Scheme Specific Part, without the 's3:' prefix.
   * Not all rules are validated here, but the following are:
   * <ul>
   *     <li>Bucket names must contain only lowercase letters, numbers, dashes (-), and dots (.)
   *     <li>Bucket names must contain 3 to 63 characters.
   *     <li>Bucket names must not be formatted as IP addresses (e.g., 192.168.1.1).
   *     <li>Object names must be at least one character.
   *     <li>Object names should avoid using control characters.
   * </ul>
   * One exception is allowed, the object name may be an empty string({@code ""}), this is used to
   * create a virtual root directory.
   */
  static S3Object toS3Object(URI uri) {
    Matcher m = S3_URL_PATTERN.matcher(uri.getSchemeSpecificPart());

    if (m.matches()) {
      String bucket = m.group(1);
      String objectName = dirName(m.group(2));

      if (!isValidBucketName(bucket)) {
        throw new IllegalArgumentException(
          "The bucket name '" +
          bucket +
          "' is invalid. " +
          "Bucket names must contain only lowercase letters, numbers, dashes (-), and dots (.), " +
          "contain 3 to 63 characters, and must not be formatted as IP addresses."
        );
      }

      return new S3Object(bucket, objectName);
    }
    throw new IllegalArgumentException(
      "The '" +
      uri +
      "' is not a legal S3 URL on format: '" +
      S3_SCHEMA_PREFIX +
      "://bucket-name/object-name'."
    );
  }

  /* private methods */

  private static String dirName(String objectDir) {
    return objectDir == null ? "" : objectDir;
  }

  private static boolean isValidBucketName(String bucketName) {
    if (!S3_BUCKET_NAME_PATTERN.matcher(bucketName).matches()) {
      return false;
    }
    // Bucket name must not be formatted as an IP address
    return !bucketName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
  }
}
