/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Token} that represents a byte size value with units.
 * Supports parsing values like "10KB", "1.5MB", "2GB", etc.
 */
@PublicEvolving
public class ByteSize implements Token {
  private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*([KMGTP]?B)$");
  private final long bytes;
  private final String original;

  public ByteSize(String value) {
    this.original = value;
    Matcher matcher = BYTE_SIZE_PATTERN.matcher(value.toUpperCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid byte size format: " + value);
    }

    double size = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(2);

    switch (unit) {
      case "B":
        bytes = (long) size;
        break;
      case "KB":
        bytes = (long) (size * 1024);
        break;
      case "MB":
        bytes = (long) (size * 1024 * 1024);
        break;
      case "GB":
        bytes = (long) (size * 1024 * 1024 * 1024);
        break;
      case "TB":
        bytes = (long) (size * 1024L * 1024 * 1024 * 1024);
        break;
      case "PB":
        bytes = (long) (size * 1024L * 1024 * 1024 * 1024 * 1024);
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte size unit: " + unit);
    }
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", type().name());
    object.addProperty("value", original);
    object.addProperty("bytes", bytes);
    return object;
  }

  public long getBytes() {
    return bytes;
  }

  public String getOriginal() {
    return original;
  }
}
