package ch.ivyteam.db.meta.generator.internal;

import java.util.List;
import java.util.stream.Collectors;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlAtom;
import ch.ivyteam.db.meta.model.internal.SqlBinaryRelation;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexWhenThen;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlFunction;
import ch.ivyteam.db.meta.model.internal.SqlLiteral;
import ch.ivyteam.db.meta.model.internal.SqlLogicalExpression;
import ch.ivyteam.db.meta.model.internal.SqlNot;
import ch.ivyteam.db.meta.model.internal.SqlParent;
import ch.ivyteam.db.meta.model.internal.SqlSimpleExpr;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlWhenThen;

public class ReplaceOldTriggerVariable {

  private final Triggers triggers;

  public ReplaceOldTriggerVariable(Triggers triggers) {
    this.triggers = triggers;
  }

  public SqlDmlStatement replace(SqlDmlStatement stmt) {
    if (stmt instanceof SqlDelete) {
      SqlDelete delete = (SqlDelete) stmt;
      return new SqlDelete(delete.getTable(), replace(delete.getFilterExpression()));
    } else if (stmt instanceof SqlUpdate) {
      SqlUpdate update = (SqlUpdate) stmt;
      return new SqlUpdate(update.getTable(), update.getColumnExpressions(),
              replace(update.getFilterExpression()), update.getDatabaseManagementSystemHints(),
              update.getComment());
    }
    throw new MetaException("Cannot replace old trigger variable in type " + stmt);
  }

  private SqlSimpleExpr replace(SqlSimpleExpr filterExpression) {
    if (filterExpression instanceof SqlBinaryRelation) {
      SqlBinaryRelation binRel = (SqlBinaryRelation) filterExpression;
      return new SqlBinaryRelation(replace(binRel.getFirst()), binRel.getOperator(),
              replace(binRel.getSecond()));
    } else if (filterExpression instanceof SqlLogicalExpression) {
      SqlLogicalExpression logicalExpr = (SqlLogicalExpression) filterExpression;
      return new SqlLogicalExpression(replace(logicalExpr.getFirst()), logicalExpr.getOperator(),
              logicalExpr.getSecond());
    } else if (filterExpression instanceof SqlNot) {
      SqlNot not = (SqlNot) filterExpression;
      return new SqlNot(replace(not.getExpression()));
    } else if (filterExpression instanceof SqlParent) {
      SqlParent parent = (SqlParent) filterExpression;
      return new SqlParent(replace(parent.getExpression()));
    }
    throw new MetaException("Cannot replace old trigger variable in type " + filterExpression);
  }

  private SqlAtom replace(SqlAtom expression) {
    if (expression instanceof SqlFullQualifiedColumnName) {
      SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName) expression;
      return replace(columnName);
    } else if (expression instanceof SqlCaseExpr) {
      SqlCaseExpr caseExpr = (SqlCaseExpr) expression;
      return new SqlCaseExpr(replace(caseExpr.getColumnName()),
              replaceInWhenThenList(caseExpr.getWhenThenList()));
    } else if (expression instanceof SqlComplexCaseExpr) {
      SqlComplexCaseExpr caseExpr = (SqlComplexCaseExpr) expression;
      return new SqlComplexCaseExpr(replaceInComplexWhenThenList(caseExpr.getWhenThenList()),
              replace(caseExpr.getElseAction()));
    } else if (expression instanceof SqlLiteral) {
      return expression;
    } else if (expression instanceof SqlFunction) {
      SqlFunction function = (SqlFunction) expression;
      return new SqlFunction(function.getName(), replace(function.getArguments()));
    }
    throw new MetaException("Cannot replace old trigger variable in type " + expression);
  }

  private SqlFullQualifiedColumnName replace(SqlFullQualifiedColumnName columnName) {
    if (triggers.isDefaultRowTriggerOldVariableName(columnName.getTable())) {
      return triggers.dmlStatements.identifiers.replaceTriggerVariable(columnName,
              triggers.getRowTriggerOldVariableName());
    }
    return new SqlFullQualifiedColumnName(columnName.getTable(), columnName.getColumn());
  }

  private List<SqlAtom> replace(List<SqlAtom> arguments) {
    return arguments.stream().map(this::replace).collect(Collectors.toList());
  }

  private List<SqlComplexWhenThen> replaceInComplexWhenThenList(List<SqlComplexWhenThen> whenThenList) {
    return whenThenList.stream().map(this::replace).collect(Collectors.toList());
  }

  private List<SqlWhenThen> replaceInWhenThenList(List<SqlWhenThen> whenThenList) {
    return whenThenList.stream().map(this::replace).collect(Collectors.toList());
  }

  private SqlWhenThen replace(SqlWhenThen whenThen) {
    return new SqlWhenThen(whenThen.getLiteral(), replace(whenThen.getColumnName()));
  }

  private SqlComplexWhenThen replace(SqlComplexWhenThen whenThen) {
    return new SqlComplexWhenThen(replace(whenThen.getCondition()), replace(whenThen.getAction()));
  }
}
