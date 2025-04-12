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
 * A {@link Token} that represents a time duration value with units.
 * Supports parsing values like "10ms", "1.5s", "2m", "1h", etc.
 */
@PublicEvolving
public class TimeDuration implements Token {
  private static final Pattern TIME_DURATION_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)$");
  private final long nanoseconds;
  private final String original;

  public TimeDuration(String value) {
    this.original = value;
    Matcher matcher = TIME_DURATION_PATTERN.matcher(value.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time duration format: " + value);
    }

    double duration = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(2);

    switch (unit) {
      case "ns":
        nanoseconds = (long) duration;
        break;
      case "ms":
        nanoseconds = (long) (duration * 1_000_000);
        break;
      case "s":
        nanoseconds = (long) (duration * 1_000_000_000);
        break;
      case "m":
        nanoseconds = (long) (duration * 60 * 1_000_000_000);
        break;
      case "h":
        nanoseconds = (long) (duration * 60 * 60 * 1_000_000_000);
        break;
      case "d":
        nanoseconds = (long) (duration * 24 * 60 * 60 * 1_000_000_000);
        break;
      default:
        throw new IllegalArgumentException("Unsupported time unit: " + unit);
    }
  }

  @Override
  public Object value() {
    return nanoseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", type().name());
    object.addProperty("value", original);
    object.addProperty("nanoseconds", nanoseconds);
    return object;
  }

  public long getNanoseconds() {
    return nanoseconds;
  }

  public String getOriginal() {
    return original;
  }
}
