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

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

public class TimeDurationTest {
  @Test
  public void testValidTimeDurations() {
    // Test basic time durations
    Assert.assertEquals(1_000_000, new TimeDuration("1ms").getNanoseconds());
    Assert.assertEquals(1_000_000_000, new TimeDuration("1s").getNanoseconds());
    Assert.assertEquals(60 * 1_000_000_000L, new TimeDuration("1m").getNanoseconds());
    Assert.assertEquals(60 * 60 * 1_000_000_000L, new TimeDuration("1h").getNanoseconds());
    Assert.assertEquals(24 * 60 * 60 * 1_000_000_000L, new TimeDuration("1d").getNanoseconds());

    // Test decimal values
    Assert.assertEquals(500_000, new TimeDuration("0.5ms").getNanoseconds());
    Assert.assertEquals(1_500_000, new TimeDuration("1.5ms").getNanoseconds());
    Assert.assertEquals(1.5 * 1_000_000_000, new TimeDuration("1.5s").getNanoseconds(), 0.001);

    // Test with spaces
    Assert.assertEquals(1_000_000, new TimeDuration("1 ms").getNanoseconds());
    Assert.assertEquals(1_000_000, new TimeDuration("1  ms").getNanoseconds());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new TimeDuration("1xs");
  }

  @Test
  public void testValueAndType() {
    TimeDuration duration = new TimeDuration("1s");
    Assert.assertEquals(1_000_000_000L, duration.value());
    Assert.assertEquals("1s", duration.getOriginal());
  }
} 
