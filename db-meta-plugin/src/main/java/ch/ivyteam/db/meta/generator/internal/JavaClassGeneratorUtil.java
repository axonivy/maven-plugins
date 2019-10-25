package ch.ivyteam.db.meta.generator.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlForeignKey;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlView;

/**
 * Utility methods to generate ivy persistent realated java classes
 * @author fs
 * @since 11.01.2012
 */
public final class JavaClassGeneratorUtil
{

  private JavaClassGeneratorUtil()
  {
  }

  /**
   * Gets the entity class name for the given table
   * @param table table
   * @return entity class name
   */
  public static String getEntityClassName(SqlTable table)
  {
   return getJavaClassName(table)+"Data";
  }
  
  /**
   * @param view
   * @return -
   */
  public static String getEntityClassName(SqlView view)
  {
    return getJavaClassName(view)+"Data";
  }



  /**
   * @param table
   * @return -
   */
  public static String getJavaClassName(SqlTable table)
  {
    String className;
    if (table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.CLASS_NAME))
    {
      className = table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.CLASS_NAME);
    }
    else
    {
      className = removeTablePrefix(table.getId());
    }
    return className;
  }
  
  /**
   * @param view
   * @return -
   */
  public static String getJavaClassName(SqlView view)
  {
    String className;
    if (view.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.CLASS_NAME))
    {
      className = view.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.CLASS_NAME);
    }
    else
    {
      className = removeTablePrefix(view.getId());
    }
    return className;
  }


  /**
   * Remove the prefix from the table name
   * @param tableName name of the table
   * @return table name without prefix
   */
  public static String removeTablePrefix(String tableName)
  {
    if (tableName.startsWith("IWA_"))
    {
      return tableName.substring("IWA_".length());
    }
    else
    {
      return tableName;
    }
  }

  /**
   * Generates a java identifier for the given column
   * @param column
   * @return java identifier
   */
  public static String generateJavaIdentifier(SqlTableColumn column)
  {
    if (column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.ATTRIBUTE_NAME))
    {
      return StringUtils.capitalize(column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.ATTRIBUTE_NAME));
    }
    else
    {
      return column.getId();
    }
  }

  /**
   * Gets long binary object columns
   * @param table
   * @return list with the blob columns
   */
  public static List<SqlTableColumn> getBlobColumns(SqlTable table)
  {
    List<SqlTableColumn> columns = new ArrayList<SqlTableColumn>();
    for (SqlTableColumn column : table.getColumns())
    {
      if (column.getDataType().getDataType() == SqlDataType.DataType.BLOB)
      {
        columns.add(column);
      }
    }
    return columns;
  }

  /**
   * Gets long character object columns
   * @param table
   * @return list with the clob columns
   */
  public static List<SqlTableColumn> getClobColumns(SqlTable table)
  {
    List<SqlTableColumn> columns = new ArrayList<SqlTableColumn>();
    for (SqlTableColumn column : table.getColumns())
    {
      if ((column.getDataType().getDataType() == SqlDataType.DataType.CLOB)&&
         (!"String".equals(column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.DATA_TYPE))))
      {
        columns.add(column);
      }
    }
    return columns;
  }

  /**
   * Gets the java data type of the given column
   * @param column
   * @return java data type
   */
  public static String getJavaDataType(SqlTableColumn column)
  {
    return JavaClassGeneratorUtil.getJavaDataType(column, false);
  }

  /**
   * Gets the java data type of the given column. Data Type Java system hints are considered.
   * @param column
   * @param noNativeDataTypes if no native data types like int, boolean, float should be returned. Instead Integer, Boolean, Float will returned
   * @return java data type
   */
  protected static String getJavaDataType(SqlTableColumn column, boolean noNativeDataTypes)
  {
    if (column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.DATA_TYPE))
    {
      return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.DATA_TYPE);
    }
    else if (column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.ENUM))
    {
      return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.ENUM);
    }
    else
    {
      return JavaClassGeneratorUtil.getRawJavaDataType(column, noNativeDataTypes);
    }
  }

  /**
   * Gets a list of all columns of the table without the primary and parent key columns and all
   * columns with LOB data type
   * @param table the table
   * @return list of columns
   */
  public static List<SqlTableColumn> getNonPrimaryAndParentKeyAndLobColumns(SqlTable table)
  {
    List<SqlTableColumn> columns;
  
    columns = JavaClassGeneratorUtil.getNonPrimaryAndParentKeyColumns(table);
    for (SqlTableColumn column : table.getColumns())
    {
      if (JavaClassGeneratorUtil.isLobColumn(column))
      {
        columns.remove(column);
      }
    }
    return columns;
  }

  /**
   * Gets non primary key columns
   * @param table the table
   * @return the columns
   */
  public static List<SqlTableColumn> getNonPrimaryAndParentKeyColumns(SqlTable table)
  {
    List<SqlTableColumn> columns = new ArrayList<SqlTableColumn>();
    List<String> primaryKeyColumns = table.getPrimaryKey().getPrimaryKeyColumns();
    boolean found;
    String parentKey = JavaClassGeneratorUtil.getParentKey(table);
    for (SqlTableColumn column : table.getColumns())
    {
      found = false;
      for (String primaryKeyColumn : primaryKeyColumns)
      {
        if (primaryKeyColumn.equals(column.getId()))
        {
          found = true;
          break;
        }
      }
      if (column.getId().equals(parentKey))
      {
        found = true;
      }
      if(!found)
      {
        columns.add(column);
      }
    }
    return columns;
  }

  /**
   * Gets the parent key
   * @param table
   * @return parent key or null
   */
  public static String getParentKey(SqlTable table)
  {
    for (SqlForeignKey foreignKey : table.getForeignKeys())
    {
      if (foreignKey.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.AS_PARENT))
      {
        return foreignKey.getColumnName();
      }
    }
    return null;
  }
  
  public static String getFieldForOptimisticLocking(SqlTable table)
  {
    for (SqlTableColumn col : table.getColumns())
    {
      if (col.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.FIELD_FOR_OPTIMISTIC_LOCKING))
      {
        return col.getId();
      }
    }
    return null;
  }

  /**
   * Gets the parent key column of the given talbe
   * @param table
   * @return parent key column or null
   */
  public static SqlTableColumn getParentKeyColumn(SqlTable table)
  {
    List<SqlTableColumn> parentKeys = JavaClassGeneratorUtil.getParentKeyColumns(table);
    if (parentKeys.isEmpty())
    {
      return null;
    }
    return parentKeys.get(0);
  }

  /**
   * Gets the parent key columns
   * @param table the table
   * @return parent key columns
   */
  protected static List<SqlTableColumn> getParentKeyColumns(SqlTable table)
  {
    String parentKey = getParentKey(table);
    if (parentKey == null)
    {
      return Collections.emptyList();
    }
    return Arrays.asList(table.findColumn(parentKey));
  }

  /**
   * Gets the java data type of the given column. Data Type Java system hints are <b>not</b> considered.
   * @param column
   * @param noNativeDataTypes if no native data types like int, boolean, float should be returned. Instead Integer, Boolean, Float will returned
   * @return java data type
   */
  public static String getRawJavaDataType(SqlTableColumn column, boolean noNativeDataTypes)
  {
    boolean canBeNull = column.isCanBeNull();
  
    switch(column.getDataType().getDataType())
    {
      case BIT:
        if (canBeNull || noNativeDataTypes)
        {
          return "Boolean";
        }
        else
        {
          return "boolean";
        }
      case BLOB:
        return "java.sql.Blob";
      case CLOB:
        return "java.sql.Clob";
      case CHAR:
      case VARCHAR:
        return "String";
      case DATETIME:
      case DATE:
      case TIME:
        return "java.util.Date";
      case DECIMAL:
        if (column.getDataType().getPrecision()>0)
        {
          return "Number";
        }
        else
        {
          return "Integer";
        }
      case FLOAT:
        if (canBeNull || noNativeDataTypes)
        {
          return "Float";
        }
        else
        {
          return "float";
        }
      case INTEGER:
        if (canBeNull || noNativeDataTypes)
        {
          return "Integer";
        }
        else
        {
          return "int";
        }
      case BIGINT:
        if (canBeNull || noNativeDataTypes)
        {
          return "Long";
        }
        else
        {
          return "long";
        }
      case NUMBER:
        return "Integer";
      default:
        return "Unknown";
    }
  }

  /**
   * Gets the primary key column of the given table
   * @param table
   * @return primary key column
   * @throws MetaException
   */
  public static SqlTableColumn getPrimaryKeyColumn(SqlTable table)
  {
    List<SqlTableColumn> primaryKeys = getPrimaryKeyColumns(table);
    if (primaryKeys.isEmpty())
    {
      throw new MetaException("Table must define a primary key");
    }
    return primaryKeys.get(0);
  }

  /**
   * Checks if the given column is a password column
   * @param column
   * @return true if the column is a password column otherwise false
   */
  public static boolean isPasswordColumn(SqlTableColumn column)
  {
   return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.PASSWORD);
  }
  
  public static SqlTableColumn getOptionalPasswordColumnFor(SqlTable table, SqlTableColumn column)
  {
    String optionalPasswordColumnName = column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.PASSWORD);
    if (optionalPasswordColumnName == null)
    {
      return null;
    }
    SqlTableColumn optionalPasswordColumn = table.findColumn(optionalPasswordColumnName);
    if (optionalPasswordColumn == null)
    {
      throw new MetaException("Optional password control column "+optionalPasswordColumnName+" defined on column "+column.getId()+" not found in table "+table.getId());
    }
    return optionalPasswordColumn;
  }

  
  public static boolean isOptimisticLockingColumn(SqlTableColumn column)
  {
   return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.FIELD_FOR_OPTIMISTIC_LOCKING);
  }

  /**
   * Gets the primary key columns
   * @param table the table
   * @return primary key columns
   */
  protected static List<SqlTableColumn> getPrimaryKeyColumns(SqlTable table)
  {
    List<SqlTableColumn> columns = new ArrayList<>();
  
    if (table.getPrimaryKey()==null)
    {
      return columns;
    }
    for (SqlTableColumn column : table.getColumns())
    {
      if (table.getPrimaryKey().getPrimaryKeyColumns().contains(column.getId()))
      {
        columns.add(column);
      }
    }
    return columns;
  }

  /**
   * Checks if the given column has a lob (clob, blob) data type
   * @param column the column
   * @return true if it is a lob, false if not
   */
  public static boolean isLobColumn(SqlTableColumn column)
  {
    return (((column.getDataType().getDataType() == DataType.CLOB)&&
             (!"String".equals(column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.DATA_TYPE))))||
            (column.getDataType().getDataType() == DataType.BLOB));
  }
  
  /**
   * @param metaDefinition
   * @param table
   * @return query view or null
   */
  public static SqlView getQueryView(SqlMeta metaDefinition, SqlTable table)
  {
    if (!table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.QUERY_TABLE_NAME))
    {
      return null;
    }
    String queryView = table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_TABLE_NAME);
    SqlView view = metaDefinition.findView(queryView);
    if (view == null)
    {
      throw new MetaException("Could not find view "+queryView);
    }
    return view;
  }
  
  public static String convertToJavaDoc(String comment, int indent)
  {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String line : comment.split("\n"))
    {
      if (!first)
      {
        builder.append("\n");
        for (int pos=0; pos < indent; pos++)
        {
          builder.append(" ");
        }          
        builder.append("* ");
      }
      first = false;
      builder.append(line);
      builder.append("<br>");
    }
    return JavaClassGenerator.removeAtRef((builder.toString()));
  }
}
