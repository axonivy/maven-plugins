package ch.ivyteam.db.meta.generator.internal.mysql;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.ivyteam.db.meta.generator.internal.Comments;
import ch.ivyteam.db.meta.generator.internal.DbHints;
import ch.ivyteam.db.meta.generator.internal.Delimiter;
import ch.ivyteam.db.meta.generator.internal.DmlStatements;
import ch.ivyteam.db.meta.generator.internal.ForeignKeys;
import ch.ivyteam.db.meta.generator.internal.GenerateAlterTableUtil;
import ch.ivyteam.db.meta.generator.internal.Identifiers;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.Triggers;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlIndex;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlUniqueConstraint;

/**
 * Generates the sql script for MySql database systems
 * @author rwei
 */
public class MySqlSqlScriptGenerator extends SqlScriptGenerator
{
  private static final int MAX_INDEX_SIZE_IN_BYTES  = 767;
  
  /** Database System */
  public static final String MYSQL = String.valueOf("MySql");
  
  static final String DROP_FOREIGN_KEY_CONSTRAINT_STORED_PROCUDRE = "IWA_Drop_ForeignKey_Constraint";
  public static final String INDEX_COLUMN_LENGTH = "IndexColumnLength";
  private static final Identifiers IDENTIFIERS = new Identifiers("`", false, Arrays.asList("SYSTEM"));
  private static final Comments COMMENTS = new Comments("# ");
           
  public MySqlSqlScriptGenerator()
  {
    super(MYSQL, Delimiter.STANDARD, IDENTIFIERS, COMMENTS);
  }
  
  @Override
  protected DmlStatements createDmlStatementsGenerator(DbHints hints, Delimiter delim, Identifiers ident)
  {
    return new MySqlDmlStatements(hints, delim, ident);
  }
  
  @Override
  protected Triggers createTriggersGenerator(DbHints hints, Delimiter delim, DmlStatements dmlStmts, ForeignKeys fKeys)
  {
    return new MySqlTriggers(hints, delim, dmlStmts, fKeys);
  }
  
  @Override
  protected ForeignKeys createForeignKeysGenerator(DbHints hints, Delimiter delim, Identifiers ident, Comments cmmnts)
  {
    return new MySqlForeignKeys(hints, delim, ident, cmmnts);
  }
  
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
  
  @Override
  protected void generateTableStorage(PrintWriter pr, SqlTable table)
  {
    pr.append(" ENGINE=InnoDB");
  }

  @Override
  protected String getDatabaseComment()
  {
    return "MySQL";
  }
      
  
  @Override
  public void generateAlterTableAlterColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable, SqlTableColumn oldColumn)
  {
    GenerateAlterTableUtil.generateAlterTableChangeColumnWithDefaultAndNullConstraints(pr, this, newColumn, newTable, "MODIFY");
  }

  @Override
  public void generateAlterTableAddColumn(PrintWriter pr, SqlTableColumn newColumn, SqlTable newTable)
  {
    GenerateAlterTableUtil.generateAlterTableAddColumn(pr, this, newColumn, newTable, "ADD COLUMN");
  }
  
  @Override
  protected void generateAlterTableDropUniqueConstraint(PrintWriter pr, SqlTable table, SqlUniqueConstraint unique, List<String> createdTemporaryStoredProcedures)
  {
    pr.print("ALTER TABLE ");
    identifiers.generate(pr, table.getId());
    pr.print(" DROP INDEX ");
    identifiers.generate(pr, unique.getColumns().get(0));
  }
  
  @Override
  public void generateDropIndex(PrintWriter pr, SqlTable table, SqlIndex index)
  {
    pr.print("DROP INDEX ");
    identifiers.generate(pr, getIndexName(index));
    pr.println(); 
    pr.print("ON ");
    identifiers.generate(pr, table.getId());
    delimiter.generate(pr);
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
    String lengthHintStr = dbHints.INDEX_COLUMN_LENGTH.value(index);
    if (lengthHintStr == null)
    {
      return Collections.emptyMap();
    }

    int[] lengthHints = Arrays.stream(lengthHintStr.split(",")).mapToInt(Integer::parseInt).toArray();
    if (lengthHints.length != index.getColumns().size())
    {
      throw new IllegalArgumentException(
              "Hint "+dbHints.INDEX_COLUMN_LENGTH.name()+" on index "+index.getId()+" in table "+table.getId()+
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
    identifiers.generate(pr, getIndexName(index));
    pr.println();
    pr.print("ON ");
    pr.print(table.getId());
    pr.print(" (");
    generateColumnList(pr, bytesPerColumn);
    pr.print(")");
    delimiter.generate(pr);
    pr.println();
    pr.println();    
  }
  
  private boolean generateIndexInTable(PrintWriter pr, SqlIndex index,
          Map<SqlTableColumn, MemoryInfo> bytesPerColumn)
  {
    spaces.generate(pr, 2);
    pr.print("INDEX ");
    identifiers.generate(pr, getIndexName(index));
    pr.print(' ');
    pr.print('(');
    generateColumnList(pr, bytesPerColumn);
    pr.print(')');
    return true;
  }

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
      identifiers.generate(pr, column.getId());
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