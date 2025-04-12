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
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.Expression;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.expression.EL;
import io.cdap.wrangler.expression.ELContext;
import io.cdap.wrangler.expression.ELException;
import io.cdap.wrangler.expression.ELResult;

import java.util.List;

/**
 * A directive for sending records to error collector based on condition.
 */
@Plugin(type = Directive.TYPE)
@Name("send-to-error-and-continue")
@Categories(categories = { "row", "data-quality"})
@Description("Sends a row to error and continues processing if condition is true")
public class SendToErrorAndContinue implements Directive {
  public static final String NAME = "send-to-error-and-continue";
  private String condition;
  private String message;
  private EL conditionExpr;
  private EL messageExpr;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("condition", TokenType.EXPRESSION);
    builder.define("message", TokenType.EXPRESSION);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    condition = ((Expression) args.value("condition")).value();
    message = ((Expression) args.value("message")).value();
    try {
      this.conditionExpr = EL.compile(condition);
      this.messageExpr = EL.compile(message);
    } catch (ELException e) {
      throw new DirectiveParseException(NAME, e.getMessage(), e);
    }
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context)
      throws DirectiveExecutionException, ErrorRowException {
    for (Row row : rows) {
      try {
        if (context != null && context.getTransientStore() != null) {
          // Initialize dq_failure counter for each row
          context.getTransientStore().set(TransientVariableScope.GLOBAL, "dq_failure", 0L);
        }
        execute(row, context);
      } catch (ErrorRowException e) {
        // Let the RecipePipelineExecutor handle the error
        if (context != null && context.getTransientStore() != null) {
          context.getTransientStore().increment(TransientVariableScope.GLOBAL, "dq_failure", 1L);
        }
        throw e;
      } catch (Exception e) {
        throw new DirectiveExecutionException(NAME, e.getMessage(), e);
      }
    }
    return rows;
  }

  private Row execute(Row row, ExecutorContext context) throws ErrorRowException {
    try {
      ELContext elContext = new ELContext(context, conditionExpr, row);
      ELResult result = conditionExpr.execute(elContext);
      if (result.getBoolean()) {
        elContext = new ELContext(context, messageExpr, row);
        String errorMessage = String.valueOf(messageExpr.execute(elContext).getObject());
        throw new ErrorRowException(NAME, errorMessage, 1);
      }
    } catch (ELException e) {
      throw new ErrorRowException(NAME, e.getMessage(), 1);
    }
    return row;
  }
}



