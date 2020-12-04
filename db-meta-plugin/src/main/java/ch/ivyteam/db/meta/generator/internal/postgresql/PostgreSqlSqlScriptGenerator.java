package ch.ivyteam.db.meta.generator.internal.postgresql;

import java.io.PrintWriter;
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
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for Postgre SQL database systems
 * @author rwei
 * @since 01.10.2009  
 */
public class PostgreSqlSqlScriptGenerator extends SqlScriptGenerator
{
  public static final String POSTGRESQL = String.valueOf("PostgreSql");
  public static final String CAST = "CAST";
  
  public PostgreSqlSqlScriptGenerator()
  {
    super(POSTGRESQL, Delimiter.STANDARD, Identifiers.STANDARD, Comments.STANDARD);
  }
  
  @Override
  protected DmlStatements createDmlStatementsGenerator(DbHints hints, Delimiter delim, Identifiers ident)
  {
    return new PostgreSqlDmlStatements(hints, delim, ident);
  }

  @Override
  protected Triggers createTriggersGenerator(DbHints hints, Delimiter delim, DmlStatements dmlStmts, ForeignKeys fKeys)
  {
    return new PostgreSqlTriggers(hints, delim, dmlStmts, fKeys);
  }
  
  @Override
  protected ForeignKeys createForeignKeysGenerator(DbHints hints, Delimiter delim, Identifiers ident, Comments cmmnts)
  {
    return new PostgreSqlForeignKeys(hints, delim, ident, cmmnts);
  }
  
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
    
  @Override
  protected boolean isIndexInTableSupported()
  {
    return false;
  }
  
  @Override
  public String dbName()
  {
    return "Postgre SQL";
  }
  
  @Override
  public void generateIndex(PrintWriter pr,
          SqlTable table, SqlIndex index)
  {
    pr.print("CREATE INDEX ");
    identifiers.generate(pr, getIndexName(index));
    pr.println();
    pr.print("ON ");
    identifiers.generate(pr, table.getId());
    pr.print(" (");
    generateIndexColumnList(pr, table, index.getColumns());
    pr.print(")");
    delimiter.generate(pr);
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
      identifiers.generate(pr, column);
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
  
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn)
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
  
  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }
  
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table,
          SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.print(" DROP CONSTRAINT ");
    StringBuilder constraint = new StringBuilder(128);
    constraint.append(table.getId());
    for (String column : unique.getColumns())
    {
      constraint.append("_");
      constraint.append(column);      
    }
    constraint.append("_Key");
    identifiers.generate(pr, constraint.toString());
    pr.print(" CASCADE");
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
}
