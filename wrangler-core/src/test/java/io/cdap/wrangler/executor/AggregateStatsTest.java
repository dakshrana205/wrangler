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

package io.cdap.wrangler.executor;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.SourceInfo;
import io.cdap.wrangler.api.TokenGroup;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.DirectiveName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.parser.MapArguments;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AggregateStatsTest {
  @Test
  public void testAggregateStats() throws Exception {
    // Create test data
    List<Row> rows = Arrays.asList(
      createRow("1KB", "100ms"),
      createRow("2KB", "200ms"),
      createRow("3KB", "300ms")
    );

    // Create and initialize directive
    AggregateStats directive = new AggregateStats();
    
    // Create usage definition
    UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.TEXT);
    builder.define("total_time_column", TokenType.TEXT);
    
    // Create token group
    TokenGroup group = new TokenGroup(new SourceInfo(1, 1, "test"));
    group.add(new DirectiveName("aggregate-stats"));
    group.add(new ColumnName("size"));
    group.add(new ColumnName("time"));
    group.add(new Text("total_size_mb"));
    group.add(new Text("total_time_sec"));
    
    directive.initialize(new MapArguments(builder.build(), group));

    // Execute directive
    List<Row> results = directive.execute(rows, null);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals(0.005859375, ((Double) result.getValue("total_size_mb")), 0.000000001); // 6KB in MB
    Assert.assertEquals(0.6, ((Double) result.getValue("total_time_sec")), 0.000000001); // 600ms in seconds
  }

  @Test
  public void testEmptyInput() throws Exception {
    List<Row> rows = List.of();

    AggregateStats directive = new AggregateStats();
    
    // Create usage definition
    UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.TEXT);
    builder.define("total_time_column", TokenType.TEXT);
    
    // Create token group
    TokenGroup group = new TokenGroup(new SourceInfo(1, 1, "test"));
    group.add(new DirectiveName("aggregate-stats"));
    group.add(new ColumnName("size"));
    group.add(new ColumnName("time"));
    group.add(new Text("total_size_mb"));
    group.add(new Text("total_time_sec"));
    
    directive.initialize(new MapArguments(builder.build(), group));

    List<Row> results = directive.execute(rows, null);
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals(0.0, ((Double) result.getValue("total_size_mb")), 0.000000001);
    Assert.assertEquals(0.0, ((Double) result.getValue("total_time_sec")), 0.000000001);
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testMissingColumn() throws Exception {
    List<Row> rows = Arrays.asList(new Row().add("size", new ByteSize("1KB")));

    AggregateStats directive = new AggregateStats();
    
    // Create usage definition
    UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.TEXT);
    builder.define("total_time_column", TokenType.TEXT);
    
    // Create token group
    TokenGroup group = new TokenGroup(new SourceInfo(1, 1, "test"));
    group.add(new DirectiveName("aggregate-stats"));
    group.add(new ColumnName("size"));
    group.add(new ColumnName("time"));
    group.add(new Text("total_size_mb"));
    group.add(new Text("total_time_sec"));
    
    directive.initialize(new MapArguments(builder.build(), group));

    directive.execute(rows, null);
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testInvalidValueType() throws Exception {
    List<Row> rows = Arrays.asList(
      new Row()
        .add("size", "1KB")  // String instead of ByteSize
        .add("time", new TimeDuration("100ms"))
    );

    AggregateStats directive = new AggregateStats();
    
    // Create usage definition
    UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.TEXT);
    builder.define("total_time_column", TokenType.TEXT);
    
    // Create token group
    TokenGroup group = new TokenGroup(new SourceInfo(1, 1, "test"));
    group.add(new DirectiveName("aggregate-stats"));
    group.add(new ColumnName("size"));
    group.add(new ColumnName("time"));
    group.add(new Text("total_size_mb"));
    group.add(new Text("total_time_sec"));
    
    directive.initialize(new MapArguments(builder.build(), group));

    directive.execute(rows, null);
  }

  private Row createRow(String size, String time) {
    return new Row()
      .add("size", new ByteSize(size))
      .add("time", new TimeDuration(time));
  }
} 
