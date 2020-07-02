package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlAtom;
import ch.ivyteam.db.meta.model.internal.SqlBinaryRelation;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexWhenThen;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlFunction;
import ch.ivyteam.db.meta.model.internal.SqlInsert;
import ch.ivyteam.db.meta.model.internal.SqlInsertWithSelect;
import ch.ivyteam.db.meta.model.internal.SqlInsertWithValues;
import ch.ivyteam.db.meta.model.internal.SqlJoinTable;
import ch.ivyteam.db.meta.model.internal.SqlLiteral;
import ch.ivyteam.db.meta.model.internal.SqlLogicalExpression;
import ch.ivyteam.db.meta.model.internal.SqlNot;
import ch.ivyteam.db.meta.model.internal.SqlNull;
import ch.ivyteam.db.meta.model.internal.SqlParent;
import ch.ivyteam.db.meta.model.internal.SqlSelect;
import ch.ivyteam.db.meta.model.internal.SqlSelectExpression;
import ch.ivyteam.db.meta.model.internal.SqlSimpleExpr;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;
import ch.ivyteam.db.meta.model.internal.SqlWhenThen;

public class DmlStatements
{
  protected final Spaces spaces = new Spaces();
  protected final Delimiter delimiter;
  protected final Identifiers identifiers;
  protected final DbHints dbHints;
  
  public DmlStatements(DbHints dbHints, Delimiter delimiter, Identifiers identifiers)
  {
    this.dbHints = dbHints;
    this.delimiter = delimiter;
    this.identifiers = identifiers;
  }
  
  final void generate(PrintWriter pr, SqlDmlStatement stmt, int insets)
  {
    if (stmt instanceof SqlDelete)
    {
      generateDelete(pr, (SqlDelete)stmt, insets);
    }
    else if (stmt instanceof SqlUpdate)
    {
      generateUpdate(pr, (SqlUpdate)stmt, insets);
    }    
  }
  
  protected void generateDelete(PrintWriter pr, SqlDelete deleteStmt, int indent)
  {
    spaces.generate(pr, indent);
    pr.print("DELETE FROM ");
    pr.println(deleteStmt.getTable());
    spaces.generate(pr, indent);
    pr.print("WHERE ");
    generateFilterExpression(pr, deleteStmt.getFilterExpression());
  }
  
  public final void generateDelete(PrintWriter pr, SqlInsertWithValues insert)
  {
    pr.append("DELETE FROM ");
    pr.append(insert.getTable());
    pr.append(" WHERE ");
    
    boolean first = true;
    for (int pos = 0; pos < insert.getColumns().size(); pos++)
    {
      if (!first)
      {
        pr.append(" AND ");
      }
      first = false;
      String column = insert.getColumns().get(pos);
      pr.append(column);
      pr.append("=");
      Object value = insert.getValues().get(pos).getValue();
      generateValue(pr, value);      
    }
    delimiter.generate(pr);
    pr.append("\n\n");
  }
  
  public void generateUpdate(PrintWriter pr, SqlUpdate updateStmt, int indent)
  {
    spaces.generate(pr, indent);
    pr.print("UPDATE ");
    pr.println(updateStmt.getTable());
    spaces.generate(pr, indent);
    pr.print("SET ");
    boolean first = true;
    for (SqlUpdateColumnExpression expr: updateStmt.getColumnExpressions())
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      pr.print(updateStmt.getTable());
      pr.print('.');
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    if (updateStmt.getFilterExpression() != null)
    {
      pr.println();
      spaces.generate(pr, indent);
      pr.print("WHERE ");
      generateFilterExpression(pr, updateStmt.getFilterExpression());
    }
  }
  
  /**
   * Generates an insert statement
   * @param pr the writer to write to
   * @param insert the insert statement
   */
  public final void generateInsert(PrintWriter pr, SqlInsertWithValues insert)
  {
    generateInsertInto(pr, insert);
    pr.append(" VALUES (");
    boolean first = true;
    for (SqlLiteral value : insert.getValues())
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      generateValue(pr, value.getValue());
    }
    pr.append(")");
    delimiter.generate(pr);
    pr.append("\n\n");
  }
  
  public final void generateInsertWithSelect(PrintWriter pr, SqlInsertWithSelect insert)
  {
    generateInsertInto(pr, insert);
    pr.append("\n");
    generateSelect(pr, insert.getSelect(), 0);
    delimiter.generate(pr);
    pr.append("\n\n");
  }
  
  private void generateInsertInto(PrintWriter pr, SqlInsert insert)
  {
    pr.append("INSERT INTO ");
    pr.append(insert.getTable());
    pr.append(" (");
    pr.append(StringUtils.join(insert.getColumns(), ", "));
    pr.append(")");
  }

  
  protected final void generateFilterExpression(PrintWriter pr, SqlSimpleExpr filterExpression)
  {
    if (filterExpression instanceof SqlBinaryRelation)
    {
      generateSqlAtom(pr, ((SqlBinaryRelation)filterExpression).getFirst());
      pr.print(' ');
      pr.print(((SqlBinaryRelation)filterExpression).getOperator());
      pr.print(' ');
      generateSqlAtom(pr, ((SqlBinaryRelation)filterExpression).getSecond());      
    }
    else if (filterExpression instanceof SqlLogicalExpression)
    {
      generateFilterExpression(pr, ((SqlLogicalExpression)filterExpression).getFirst());
      pr.print(' ');
      pr.print(((SqlLogicalExpression)filterExpression).getOperator());
      pr.print(' ');
      generateFilterExpression(pr, ((SqlLogicalExpression)filterExpression).getSecond());
    }
    else if (filterExpression instanceof SqlNot)
    {
      pr.print("NOT ");
      generateFilterExpression(pr, ((SqlNot)filterExpression).getExpression());      
    }
    else if (filterExpression instanceof SqlParent)
    {
      pr.print('(');
      generateFilterExpression(pr, ((SqlParent)filterExpression).getExpression());
      pr.print(')');
    }
  }
  
  private void generateSqlAtom(PrintWriter pr, SqlAtom expression)
  {
    generateSqlAtom(pr, expression, SqlArtifact.UNDEFINED);
  }
  
  /**
   * Generates a sql atom
   * @param pr
   * @param expression
   * @param artifact
   * @throws MetaException 
   */
  private void generateSqlAtom(PrintWriter pr, SqlAtom expression, SqlArtifact artifact)
  {
    if (expression instanceof SqlFullQualifiedColumnName)
    {
      identifiers.generateFullQualifiedColumnName(pr, (SqlFullQualifiedColumnName)expression);
    }
    else if (expression instanceof SqlCaseExpr)
    {
      generateSqlCaseExpression(pr, (SqlCaseExpr)expression);
    }
    else if (expression instanceof SqlComplexCaseExpr)
    {
      generateSqlComplexCaseExpression(pr, (SqlComplexCaseExpr) expression);
    }
    else if (expression instanceof SqlLiteral)
    {
      generateValue(pr, ((SqlLiteral)expression).getValue(), artifact);
    }
    else if (expression instanceof SqlFunction)
    {
      generateSqlFunction(pr, (SqlFunction) expression);
    }
    else
    {
      throw new MetaException("Unknown expression "+expression);
    }
  }
  
  /**
   * Generates a case expression
   * @param pr
   * @param caseExpr
   */
  private void generateSqlCaseExpression(PrintWriter pr, SqlCaseExpr caseExpr)
  {
    pr.print("CASE ");
    identifiers.generateFullQualifiedColumnName(pr, caseExpr.getColumnName());
    
    for (SqlWhenThen whenThen : caseExpr.getWhenThenList())
    {
      pr.print(' ');
 
      pr.print("WHEN ");
      generateValue(pr, whenThen.getLiteral());
      pr.print(" THEN ");
      identifiers.generateFullQualifiedColumnName(pr, whenThen.getColumnName());
    }
    pr.print(" END");
  }
  
  /**
   * Generates a complex case expression
   * @param pr
   * @param caseExpr
   */
  private void generateSqlComplexCaseExpression(PrintWriter pr, SqlComplexCaseExpr caseExpr)
  {
    pr.print("CASE");
    for (SqlComplexWhenThen whenThen : caseExpr.getWhenThenList())
    {
      pr.print(' ');
      pr.print("WHEN ");
      generateFilterExpression(pr, whenThen.getCondition());
      pr.print(" THEN ");
      generateSqlAtom(pr, whenThen.getAction());
    }
    if (caseExpr.getElseAction() != null)
    {
      pr.print(" ELSE ");
      generateSqlAtom(pr, caseExpr.getElseAction());
    }
    pr.print(" END");
  }
  
  private void generateSqlFunction(PrintWriter pr, SqlFunction function)
  {
    function = convertFunction(function);
    pr.print(function.getName());
    pr.print("(");
    boolean first = true;
    for(SqlAtom argument : function.getArguments())
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      generateSqlAtom(pr, argument);
    }
    pr.print(")");
  }
  
  protected SqlFunction convertFunction(SqlFunction function)
  {
    return function;
  }
  

  public final void generateValue(PrintWriter pr, Object value)
  {
    generateValue(pr, value, SqlArtifact.UNDEFINED);
  }
  
  /**
   * Generates a value
   * @param pr the writer
   * @param value the value to generate
   * @param artifact 
   */
  private void generateValue(PrintWriter pr, Object value, SqlArtifact artifact)
  {
    if (value == SqlNull.getInstance())
    {
      generateNULL(pr, artifact);
    }
    else if (value instanceof String)
    {
      pr.append("'");
      pr.append(value.toString());
      pr.append("'");
    }
    else
    {
      pr.append(value.toString());
    }
  }
  
  /**
   * Generate NULL value 
   * @param pr
   * @param artifact
   */
  protected void generateNULL(PrintWriter pr, @SuppressWarnings("unused") SqlArtifact artifact)
  {
    pr.append("NULL");
  }
  
  final void generateSelect(PrintWriter pr, SqlSelect select, int indent)
  {
    boolean first = true;
    spaces.generate(pr, indent);
    pr.println("SELECT");
    for (SqlSelectExpression expression : select.getExpressions())
    {
      if (!first)
      {
        pr.println(",");
      }
      first = false;
      spaces.generate(pr, indent+2);
      generateViewExpression(pr, expression);      
    }
    pr.println();
    spaces.generate(pr, indent);
    pr.print("FROM ");
    first = true;
    for (SqlJoinTable joinTable : select.getJoinTables())
    {
      if (!first )
      {
        if (joinTable.getJoinKind() != null)
        {
          pr.print("\n    ");
          pr.print(joinTable.getJoinKind());
          pr.print(" ");
        }
        else
        {
          pr.print(",\n    ");
        }
      }
      first = false;
      identifiers.generateSqlTableId(pr, joinTable.getTable());
      if (joinTable.getJoinCondition() != null)
      {
        pr.print(" ON ");
        generateFilterExpression(pr, joinTable.getJoinCondition());
      }
    }
    if (select.getCondition() != null)
    {
      pr.println();
      spaces.generate(pr, indent);
      pr.print("WHERE ");
      generateFilterExpression(pr, select.getCondition());
    }
  }
  
  private void generateViewExpression(PrintWriter pr, SqlSelectExpression expression)
  {
    generateSqlAtom(pr, expression.getExpression(), expression);
  }
}
