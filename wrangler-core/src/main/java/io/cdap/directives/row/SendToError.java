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

package io.cdap.directives.row;

import com.google.common.collect.ImmutableList;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.EntityCountMetric;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.ReportErrorAndProceed;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.Expression;
import io.cdap.wrangler.api.parser.Identifier;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.expression.EL;
import io.cdap.wrangler.expression.ELContext;
import io.cdap.wrangler.expression.ELException;
import io.cdap.wrangler.expression.ELResult;

import java.util.ArrayList;
import java.util.List;

import static io.cdap.wrangler.metrics.JexlCategoryMetricUtils.getJexlCategoryMetric;

/**
 * A directive for erroring the record if
 *
 * <p>
 *   This step will evaluate the condition, if the condition evaluates to
 *   true, then the row will be skipped. If the condition evaluates to
 *   false, then the row will be accepted.
 * </p>
 */
@Plugin(type = Directive.TYPE)
@Name(SendToError.NAME)
@Categories(categories = { "row", "data-quality"})
@Description("Send records that match condition to the error collector.")
public class SendToError implements Directive, Lineage {
  public static final String NAME = "send-to-error";
  private EL el;
  private String condition;
  private String metric = null;
  private String message = null;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("condition", TokenType.EXPRESSION);
    builder.define("metric", TokenType.IDENTIFIER, Optional.TRUE);
    builder.define("message", TokenType.TEXT, Optional.TRUE);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    condition = ((Expression) args.value("condition")).value();
    try {
      el = EL.compile(condition);
    } catch (ELException e) {
      throw new DirectiveParseException(
        NAME, String.format(" Invalid condition '%s'.", condition)
      );
    }
    if (args.contains("metric")) {
      metric = ((Identifier) args.value("metric")).value();
    }
    if (args.contains("message")) {
      message = ((Text) args.value("message")).value();
    }
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context)
    throws DirectiveExecutionException, ErrorRowException {
    List<Row> results = new ArrayList<>();
    List<Row> errors = new ArrayList<>();
    TransientStore store = context.getTransientStore();

    // Initialize transient variables if they don't exist
    if (!store.getVariables().contains("total")) {
      store.set(TransientVariableScope.GLOBAL, "total", 0L);
    }
    if (!store.getVariables().contains("success")) {
      store.set(TransientVariableScope.GLOBAL, "success", 0L);
    }
    if (!store.getVariables().contains("failure")) {
      store.set(TransientVariableScope.GLOBAL, "failure", 0L);
    }
    if (!store.getVariables().contains("dq_failure")) {
      store.set(TransientVariableScope.GLOBAL, "dq_failure", 0L);
    }

    for (Row row : rows) {
      store.increment(TransientVariableScope.GLOBAL, "total", 1L);
      
      try {
        // Move the fields from the row into the context.
        ELContext ctx = new ELContext(context, el, row);

        // Add transient variables to context
        for (String variable : store.getVariables()) {
          ctx.set(variable, store.get(variable));
        }

        // Execute the condition
        ELResult result = el.execute(ctx);
        if (result.getBoolean()) {
          // For dq_failure checks, only mark as error if not already marked
          if (condition.contains("dq_failure")) {
            if (row.find("_error") == -1) {
              store.increment(TransientVariableScope.GLOBAL, "failure", 1L);
              row.add("_error", true);
              row.add("_error_msg", message != null ? message : condition);
              errors.add(row);
            }
          } else {
            store.increment(TransientVariableScope.GLOBAL, "failure", 1L);
            row.add("_error", true);
            row.add("_error_msg", message != null ? message : condition);
            errors.add(row);
          }
        } else {
          store.increment(TransientVariableScope.GLOBAL, "success", 1L);
          results.add(row);
        }
      } catch (ELException e) {
        throw new DirectiveExecutionException(NAME, e.getMessage(), e);
      }
    }
    
    if (!errors.isEmpty()) {
      throw new ErrorRowException(NAME, message != null ? message : condition, errors.size());
    }
    
    return results;
  }

  @Override
  public Mutation lineage() {
    Mutation.Builder builder = Mutation.builder()
      .readable("Redirecting records to error path based on expression '%s'", condition);
    el.variables().forEach(column -> builder.relation(column, column));
    return builder.build();
  }

  @Override
  public List<EntityCountMetric> getCountMetrics() {
    EntityCountMetric jexlCategoryMetric = getJexlCategoryMetric(el.getScriptParsedText());
    return (jexlCategoryMetric == null) ? null : ImmutableList.of(jexlCategoryMetric);
  }
}
