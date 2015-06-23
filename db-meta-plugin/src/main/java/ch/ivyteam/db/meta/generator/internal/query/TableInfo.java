package ch.ivyteam.db.meta.generator.internal.query;


import java.util.List;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/**
 * Contains informations about the sql table to generate java query classes.
 * @author fs
 * @since 11.01.2012
 */
public class TableInfo
{
  /** System DB Hint, used in the SystemDatabase.meta */
  public static final String BUSINESS_CLASS = "BusinessClass";
  
  private final SqlTable table;

  private boolean hasNumberColumns;

  private boolean hasDateColumns;

  private boolean hasIntegerColumns;

  private boolean hasBooleanColumns;

  private boolean hasStringColumns;

  /**
   * Constructor
   * @param table -
   */
  private TableInfo(SqlTable table)
  {
    this.table = table;
  }

  /**
   * @return -
   */
  public String getQueryClassName()
  {
    return JavaClassGeneratorUtil.getJavaClassName(table) + "Query";
  }

  /**
   * @return -
   */
  public String getJavaDataClassName()
  {
    String tableJavaName = JavaClassGeneratorUtil.getJavaClassName(table);
    return "Db"+tableJavaName+"Data";
  }

  /**
   * @return -
   */
  public String getBusinessClassName()
  {
    return table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(BUSINESS_CLASS);
  }

  /**
   * @return -
   */
  public SqlTable getTable()
  {
    return table;
  }
  
  /**
   * @return -
   */
  public String getName()
  {
    return table.getId();
  }
  
  /**
   * @return -
   */
  public boolean getHasStringColumns()
  {
    return hasStringColumns;
  }
      
  /**
   * @return -
   */
  public boolean getHasBooleanColumns()
  {
    return hasBooleanColumns;
  }
      
  /**
   * @return -
   */
  public boolean getHasIntegerColumns()
  {
    return hasIntegerColumns;
  }
  
  /**
   * @return -
   */
  public boolean getHasNumberColumns()
  {
    return hasNumberColumns;
  }
  
  /**
   * @return -
   */
  public boolean getHasDateColumns()
  {
    return hasDateColumns;
  }

  /**
   * @param columns
   */
  private void summarizeDataTypes(List<ColumnInfo> columns)
  {
    for (ColumnInfo column : columns)
    {
      if (column.supportsBooleanOption())
      {
        hasBooleanColumns=true;
      }
      else if (column.supportsStringOption())
      {
        hasStringColumns=true;
      }
      else if (column.supportsDateTimeOption())
      {
        hasDateColumns=true;
      }
      else if (column.supportsDecimalOption())
      {
        hasNumberColumns=true;
      }
      else if (column.supportsIntegerOption())
      {
        hasIntegerColumns=true;
      }
    }
  }

  /**
   * @param table
   * @param meta
   * @return table info
   */
  public static TableInfo create(SqlTable table, SqlMeta meta)
  {
    TableInfo tableInfo = new TableInfo(table);
    tableInfo.summarizeDataTypes(ColumnInfo.getColumns(meta, tableInfo));
    return tableInfo;
  }

}
