/*
 * Copyright © 2024 Cask Data, Inc.
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

package io.cdap.directives.aggregates;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A directive for aggregating byte sizes and time durations.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates size and time values across rows.")
public class AggregateStats implements Directive {
    public static final String NAME = "aggregate-stats";
    private String aggregationType;
    private String sizeColumn;
    private String timeColumn;
    private String totalSizeColumn;
    private String totalTimeColumn;
    private String sizeUnit;
    private String timeUnit;
    private String totalSizeUnit;
    private String totalTimeUnit;
    private static final Set<String> VALID_SIZE_UNITS = new HashSet<>(Arrays.asList("B", "KB", "MB", "GB", "TB"));
    private static final Set<String> VALID_TIME_UNITS = new HashSet<>(Arrays.asList("S", "M", "H", "D"));
    private static final String TOTAL_SIZE_KEY = "aggregate_stats_total_size";
    private static final String TOTAL_TIME_KEY = "aggregate_stats_total_time";
    private static final String VALID_ROWS_KEY = "aggregate_stats_valid_rows";

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("size", TokenType.COLUMN_NAME);
        builder.define("time", TokenType.COLUMN_NAME);
        builder.define("total_size", TokenType.COLUMN_NAME);
        builder.define("total_time", TokenType.COLUMN_NAME);
        builder.define("size_unit", TokenType.TEXT);
        builder.define("time_unit", TokenType.TEXT);
        builder.define("aggregation_type", TokenType.TEXT);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        sizeColumn = ((ColumnName) args.value("size")).value();
        timeColumn = ((ColumnName) args.value("time")).value();
        totalSizeColumn = ((ColumnName) args.value("total_size")).value();
        totalTimeColumn = ((ColumnName) args.value("total_time")).value();
        sizeUnit = ((Text) args.value("size_unit")).value();
        timeUnit = ((Text) args.value("time_unit")).value();
        aggregationType = ((Text) args.value("aggregation_type")).value();

        // Validate size units
        if (!VALID_SIZE_UNITS.contains(sizeUnit.toUpperCase())) {
            throw new DirectiveParseException(
                String.format("Invalid size unit '%s'. Valid units are: %s", sizeUnit, VALID_SIZE_UNITS));
        }

        // Validate time units
        if (!VALID_TIME_UNITS.contains(timeUnit.toUpperCase())) {
            throw new DirectiveParseException(
                String.format("Invalid time unit '%s'. Valid units are: %s", timeUnit, VALID_TIME_UNITS));
        }

        // Validate aggregation type
        if (!aggregationType.equalsIgnoreCase("total") && !aggregationType.equalsIgnoreCase("average")) {
            throw new DirectiveParseException(
                String.format("Invalid aggregation type '%s'. Valid types are: total, average", aggregationType));
        }
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) {
        double totalSize = 0.0;
        double totalTime = 0.0;
        long validRows = 0;

        // Process all rows first
        for (Row row : rows) {
            Object sizeObj = row.getValue(sizeColumn);
            Object timeObj = row.getValue(timeColumn);

            if (sizeObj != null && timeObj != null) {
                double size = ((Number) sizeObj).doubleValue();
                double time = ((Number) timeObj).doubleValue();

                // Convert size to bytes
                size = convertToBytes(size, sizeUnit);
                // Convert time to seconds
                time = convertToSeconds(time, timeUnit);

                // Update running totals
                totalSize += size;
                totalTime += time;
                validRows++;
            }
        }

        // Create aggregated row
        Row aggregatedRow = new Row();
        
        if (aggregationType.equalsIgnoreCase("total")) {
            // Convert back to the target units
            double finalSize = convertFromBytes(totalSize, sizeUnit);
            double finalTime = convertFromSeconds(totalTime, timeUnit);
            aggregatedRow.add(totalSizeColumn, finalSize);
            aggregatedRow.add(totalTimeColumn, finalTime);
        } else if (aggregationType.equalsIgnoreCase("average")) {
            // For average, always set a value even with no valid rows
            double avgSize = validRows > 0 ? convertFromBytes(totalSize / validRows, sizeUnit) : 0.0;
            double avgTime = validRows > 0 ? convertFromSeconds(totalTime / validRows, timeUnit) : 0.0;
            aggregatedRow.add(totalSizeColumn, avgSize);
            aggregatedRow.add(totalTimeColumn, avgTime);
        }

        // For empty input, still return the aggregated row with zeros
        if (rows.isEmpty()) {
            aggregatedRow.add(totalSizeColumn, 0.0);
            aggregatedRow.add(totalTimeColumn, 0.0);
        }

        // Only return the aggregated row
        return Collections.singletonList(aggregatedRow);
    }

    private double convertToBytes(double value, String unit) {
        switch (unit.toUpperCase()) {
            case "B":
                return value;
            case "KB":
                return value * 1024;
            case "MB":
                return value * 1024 * 1024;
            case "GB":
                return value * 1024 * 1024 * 1024;
            case "TB":
                return value * 1024 * 1024 * 1024 * 1024;
            default:
                throw new IllegalArgumentException("Invalid size unit: " + unit);
        }
    }

    private double convertFromBytes(double bytes, String unit) {
        switch (unit.toUpperCase()) {
            case "B":
                return bytes;
            case "KB":
                return bytes / 1024;
            case "MB":
                return bytes / (1024 * 1024);
            case "GB":
                return bytes / (1024 * 1024 * 1024);
            case "TB":
                return bytes / (1024 * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("Invalid size unit: " + unit);
        }
    }

    private double convertToSeconds(double value, String unit) {
        switch (unit.toUpperCase()) {
            case "S":
                return value;
            case "M":
                return value * 60;
            case "H":
                return value * 3600;
            case "D":
                return value * 86400;
            default:
                throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    private double convertFromSeconds(double seconds, String unit) {
        switch (unit.toUpperCase()) {
            case "S":
                return seconds;
            case "M":
                return seconds / 60;
            case "H":
                return seconds / 3600;
            case "D":
                return seconds / 86400;
            default:
                throw new IllegalArgumentException("Invalid time unit: " + unit);
        }
    }

    @Override
    public void destroy() {
        // Clean up any resources if needed
    }
} 

