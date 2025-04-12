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
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A directive for aggregating numeric values with various functions like sum, avg, min, max.
 */
@Plugin(type = Directive.TYPE)
@Name(NumericAggregate.NAME)
@Categories(categories = {"aggregate"})
@Description("Aggregates numeric values using functions like sum, avg, min, max.")
public class NumericAggregate implements Directive {
    public static final String NAME = "numeric-aggregate";
    private String sourceColumn;
    private String targetColumn;
    private String function;
    private static final Set<String> VALID_FUNCTIONS = new HashSet<>(Arrays.asList("sum", "avg", "min", "max"));
    private static final String COUNT_SUFFIX = "_count";
    private static final String VALUE_SUFFIX = "_value";

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
        builder.define("source", TokenType.COLUMN_NAME);
        builder.define("target", TokenType.COLUMN_NAME);
        builder.define("function", TokenType.TEXT);
        return builder.build();
    }

    @Override
    public void initialize(Arguments args) throws DirectiveParseException {
        this.sourceColumn = ((ColumnName) args.value("source")).value();
        this.targetColumn = ((ColumnName) args.value("target")).value();
        this.function = ((Text) args.value("function")).value().toLowerCase();

        if (!VALID_FUNCTIONS.contains(this.function)) {
            throw new DirectiveParseException(
                String.format("Invalid aggregation function '%s'. Valid functions are: %s", function, VALID_FUNCTIONS));
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
        TransientStore store = context.getTransientStore();
        String countKey = targetColumn + COUNT_SUFFIX;
        String valueKey = targetColumn + VALUE_SUFFIX;

        // Initialize if not already done
        if (!store.getVariables().contains(countKey)) {
            store.set(TransientVariableScope.GLOBAL, countKey, 0L);
            
            if (function.equals("min")) {
                store.set(TransientVariableScope.GLOBAL, valueKey, Double.MAX_VALUE);
            } else if (function.equals("max")) {
                store.set(TransientVariableScope.GLOBAL, valueKey, Double.MIN_VALUE);
            } else {
                store.set(TransientVariableScope.GLOBAL, valueKey, 0.0);
            }
        }

        // Process each row
        for (Row row : rows) {
            Object value = row.getValue(sourceColumn);
            if (value != null && value instanceof Number) {
                double numericValue = ((Number) value).doubleValue();
                long count = (Long) store.get(countKey);
                double currentValue = (Double) store.get(valueKey);

                // Update count
                store.set(TransientVariableScope.GLOBAL, countKey, count + 1);

                // Update value based on function
                switch (function) {
                    case "sum":
                    case "avg":
                        store.set(TransientVariableScope.GLOBAL, valueKey, currentValue + numericValue);
                        break;
                    case "min":
                        store.set(TransientVariableScope.GLOBAL, valueKey, Math.min(currentValue, numericValue));
                        break;
                    case "max":
                        store.set(TransientVariableScope.GLOBAL, valueKey, Math.max(currentValue, numericValue));
                        break;
                }
            }
        }

        // Calculate final result for each row
        List<Row> results = new ArrayList<>();
        for (Row row : rows) {
            long count = (Long) store.get(countKey);
            double value = (Double) store.get(valueKey);

            // Calculate the result based on the function
            double result;
            if (function.equals("avg") && count > 0) {
                result = value / count;
            } else {
                result = value;
            }

            row.addOrSet(targetColumn, result);
            results.add(row);
        }

        return results;
    }
} 
