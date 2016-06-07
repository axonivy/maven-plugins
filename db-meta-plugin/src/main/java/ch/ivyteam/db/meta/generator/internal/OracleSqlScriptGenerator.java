package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDelete;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlFunction;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;
import ch.ivyteam.db.meta.model.internal.SqlUpdate;
import ch.ivyteam.db.meta.model.internal.SqlUpdateColumnExpression;

/**
 * Generates the sql script for Oracle database systems
 * @author rwei
 */
public class OracleSqlScriptGenerator extends SqlScriptGenerator
{
  /** Oracle */
  public static final String ORACLE = String.valueOf("Oracle");

  /** 
   * System Database Hint ConvertEmptyStringToNull:
   * Columns are generated with NULL instead of NOT NULL.
   * Persistency layer will write "" values as NULL.
   * Persistency layer will read NULL values as "". 
   */
  public static final String CONVERT_EMPTY_STRING_TO_NULL = "ConvertEmptyStringToNull";
  
  /**
   * @see SqlScriptGenerator#generateDataType(PrintWriter, DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case BIT:
        pr.append("NUMBER(1)");
        break;
      case INTEGER:
        pr.append("NUMBER(10)");
        break;
      case BIGINT:
        pr.append("NUMBER(20)");
        break;
      case VARCHAR:
        pr.append("VARCHAR2");
        break;
      case DATETIME:
      case DATE:
      case TIME:
        pr.append("DATE");
        break;
      default:
        super.generateDataType(pr, dataType);
        break;
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
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIndex(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlIndex)
   */
  @Override
  public void generateIndex(PrintWriter pr, SqlTable table, SqlIndex index)
  {
    pr.print("CREATE INDEX ");
    if (isDatabaseSystemHintSet(index, INDEX_NAME))
    {
      pr.println(getDatabaseSystemHintValue(index, INDEX_NAME));
    }
    else
    {
      pr.println(index.getId());
    }
    pr.print("ON ");
    pr.print(table.getId());
    pr.print(" (");
    generateColumnList(pr, index.getColumns());
    pr.print(")");
    pr.println();
    pr.print("TABLESPACE {0}");
    generateDelimiter(pr);
    pr.println();
    pr.println();    
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isNullBeforeDefaultConstraint()
   */
  @Override
  protected boolean isNullBeforeDefaultConstraint()
  {
    return false;
  }
  
  /**
   * @see SqlScriptGenerator#generateNullConstraint(PrintWriter, boolean, SqlTableColumn)
   */
  @Override
  protected void generateNullConstraint(PrintWriter pr, boolean canBeNull, SqlTableColumn column)
  {
    if (isDatabaseSystemHintSet(column, CONVERT_EMPTY_STRING_TO_NULL))
    {
      canBeNull = true; 
    }
    super.generateNullConstraint(pr, canBeNull, column);
  }
  
  /**
   * @see SqlScriptGenerator#generateTableStorage(PrintWriter, SqlTable)
   */
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
  {
    pr.append("\nTABLESPACE {0}");
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return ORACLE;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(ORACLE);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachRowDeleteTrigger(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, java.util.List, boolean)
   */
  @Override
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger) throws MetaException
  {
    super.generateForEachRowDeleteTrigger(pr, table, triggerStatements, recursiveTrigger);
    pr.println();
    generateDelimiter(pr);
    pr.println();
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateForEachStatementDeleteTrigger(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, java.util.List)
   */
  @Override
  protected void generateForEachStatementDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements) throws MetaException
  {
    super.generateForEachStatementDeleteTrigger(pr, table, triggerStatements);
    pr.println();
    generateDelimiter(pr);
    pr.println();    
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIdentifier(java.io.PrintWriter, java.lang.String)
   */
  @Override
  protected void generateIdentifier(PrintWriter pr, String identifier)
  {
    if (isReservedSqlKeyword(identifier))
    {
      identifier = identifier.toUpperCase();
    }
    super.generateIdentifier(pr, identifier);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isReservedSqlKeyword(java.lang.String)
   */
  @Override
  protected boolean isReservedSqlKeyword(String identifier)
  {
    if (identifier.equalsIgnoreCase("State"))
    {
      return false;
    }
    return super.isReservedSqlKeyword(identifier);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateDeleteStatement(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlDelete, int)
   */
  @Override
  protected void generateDeleteStatement(PrintWriter pr, SqlDelete deleteStmt, int insets)
          throws MetaException
  {
    writeSpaces(pr, insets);
    pr.print("DELETE ");
    pr.println(deleteStmt.getTable());
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    generateFilterExpression(pr, deleteStmt.getFilterExpression());
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return ":old";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAlterColumn(PrintWriter, SqlTableColumn, SqlTable, SqlTableColumn)
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable table, SqlTableColumn oldColumn) throws MetaException
  {
    boolean changed = false;
    if (!Objects.equals(newColumn.getDataType(), oldColumn.getDataType()))
    {
      if (EnumSet.of(SqlDataType.DataType.INTEGER, SqlDataType.DataType.NUMBER, SqlDataType.DataType.FLOAT)
              .contains(oldColumn.getDataType().getDataType()) && 
          newColumn.getDataType().getDataType() == SqlDataType.DataType.VARCHAR)
      {
        SqlTableColumn tmpColumn = newColumn.changeId(newColumn.getId()+"_temp");
        generateAlterTableAddColumn(pr, tmpColumn, table);
        pr.println();
        copyValues(pr, table, oldColumn, tmpColumn);
        pr.println();
        GenerateAlterTableUtil.generateAlterTableDropColumn(pr, this, table, oldColumn);
        GenerateAlterTableUtil.renameColumn(pr, this, table, tmpColumn, newColumn);
      }
      else
      {
        GenerateAlterTableUtil.generateAlterTableAlterColumnType(pr, this, newColumn, table, "MODIFY", "");
      }
      changed = true;
    }
    
    if (newColumn.isCanBeNull() != oldColumn.isCanBeNull())
    {
      if (changed)
      {
        pr.println();
      }
      GenerateAlterTableUtil.generateAlterTableAlterColumnNotNull(pr, this, newColumn, table, "MODIFY", "NOT NULL", "NULL");
      changed = true;
    }
    
    if (!changed)
    {
      throw new IllegalArgumentException("Only changing of the data type is supported");
    }
  }

  private void copyValues(PrintWriter pr, SqlTable table, SqlTableColumn numberColumn,
          SqlTableColumn varcharColumn)
  {
    List<SqlUpdateColumnExpression> columnExpressions = Arrays.asList(
            new SqlUpdateColumnExpression(varcharColumn.getId(), 
                    new SqlFunction("to_char", new SqlFullQualifiedColumnName(table.getId(), numberColumn.getId())
    )));
    SqlUpdate updateStmt = new SqlUpdate(
            table.getId(), columnExpressions, null, Collections.emptyList(), null);
    generateUpdateStatement(pr, updateStmt , 0);
    generateDelimiter(pr);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableAddColumn(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTableColumn, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateAlterTableDropUniqueConstraint(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable, ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint, java.util.List)
   */
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    super.generateAlterTableDropUniqueConstraint(pr, table, unique, createdTemporaryStoredProcedures);
    pr.print(" DROP INDEX");
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.uniqueConstraintsOnAlterTable = true;
    return options;
  }
}
