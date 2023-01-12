package ch.ivyteam.db.meta.generator.internal.oracle;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.GenerateAlterTableUtil;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
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
  public static final String ORACLE = String.valueOf("Oracle");

  /** 
   * System Database Hint ConvertEmptyStringToNull:
   * Columns are generated with NULL instead of NOT NULL.
   * Persistency layer will write "" values as NULL.
   * Persistency layer will read NULL values as "". 
   */
  public static final String CONVERT_EMPTY_STRING_TO_NULL = "ConvertEmptyStringToNull";
  private static final Identifiers IDENTIFIERS = new OracleIdentifiers();

  private boolean alterTable;
  
  public OracleSqlScriptGenerator()
  {
    super(ORACLE, Delimiter.STANDARD, IDENTIFIERS, Comments.STANDARD);
  }
  
  @Override
  protected DmlStatements createDmlStatementsGenerator(DbHints hints, Delimiter delim, Identifiers ident)
  {
    return new OracleDmlStatements(hints, delim, ident);
  }
  
  @Override
  protected Triggers createTriggersGenerator(DbHints hints, Delimiter delim, DmlStatements dmlStmts, ForeignKeys fKeys)
  {
    return new OracleTriggers(hints, delim, dmlStmts, fKeys);
  }
  
  @Override
  protected ForeignKeys createForeignKeysGenerator(DbHints hints, Delimiter delim, Identifiers ident, Comments cmmnts)
  {
    return new OracleForeignKeys(hints, delim, ident, cmmnts);
  }
  
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
        pr.append("TIMESTAMP");
        break;
      default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  @Override
  public void generateIndex(PrintWriter pr, SqlTable table, SqlIndex index)
  {
    pr.print("CREATE INDEX ");
    pr.println(getIndexName(index));
    pr.print("ON ");
    pr.print(table.getId());
    pr.print(" (");
    generateColumnList(pr, index.getColumns());
    pr.print(")");
    pr.println();
    pr.print("TABLESPACE ${tablespaceName}");
    delimiter.generate(pr);
    pr.println();
    pr.println();    
  }
  
  @Override
  protected boolean isNullBeforeDefaultConstraint()
  {
    return false;
  }
  
  @Override
  protected void generateNullConstraint(PrintWriter pr, boolean canBeNull, SqlTableColumn column)
  {
    if (dbHints.CONVERT_EMPTY_STRING_TO_NULL.isSet(column))
    {
      canBeNull = true; 
    }
    super.generateNullConstraint(pr, canBeNull, column);
  }
  
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
  {
    pr.append("\nTABLESPACE ${tablespaceName}");
  }

  @Override
  public String dbName()
  {
    return ORACLE;
  }

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
    dmlStatements.generateUpdate(pr, updateStmt , 0);
    delimiter.generate(pr);
  }
  
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    alterTable=true;
    try
    {
      GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD");
    }
    finally
    {
      alterTable=false;
    }
  }
  
  @Override
  protected void generateDefaultValue(PrintWriter pr, SqlTableColumn column)
  {
    if (alterTable)
    {
      if (column.getDefaultValue() != null && "".equals(column.getDefaultValue().getValue()) && !column.isCanBeNull())
      {
        pr.append(" DEFAULT ");
        dmlStatements.generateValue(pr, " ");
        return;
      }
    }
    super.generateDefaultValue(pr, column);
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
    options.foreignKeysOnAlterTable=true;
    return options;
  }
}
