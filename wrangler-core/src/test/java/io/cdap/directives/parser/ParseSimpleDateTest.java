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

package io.cdap.directives.parser;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

public class ParseSimpleDateTest {
  @Test
  public void testBasicDateParsing() throws Exception {
    String[] directives = new String[] {
      "parse-as-simple-date date MM/dd/yyyy"
    };

    List<Row> rows = Arrays.asList(
      new Row("date", "12/10/2016")
    );

    rows = TestingRig.execute(directives, rows);
    LocalDate localDate = LocalDate.of(2016, 12, 10);
    LocalTime zeroTime = LocalTime.of(0, 0);
    ZonedDateTime expected = ZonedDateTime.of(localDate, zeroTime, ZoneId.ofOffset("UTC", ZoneOffset.UTC));
    Assert.assertEquals(expected, rows.get(0).getValue("date"));
  }
}
