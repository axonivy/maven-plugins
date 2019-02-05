package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlArtifact;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlDmlStatement;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
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
  public static final String POSTGRESQL = String.valueOf("PostgreSql");
  public static final String CAST = "CAST";
  
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
  protected String getDatabaseComment()
  {
    return "Postgre SQL";
  }

  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(POSTGRESQL);
  }

  @Override
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger) throws MetaException
  {
    String functionName = table.getId() + "DeleteTriggerFunc()";

    // Drop already existing functions (used for regeneration)
    pr.print("DROP FUNCTION IF EXISTS ");
    pr.print(functionName);
    generateDelimiter(pr);
    pr.println();
    pr.println();

    // create function
    pr.print("CREATE FUNCTION ");
    pr.print(functionName);
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

    // create trigger that uses function
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
  
  @Override
  public void generateNonMetaDiffChangesPost(PrintWriter pr, SqlMeta metaDefinitionFrom, SqlMeta metaDefinitionTo, int newVersionId)
  {
    if (newVersionId == 48)
    {
      recreateVarCharIndices(pr, metaDefinitionFrom);
    }
    super.generateNonMetaDiffChangesPost(pr, metaDefinitionFrom, metaDefinitionTo, newVersionId);
  }

  private void recreateVarCharIndices(PrintWriter pr, SqlMeta metaDefinitionTo)
  {
    for (SqlTable table : metaDefinitionTo.getArtifacts(SqlTable.class))
    {
      for (SqlIndex index : getIndexes(table))
      {
        if (hasVarCharColumn(table, index))
        {
          generateCommentLine(pr, "Drop and recreate index with varchar_pattern_ops to get better LIKE 'prefix%' performance");
          generateDropIndex(pr, table, index);
          pr.println();
          generateIndex(pr, table, index);
          pr.println();
        }
      }
    }
  }

  private boolean hasVarCharColumn(SqlTable table, SqlIndex index)
  {
    return index.getColumns()
            .stream()
            .filter(column -> this.isVarCharColumn(table, column))
            .findAny()
            .isPresent();
  }

  private boolean isVarCharColumn(SqlTable table, String column)
  {
    return table.findColumn(column).getDataType().getDataType() == SqlDataType.DataType.VARCHAR;
  }

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
  
  @Override
  public void generateDropTrigger(PrintWriter pr, SqlTable table, SqlMeta metaDefinitionFrom)
  {
    pr.write("DROP TRIGGER ");
    generateTriggerName(pr, table);
    pr.append(" ON ");
    pr.append(table.getId());
    generateDelimiter(pr);
    pr.println(); 
  }
  
  @Override
  protected void generateNULL(PrintWriter pr, SqlArtifact artifact)
  {
    super.generateNULL(pr, artifact);
    if (isDatabaseSystemHintSet(artifact, CAST))
    {
      generateDatabaseManagementHintValue(pr, artifact, CAST);
    }
  }
}
