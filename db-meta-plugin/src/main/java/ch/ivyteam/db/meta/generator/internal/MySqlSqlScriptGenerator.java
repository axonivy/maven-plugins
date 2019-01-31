package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ivyteam.db.meta.model.internal.MetaException;
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
 * Generates the sql script for MySql database systems
 * @author rwei
 */
public class MySqlSqlScriptGenerator extends SqlScriptGenerator
{
  private static final int MAX_INDEX_SIZE_IN_BYTES  = 767;
  private static final Set<String> RESERVED_WORDS_MYSQL = new HashSet<>(Arrays.asList("SYSTEM"));
  
  /** Database System */
  public static final String MYSQL = String.valueOf("MySql");
  
  private static final String DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_ForeignKey_Constraint";
  public static final String INDEX_COLUMN_LENGTH = "IndexColumnLength";
                                                    
  /**
   * @see SqlScriptGenerator#generateDataType(PrintWriter, DataType)
   */
  @Override
  protected void generateDataType(PrintWriter pr, DataType dataType)
  {
    switch(dataType)
    {
      case CLOB:
        pr.append("TEXT");
        break;
      case BLOB:
        pr.append("MEDIUMBLOB");
        break;
      default:
        super.generateDataType(pr, dataType);
        break;
    }   
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#isForeignKeyReferenceInColumnDefinitionSupported()
   */
  @Override
  public boolean isForeignKeyReferenceInColumnDefinitionSupported()
  {
    return false;
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateTableStorage(java.io.PrintWriter, ch.ivyteam.db.meta.model.internal.SqlTable)
   */
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
  {
    pr.append(" ENGINE={0}");
  }

  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseComment()
   */
  @Override
  protected String getDatabaseComment()
  {
    return "MySQL";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateComment(java.io.PrintWriter)
   */
  @Override
  protected void generateComment(PrintWriter pr)
  {
    pr.append("# ");
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getDatabaseSystemNames()
   */
  @Override
  protected List<String> getDatabaseSystemNames()
  {
    return Arrays.asList(MYSQL);
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#getRowTriggerOldVariableName()
   */
  @Override
  protected String getRowTriggerOldVariableName()
  {
    return "OLD";
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator#generateIdentifierQuote(java.io.PrintWriter)
   */
  @Override
  protected void generateIdentifierQuote(PrintWriter pr)
  {
    pr.print("`");
  }
  
 /**
  * Generates a table row delete trigger. Subclasses may override this method.
  * @param pr the print writer to generate to
  * @param table the table which triggers the trigger
  * @param triggerStatements the statements that have to be executed by the trigger
  * @param recursiveTrigger flag indicating if this trigger is recursive
 * @throws MetaException 
  */
  @Override
  protected void generateForEachRowDeleteTrigger(PrintWriter pr, SqlTable table,
          List<SqlDmlStatement> triggerStatements, boolean recursiveTrigger) throws MetaException
  {
    pr.print("CREATE TRIGGER ");
    if (isDatabaseSystemHintSet(table, DELETE_TRIGGER_NAME))
    {
      pr.println(getDatabaseSystemHintValue(table, DELETE_TRIGGER_NAME));
    }
    else
    {
      pr.print(table.getId());
      pr.println("DeleteTrigger");
    }
    pr.print("AFTER DELETE ON ");
    pr.println(table.getId());
    pr.println("FOR EACH ROW");
    pr.println("BEGIN");
    for (SqlDmlStatement stmt : triggerStatements)
    {
      generateDmlStatement(pr, stmt, 2);
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
    pr.println("END");
    generateDelimiter(pr);
  }
  
  /**
   * Generates the update statement
   * @param pr the print writer
   * @param updateStmt the update statement
   * @param insets the insets
   */
  @Override
  protected void generateUpdateStatement(PrintWriter pr, SqlUpdate updateStmt, int insets)
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
      pr.print(expr.getColumnName());
      pr.print('=');
      pr.print(expr.getExpression());
    }
    pr.println();
    writeSpaces(pr, insets);
    pr.print("WHERE ");
    pr.print(updateStmt.getFilterExpression());
  }
  
  /**
   * Could overridden from different database types
   * @param pr
   * @param newColumn 
   * @param newTable
   * @param oldColumn 
   * @throws MetaException 
   */
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn) throws MetaException
  {
    GenerateAlterTableUtil.generateAlterTableChangeColumnWithDefaultAndNullConstraints(pr, this, newColumn, newTable, "MODIFY");
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
    pr.print(" DROP INDEX ");
    generateIdentifier(pr, unique.getColumns().get(0));
  }
  
  @Override
  public void generateAlterTableDropForeignKey(PrintWriter pr, SqlTable table, SqlForeignKey foreignKey,
          List<String> createdTemporaryStoredProcedures)
  {
    generateDropForeignKeyConstraintStoredProcedure(pr, createdTemporaryStoredProcedures);
    pr.print("CALL ");
    pr.print(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
    pr.print("(");
    pr.print("SCHEMA()"); 
    pr.print(", '");
    pr.print(table.getId()); 
    pr.print("', '");
    pr.print(foreignKey.getColumnName());
    pr.print("')");    
    generateDelimiter(pr);
    pr.println(); 
  }

  private void generateDropForeignKeyConstraintStoredProcedure(PrintWriter pr,
          List<String> createdTemporaryStoredProcedures)
  {
    if (!createdTemporaryStoredProcedures.contains(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE))
    {
      createdTemporaryStoredProcedures.add(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
      
      generateCommentLine(pr, "Store Procedure to drop a foreign key constraint");

      pr.print("CREATE PROCEDURE ");
      pr.print(DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE);
      pr.println("(fk_schema VARCHAR(64), fk_table VARCHAR(64), fk_column VARCHAR(64))");
      pr.println("BEGIN");
      pr.println("  WHILE EXISTS(");
      pr.println("    SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE");
      pr.println("    WHERE REFERENCED_COLUMN_NAME IS NOT NULL");
      pr.println("      AND TABLE_SCHEMA = fk_schema");
      pr.println("      AND TABLE_NAME = fk_table");
      pr.println("      AND COLUMN_NAME = fk_column");
      pr.println("  ) ");
      pr.println("  DO");
      pr.println("    BEGIN");
      pr.println("      SET @sqlstmt = (");
      pr.println("        SELECT CONCAT('ALTER TABLE ',TABLE_SCHEMA,'.',TABLE_NAME,' DROP FOREIGN KEY ',CONSTRAINT_NAME)");
      pr.println("        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE");
      pr.println("        WHERE REFERENCED_COLUMN_NAME IS NOT NULL");
      pr.println("          AND TABLE_SCHEMA = fk_schema");
      pr.println("          AND TABLE_NAME = fk_table");
      pr.println("          AND COLUMN_NAME = fk_column");
      pr.println("        LIMIT 1");
      pr.println("      );");
      pr.println("      PREPARE stmt1 FROM @sqlstmt;");
      pr.println("      EXECUTE stmt1;");
      pr.println("    END;");
      pr.println("  END WHILE;");
      pr.println("END;");
      generateDelimiter(pr);
      pr.println();
      pr.println();
    }
  }
  
  @Override
  public void generateDropIndex(PrintWriter pr, SqlTable table, SqlIndex index)
  {
    pr.print("DROP INDEX ");
    generateIdentifier(pr, getIndexName(index));
    pr.println(); 
    pr.print("ON ");
    generateIdentifier(pr, table.getId());
    generateDelimiter(pr);
    pr.println(); 
  }
  
  @Override
  protected boolean generateIndexInTable(PrintWriter pr, SqlTable table, SqlIndex index)
  {
    Map<SqlTableColumn, MemoryInfo> columns = reduceIndexColumnSizes(table, index);
    if (columns != null)
    {
      return generateIndexInTable(pr, index, columns);
    }
    else
    {
      return super.generateIndexInTable(pr, table, index);
    }
  }
  
  @Override
  public void generateIndex(PrintWriter pr, SqlTable table, SqlIndex index)
  {
    Map<SqlTableColumn, MemoryInfo> columns = reduceIndexColumnSizes(table, index);
    generateIndex(pr, table, index, columns);
  }

  private Map<SqlTableColumn, MemoryInfo> reduceIndexColumnSizes(SqlTable table, SqlIndex index)
  {
    Map<SqlTableColumn, MemoryInfo> bytesPerColumn = getIndexColumnBytes(table, index);
    int bytes = bytesPerColumn.values().stream().mapToInt(info -> info.bytes).sum();
    
    if (bytes > MAX_INDEX_SIZE_IN_BYTES)
    {
      reduce(index, bytesPerColumn, bytes-MAX_INDEX_SIZE_IN_BYTES);
      return bytesPerColumn;
    }
    return bytesPerColumn;
  }

  private Map<SqlTableColumn, MemoryInfo> getIndexColumnBytes(SqlTable table, SqlIndex index)
  {
    Map<SqlTableColumn, MemoryInfo> bytesPerColumn = new LinkedHashMap<>();
    Map<SqlTableColumn, Integer> columnLengthHints = getColumnLengthHints(table, index);
    for (String column : index.getColumns())
    {
      SqlTableColumn col = table.findColumn(column);
      int length = col.getDataType().getLength();
      if (columnLengthHints.containsKey(col))
      {
        length = columnLengthHints.get(col);
      }
      int columnBytes = getBytes(col.getDataType().getDataType(), length); 
      bytesPerColumn.put(col, new MemoryInfo(length, columnBytes));
    }
    return bytesPerColumn;
  }
  
  private Map<SqlTableColumn, Integer> getColumnLengthHints(SqlTable table, SqlIndex index)
  {
    String lengthHintStr = getDatabaseSystemHintValue(index,  INDEX_COLUMN_LENGTH);
    if (lengthHintStr == null)
    {
      return Collections.emptyMap();
    }

    int[] lengthHints = Arrays.stream(lengthHintStr.split(",")).mapToInt(Integer::parseInt).toArray();
    if (lengthHints.length != index.getColumns().size())
    {
      throw new IllegalArgumentException(
              "Hint "+INDEX_COLUMN_LENGTH+" on index "+index.getId()+" in table "+table.getId()+
              " contains "+lengthHints.length+" arguments expected are "+index.getColumns().size());
    }
    Map<SqlTableColumn, Integer> columnLengthHints = new HashMap<>();
    for (int pos = 0; pos < lengthHints.length; pos++)
    {
      SqlTableColumn column = table.findColumn(index.getColumns().get(pos));
      columnLengthHints.put(column, lengthHints[pos]);
    }
    return columnLengthHints;
  }

  private void reduce(SqlIndex index, Map<SqlTableColumn, MemoryInfo> bytesPerColumn, int bytesToReduce)
  {
    SqlTableColumn maxColumn=null;
    MemoryInfo maxMemory=null;
    
    for (SqlTableColumn column : bytesPerColumn.keySet())
    {
      MemoryInfo memory = bytesPerColumn.get(column);
      if (maxMemory == null || maxMemory.bytes < memory.bytes && canReduce(column.getDataType().getDataType()))
      {
        maxMemory = memory;
        maxColumn = column;
      }
    }
    if (maxColumn == null || maxMemory == null)
    {
      throw new IllegalStateException("Not possible to reduce size of index "+ index.getId());
    }
    maxMemory.length = maxMemory.length - bytesToCharLength(bytesToReduce);
    if (maxMemory.length < 100)
    {
      throw new IllegalStateException("Not possible to reduce size of index "+ index.getId());
    }
  }


  private int bytesToCharLength(int bytes)
  {
    int length = bytes/3;
    if (bytes % 3 != 0)
    {
      length = length+1;
    }
    return length;
  }

  private void generateIndex(PrintWriter pr, SqlTable table, SqlIndex index,
          Map<SqlTableColumn, MemoryInfo> bytesPerColumn)
  {
    pr.print("CREATE INDEX ");
    generateIdentifier(pr, getIndexName(index));
    pr.println();
    pr.print("ON ");
    pr.print(table.getId());
    pr.print(" (");
    generateColumnList(pr, bytesPerColumn);
    pr.print(")");
    generateDelimiter(pr);
    pr.println();
    pr.println();    
  }
  
  private boolean generateIndexInTable(PrintWriter pr, SqlIndex index,
          Map<SqlTableColumn, MemoryInfo> bytesPerColumn)
  {
    writeSpaces(pr, 2);
    pr.print("INDEX ");
    generateIdentifier(pr, getIndexName(index));
    pr.print(' ');
    pr.print('(');
    generateColumnList(pr, bytesPerColumn);
    pr.print(')');
    return true;
  }

  
  /**
   * Generates a column list
   * @param pr the writer
   * @param columns the columns to write
   */
  protected void generateColumnList(PrintWriter pr, Map<SqlTableColumn, MemoryInfo> columns)
  {
    boolean first = true;
    for (SqlTableColumn column : columns.keySet())
    {
      if (!first)
      {
        pr.append(", ");
      }
      first = false;
      generateIdentifier(pr, column.getId());
      MemoryInfo memoryInfo = columns.get(column);
      if (column.getDataType().getLength() != memoryInfo.length)
      {
        pr.append('(');
        pr.append(Integer.toString(memoryInfo.length));
        pr.append(')');
      }
    }
  }

  private boolean canReduce(DataType dataType)
  {
    return dataType.equals(DataType.VARCHAR) || dataType.equals(DataType.CHAR);
  }

  private int getBytes(DataType dataType, int length)
  {
    switch(dataType)
    {
      case VARCHAR:
      case CHAR:
        return length*3;
      case BIGINT:
        return 8;
      case BIT:
        return 1;
      case INTEGER:
        return 4;
      case FLOAT:
        return 4;
      case DATE:
        return 3;  
      case TIME:
        return 3;
      case DATETIME:
        return 8;
      case DECIMAL:
      case NUMBER:
        return length*4/9+4;
      case BLOB:
        return 0x1000000;
      case CLOB:
        return 0x10000;
    }
    return 0;
  }
  
  @Override
  protected boolean isReservedSqlKeyword(String identifier)
  {
    if (RESERVED_WORDS_MYSQL.contains(identifier.toUpperCase()))
    {
      return true;
    }
    return super.isReservedSqlKeyword(identifier);
  }
  
  @Override
  public RecreateOptions getRecreateOptions()
  {
    RecreateOptions options = super.getRecreateOptions();
    options.foreignKeysOnAlterTable = true;
    return options;
  }
  
  private static class MemoryInfo
  {
    public MemoryInfo(int length, int bytes)
    {
      this.length = length;
      this.bytes = bytes;      
    }
    
    int length;
    int bytes;
  }
}