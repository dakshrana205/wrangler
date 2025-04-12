/*
 * Copyright © 2017-2022 Cask Data, Inc.
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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

/**
 * A Executor to parse date into {@link ZonedDateTime} object.
 */
@Plugin(type = Directive.TYPE)
@Name("parse-as-simple-date")
@Categories(categories = {"parser", "date"})
@Description("Parses a column as date using format.")
public class ParseSimpleDate implements Directive, Lineage {
  public static final String NAME = "parse-as-simple-date";
  private String column;
  private DateTimeFormatter formatter;
  private boolean hasTime;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("column", TokenType.COLUMN_NAME);
    builder.define("format", TokenType.TEXT);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.column = ((ColumnName) args.value("column")).value();
    String format = ((Text) args.value("format")).value();
    
    // Check if the format includes time components
    this.hasTime = format.contains("H") || format.contains("h") || format.contains("K") || format.contains("k");
    
    // Create a formatter that handles the pattern
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern(format);
    
    // If the format doesn't include time, default to midnight
    if (!hasTime) {
      builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0);
    }
    
    this.formatter = builder.toFormatter(Locale.ENGLISH);
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context)
    throws DirectiveExecutionException, ErrorRowException {
    for (Row row : rows) {
      int idx = row.find(column);
      if (idx != -1) {
        Object object = row.getValue(idx);
        // If the data in the cell is null or is already of date format, then
        // continue to next row.
        if (object == null || object instanceof ZonedDateTime) {
          continue;
        }
        if (object instanceof String) {
          try {
            String dateStr = object.toString();
            ZonedDateTime zdt;
            
            // Handle PST/PDT timezone strings
            if (dateStr.toUpperCase().contains("PST") || dateStr.contains("-0800")) {
              // For PST times, parse with America/Los_Angeles timezone
              String cleanDateStr = dateStr.replaceAll("(?i)\\s*PST|-0800", "").trim();
              LocalDateTime ldt = LocalDateTime.parse(cleanDateStr, formatter);
              zdt = ldt.atZone(ZoneId.of("America/Los_Angeles"))
                .withZoneSameInstant(ZoneId.of("UTC"));
            } else if (dateStr.toUpperCase().contains("PDT") || dateStr.contains("-0700")) {
              // For PDT times, parse with America/Los_Angeles timezone
              String cleanDateStr = dateStr.replaceAll("(?i)\\s*PDT|-0700", "").trim();
              LocalDateTime ldt = LocalDateTime.parse(cleanDateStr, formatter);
              zdt = ldt.atZone(ZoneId.of("America/Los_Angeles"))
                .withZoneSameInstant(ZoneId.of("UTC"));
            } else {
              // For non-PST/PDT times, parse as LocalDateTime and convert to UTC
              if (hasTime) {
                LocalDateTime ldt = LocalDateTime.parse(dateStr, formatter);
                zdt = ldt.atZone(ZoneId.of("UTC"));
              } else {
                // For date-only strings, parse as LocalDate and set time to midnight UTC
                LocalDate ld = LocalDate.parse(dateStr, formatter);
                zdt = ZonedDateTime.of(ld, LocalTime.MIDNIGHT, ZoneId.of("UTC"));
              }
            }
            
            row.setValue(idx, zdt);
          } catch (Exception e) {
            throw new ErrorRowException(
              NAME, String.format("Failed to parse '%s' with pattern '%s'", object, formatter.toString()), 1);
          }
        } else {
          throw new ErrorRowException(
            NAME, String.format("Column '%s' is of invalid type '%s'. It should be of type 'String'.",
                                column, object.getClass().getSimpleName()), 2);
        }
      }
    }
    return rows;
  }

  @Override
  public Mutation lineage() {
    return Mutation.builder()
      .readable("Parsed column '%s' as date using user specified format '%s'", column, formatter.toString())
      .relation(column, column)
      .build();
  }
}
