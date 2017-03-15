package com.adobe.acs.commons.versionedclientlibs.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class Md5UriUtil {

  private Md5UriUtil() {
    // utility class
  }

  /**
   * Gets out the MD5 part out of any URI like uri(.min)(.md5).ext
   *
   * @return md5
   */
  @Nonnull
  static String getMD5FromURI(@Nullable final String uri) {
    String md5 = "";
    if (uri != null) {
      String[] parts = uri.split("\\.");
      if (parts.length > 2) {
        md5 = parts[parts.length - 2];
        if ("min".equals(md5)) {
          md5 = "";
        }
      }
    }
    return md5;
  }

  /**
   * Strips out all selectors from any URI like uri(.sel)...(.sel).ext
   *
   * @return uri.ext
   */
  @Nonnull
  static String cleanURI(@Nullable final String uri) {
    StringBuilder result = new StringBuilder();
    if (uri != null) {
      String[] parts = uri.split("\\.");
      if (parts.length > 2) {
        result.append(parts[0]);
        result.append('.');
        result.append(parts[parts.length - 1]);
      } else {
        // too little parts, just return what came in
        result.append(uri);
      }
    }
    return result.toString();
  }
}
