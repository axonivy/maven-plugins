package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

/**
 * Generates the sql script for Postgre SQL database systems
 * @author rwei
 * @since 01.10.2009
 */
public class PostgreSqlSqlScriptGenerator extends SqlScriptGenerator
{
  /** The postgre sql database system */
  public static final String POSTGRESQL = String.valueOf("PostgreSql");
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateDataType(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlDataType.DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case DATETIME:
        pr.print("TIMESTAMP");
        break;
      case CLOB:
        pr.print("TEXT");
        break;
      case BIT:
        pr.print("DECIMAL(1)");
        break;
      case NUMBER:
        pr.print("DECIMAL");
        break;
      case BLOB:
        pr.print("BYTEA");
        break;
      default:
        super.generateDataType(pr, dataType);
    }
  }
    
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isIndexInTableSupported()
   */
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "Postgre SQL";
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(POSTGRESQL);
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachRowDeleteTrigger(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, java.util.List, boolean)
   */
  @Override
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger) throws MetaException
  {
    // 1. create function
    pr.print("CREATE FUNCTION ");
    pr.print(table.getId());
    pr.print("DeleteTriggerFunc()");
    pr.println(" RETURNS TRIGGER AS '");
    writeSpaces(pr, 2);
    pr.println("BEGIN");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 4);
      generateDelimiter(pr);
      pr.println();
      pr.println();      
    }
    writeSpaces(pr, 4);
    pr.print("RETURN OLD");
    generateDelimiter(pr);
    pr.println();
    writeSpaces(pr, 2);
    pr.print("END");
    generateDelimiter(pr);
    pr.println();
    pr.print("' LANGUAGE plpgsql");
    generateDelimiter(pr);
    pr.println();
    pr.println();

    // 2. create trigger that uses function
    pr.print("CREATE TRIGGER ");
    pr.print(table.getId());
    pr.println("DeleteTrigger AFTER DELETE");
    pr.print("ON ");
    pr.print(table.getId());
    pr.println(" FOR EACH ROW");
    pr.print("EXECUTE PROCEDURE ");
    pr.print(table.getId());
    pr.print("DeleteTriggerFunc()");
    generateDelimiter(pr);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "OLD";
  }
  
  @Override
  public void generateIndex(PrintWriter pr,
          SqlTable table, SqlIndex index)
  {
    pr.print("CREATE INDEX ");
    generateIdentifier(pr, getIndexName(index));
    pr.println();
    pr.print("ON ");
    generateIdentifier(pr, table.getId());
    pr.print(" (");
    generateIndexColumnList(pr, table, index.getColumns());
    pr.print(")");
    generateDelimiter(pr);
    pr.println();
    pr.println();    
  }
  
  private void generateIndexColumnList(PrintWriter pr, SqlTable table, List<String> columns)
  {
    boolean first = true;
    for (String column : columns)
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      generateIdentifier(pr, column);
      if (isVarCharColumn(table, column))
      {
        pr.append(" ");
        pr.append("varchar_pattern_ops");
      }
    }
  }

  private boolean isVarCharColumn(SqlTable table, String column)
  {
    return table.findColumn(column).getDataType().getDataType() == SqlDataType.DataType.VARCHAR;
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddForeignKey(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlForeignKey)
   */
  @Override
  public void generateAlterTableAddForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey)
          throws MetaException
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" ADD FOREIGN KEY (");
    pr.print(foreignKey.getColumnName());
    pr.print(")");
    generateReference(pr, foreignKey.getReference(), foreignKey);
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateUpdateStatement(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlUpdate, int)
   */
  @Override
  protected void generateUpdateStatement(PrintWriter pr, SqlUpdate updateStmt, int insets)
          throws MetaException
  {
    boolean first = true;
    writeSpaces(pr, insets);
    pr.print("UPDATE ");
    pr.println(updateStmt.getTable());
    writeSpaces(pr, insets);
    pr.print("SET ");
    first = true;
    for (SqlUpdateColumnExpression expr: updateStmt.getColumnExpressions())
    {
      if (!first)
      {
        pr.print(", ");
      }
      first = false;
      generateIdentifier(pr, expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    generateFilterExpression(pr, updateStmt.getFilterExpression());
  }
  
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAlterColumn(PrintWriter, SqlTableColumn, SqlTable, SqlTableColumn)
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn) throws MetaException
  {
    boolean changed = false;
    if (!Objects.equals(newColumn.getDataType(), oldColumn.getDataType()))
    {
      GenerateAlterTableUtil.generateAlterTableAlterColumnType(pr, this, newColumn, newTable, "ALTER COLUMN", "TYPE");
      if (newColumn.getDefaultValue() != null)
      {
        // Seems the default value is dropped if we change the data type. -> reset it again
        pr.println();
        GenerateAlterTableUtil.generateAlterTableAlterColumnDefault(pr, this, newColumn, newTable, "ALTER COLUMN");
      }
      changed = true;
    }
    if (newColumn.isCanBeNull() != oldColumn.isCanBeNull())
    {
      if (changed)
      {
        pr.println();
      }
      GenerateAlterTableUtil.generateAlterTableAlterColumnNotNull(pr,  this,  newColumn, newTable, "ALTER COLUMN", "SET NOT NULL", "DROP NOT NULL");
      changed = true;
    }
    if (!changed)
    {
      throw new IllegalArgumentException("Only changing of the data type and NOT NULL is supported");
    }
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.print(" DROP CONSTRAINT ");
    StringBuilder constraint = new StringBuilder(128);
    constraint.append(table.getId());
    for (String column : unique.getColumns())
    {
      constraint.append("_");
      constraint.append(column);      
    }
    constraint.append("_Key");
    generateIdentifier(pr, constraint.toString());
    pr.print(" CASCADE");
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,List<String> createdTemporaryStoredProcedures)
  {
    String foreignKeyName = table.getId()+"_"+StringUtils.removeStart(foreignKey.getId(), "FK_")+"_fkey";
    
    pr.print("ALTER TABLE ");
    generateIdentifier(pr, table.getId());
    pr.println(" DROP");
    pr.print(" CONSTRAINT ");
    pr.println(foreignKeyName);
    generateDelimiter(pr);
    pr.println();
    pr.println();
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
}
