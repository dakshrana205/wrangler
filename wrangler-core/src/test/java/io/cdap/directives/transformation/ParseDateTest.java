/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.directives.transformation;

import io.cdap.directives.parser.ParseDate;
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
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link ParseDate}
 */
public class ParseDateTest {

  @Test
  public void testDateParser() throws Exception {
    String[] directives = new String[] {
      "parse-as-date date US/Eastern",
      "format-date date_1 MM/dd/yyyy HH:mm"
    };

    List<Row> rows = Arrays.asList(
      new Row("date", "now"),
      new Row("date", "today"),
      new Row("date", "12/10/2016"),
      new Row("date", "12/10/2016 06:45 AM"),
      new Row("date", "september 7th 2016"),
      new Row("date", "1485800109")
    );

    rows = TestingRig.execute(directives, rows);
    Assert.assertTrue(rows.size() == 6);
  }
}
