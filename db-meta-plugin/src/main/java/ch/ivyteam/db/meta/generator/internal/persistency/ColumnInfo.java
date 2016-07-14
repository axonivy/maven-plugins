package ch.ivyteam.db.meta.generator.internal.persistency;

import java.sql.Blob;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.plexus.util.StringUtils;

import ch.ivyteam.db.meta.generator.internal.ConstantBuilder;
import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;

/**
 * Info about a column that is used in the JavaClassPersistencyServiceImplemenation.ftl
 */
public class ColumnInfo
{
  private SqlTableColumn column;
  private SqlTable table;

  /**
   * Constructor
   * @param table 
   * @param column
   */
  private ColumnInfo(SqlTable table, SqlTableColumn column)
  {
    this.table = table;
    this.column = column;
  }
  
  static List<ColumnInfo> getColumns(SqlTable table)
  {
    List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    for (SqlTableColumn column : table.getColumns())
    {
      columns.add(new ColumnInfo(table, column));
    }
    return columns;
  }
  
  /**
   * @param table 
   * @return columns that are not primary key, parent key and clob or blob columns
   */
  static List<ColumnInfo> getColumnsWithoutPrimaryParentAndLob(SqlTable table)
  {
    List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyColumns(table))
    {
      if (!JavaClassGeneratorUtil.isLobColumn(column))
      {
        columns.add(new ColumnInfo(table, column));
      }
    }
    return columns;
  }

  /**
   * @param table 
   * @return columns that are not primary key, parent key columns
   */
  static List<ColumnInfo> getColumnsWithoutPrimaryAndParent(SqlTable table)
  {
    List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    for (SqlTableColumn column : JavaClassGeneratorUtil.getNonPrimaryAndParentKeyColumns(table))
    {
      columns.add(new ColumnInfo(table, column));
    }
    return columns;
  }

  /**
   * @param table 
   * @return clob columns
   */
  static List<ColumnInfo> getLongCharacterColumns(SqlTable table)
  {
    List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    for (SqlTableColumn column : JavaClassGeneratorUtil.getClobColumns(table))
    {
      columns.add(new ColumnInfo(table, column));
    }
    return columns;
  }

  /**
   * @param table 
   * @return blob columns
   */
  static List<ColumnInfo> getLongBinaryColumns(SqlTable table)
  {
    List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
    for (SqlTableColumn column : JavaClassGeneratorUtil.getBlobColumns(table))
    {
      columns.add(new ColumnInfo(table, column));
    }
    return columns;
  }
  
  static ColumnInfo getOptimisticLockingColumn(SqlTable table) 
  {
    for (SqlTableColumn column : table.getColumns())
    {
    	if (column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.FIELD_FOR_OPTIMISTIC_LOCKING))
    	{
    		return ColumnInfo.create(table, column);
    	}
    }
    return null;
  }

/**
   * @return name
   */
  public String getName()
  {
    return column.getId();
  }
  
  /**
   * @return -
   */
  public String getConstant()
  {
    return new ConstantBuilder(getName()).toConstant();
  }
  
  /**
   * @return data type
   */
  public String getDataType()
  {
    return JavaClassGeneratorUtil.getJavaDataType(column);
  }
  
  /**
   * @return key type
   */
  public String getKeyType()
  {
    if (getDataType().equals("String"))
    {
      return "STRING";
    }
    else if (getDataType().equalsIgnoreCase("long"))
    {
      return "LONG";
    }
    else
    {
      return "INTEGER";
    }
  }
  
  /**
   * Gets the data type specific method name
   * @return method
   */
  public String getMethod()
  {
    String dataType;
    
    if (isObjectKey())
    {
      dataType = getRawJavaDataType();
    }
    else
    {
      dataType = getDataType();
    }

    String method;
    if (isDataType(dataType, Boolean.TYPE))
    {
      method = "nativeBoolean";
    }
    else if (isDataType(dataType, Float.TYPE))
    {
      method="nativeFloat";
    }
    else if (isDataType(dataType, Date.class))
    {
      method="Date";
    }
    else if (isDataType(dataType, Blob.class))
    {
      method="Blob";
    }
    else if (isDataType(dataType, Clob.class))
    {
      method="Clob";
    }
    else if (isJavaEnumeration())
    {
      method = "Enumeration";
    }
    else if (isDataType(dataType, String.class) &&
             column.getDataType().getDataType().equals(DataType.CLOB))
    {
      method="Clob";
    }
    else 
    {
      method = dataType;
    }
    
    return StringUtils.capitalise(method);
  }
  
  private boolean isJavaEnumeration()
  {
    return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.ENUM);
  }
  
  /**
   * @return true if the column is a key and the java data type is Object
   */
  private boolean isObjectKey()
  {
    return isDataType(Object.class) &&
    (
       table.isPrimaryKeyColumn(column)||
       column.equals(JavaClassGeneratorUtil.getParentKeyColumn(table))||
       column.getReference()!=null
     );
  }

  /**
   * @param clazz
   * @return -
   */
  private boolean isDataType(Class<?> clazz)
  {
    return isDataType(getDataType(), clazz);
  }
  
  private boolean isDataType(String dataType, Class<?> clazz)
  {
    return dataType.equals(clazz.getSimpleName())||dataType.equals(clazz.getName());

  }

  /**
   * @return -
   */
  public String getAdditionalReadArgs()
  {
    StringBuilder additionalArgs = new StringBuilder(200);
    if (hasNoNativeDataType())
    {
      additionalArgs.append(", ");
      additionalArgs.append(column.isCanBeNull());
    }
    if (isJavaEnumeration())
    {
      additionalArgs.append(", ");
      additionalArgs.append(getJavaEnumeration());
      additionalArgs.append(".class");
    }
    else if (isDataType(String.class))
    {
      additionalArgs.append(", ");
      additionalArgs.append(column.getDatabaseManagementSystemHints(OracleSqlScriptGenerator.ORACLE).isHintSet(OracleSqlScriptGenerator.CONVERT_EMPTY_STRING_TO_NULL));
    }

    return additionalArgs.toString();
  }
  
  private String getJavaEnumeration()
  {
    return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.ENUM);
  }
  
  /**
   * True if the data type of the column has no native data type
   * @return true if the column data type has no native data type
   */
  private boolean hasNoNativeDataType()
  {
    String dataType;

    if (isObjectKey())
    {
      dataType = getRawJavaDataType();
    }
    else
    {
      dataType = getDataType();
    }
    return !(isDataType(dataType, Integer.class)||
             isDataType(dataType, Integer.TYPE)||
             isDataType(dataType, Boolean.class)||
             isDataType(dataType, Boolean.TYPE)||
             isDataType(dataType, Float.class)||
             isDataType(dataType, Float.TYPE)||
             isDataType(dataType, Long.class)||
             isDataType(dataType, Long.TYPE));
  }
  
  /**
   * @return -
   */
  public String getAdditionalWriteArgs()
  {
    StringBuilder additionalArgs = new StringBuilder(200);
    if (isLob() &&
        !isDataType(String.class))  // raw lob column. No mapping to string
    {
      appendAdditionalWriteArgsForRawLobValues(additionalArgs);
    }
    else
    {
      appendAdditionalWriteArgsForNonLobValues(additionalArgs);
    }
    if (hasNoNativeDataType())
    {
      appendAdditionalWriteArgsForNullableColumns(additionalArgs);
    }
    if (isDataType(String.class) ||
        column.getDataType().getDataType().equals(DataType.CLOB))  // String or clob data type
    {
      appendAdditionalWriteArgsForStrings(additionalArgs);
    }
    else if (isDataType(Number.class))
    {
      appendAdditionalWriteArgsForNumbers(additionalArgs);
    }
    return additionalArgs.toString();
  }
  
  /**
   * @param additionalArgs
   */
  private void appendAdditionalWriteArgsForNullableColumns(StringBuilder additionalArgs)
  {
    additionalArgs.append(", ");
    additionalArgs.append(column.isCanBeNull());
  }

  /**
   * @param additionalArgs
   */
  private void appendAdditionalWriteArgsForNumbers(StringBuilder additionalArgs)
  {
    additionalArgs.append(", ");
    additionalArgs.append(column.getDataType().getLength());
    additionalArgs.append(", ");
    additionalArgs.append(column.getDataType().getPrecision());
  }

  /**
   * @param additionalArgs
   */
  private void appendAdditionalWriteArgsForStrings(StringBuilder additionalArgs)
  {
    additionalArgs.append(", ");
    additionalArgs.append(column.getDatabaseManagementSystemHints(OracleSqlScriptGenerator.ORACLE).isHintSet(OracleSqlScriptGenerator.CONVERT_EMPTY_STRING_TO_NULL));
    if (!column.getDataType().getDataType().equals(DataType.CLOB)) // clobs are not truncated
    {
      additionalArgs.append(", ");
      additionalArgs.append(column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.TRUNCATE));
      additionalArgs.append(", ");
      additionalArgs.append(column.getDataType().getLength());
    }
    
  }

  /**
   * @param additionalArgs
   */
  private void appendAdditionalWriteArgsForNonLobValues(StringBuilder additionalArgs)
  {
    if (isPassword())
    {
      additionalArgs.append("database.encode(transaction, ");
    }
    if (isObjectKey())
    {
      additionalArgs.append(getObjectKeyToDataTypeConversionMethodName());
      additionalArgs.append("(");
    }

    additionalArgs.append("data.get");
    additionalArgs.append(StringUtils.capitalise(JavaClassGeneratorUtil.generateJavaIdentifier(column)));
    additionalArgs.append("()");
    
    if (isObjectKey())
    {
      additionalArgs.append(")");
    }
    if (isPassword())
    {
      additionalArgs.append(", ");
      additionalArgs.append(column.getDataType().getLength());
      additionalArgs.append(")");
    }
  }

  /**
   * @param additionalArgs
   */
  private void appendAdditionalWriteArgsForRawLobValues(StringBuilder additionalArgs)
  {
    if (column.isCanBeNull())
    {
      additionalArgs.append("null");
    }
    else
    {
      if (column.getDataType().getDataType().equals(DataType.CLOB))
      {
        additionalArgs.append("\"\"");
      }
      else
      {
        additionalArgs.append("new byte[0]");
      }
    }
  }

  /**
   * Gets the method name of the conversion routing to convert an object key to the real database data type
   * @return method name
   */
  private String getObjectKeyToDataTypeConversionMethodName()
  {
    return "key2"+StringUtils.capitalise(getRawJavaDataType());
  }
  
  /**
   */
  private String getRawJavaDataType()
  {
    return JavaClassGeneratorUtil.getRawJavaDataType(column, false);
  }

  /**
   * @return true if column stores a password
   */
  public boolean isPassword()
  {
    return JavaClassGeneratorUtil.isPasswordColumn(column);
  }

  /**
   * @return true if this is a blob or clob column
   */
  private boolean isLob()
  {
    return JavaClassGeneratorUtil.isLobColumn(column);
  }

  /**
   * @return sql
   */
  public String getSql()
  {
    StringBuilder sql = new StringBuilder(200);
    sql.append(column.getId());
    sql.append(" ");
    sql.append(column.getDataType());
    sql.append(" ");
    if (column.isCanBeNull())
    {
      sql.append("NULL");
    }
    else
    {
      sql.append("NOT NULL");
    }

    boolean isPrimary = JavaClassGeneratorUtil.getPrimaryKeyColumn(table).equals(column);
    boolean isParent = column.equals(JavaClassGeneratorUtil.getParentKeyColumn(table));
    if ((column.getReference() != null)||isPrimary||isParent)
    {
      sql.append(" (");
      if (isPrimary)
      {
        sql.append("primary key");
      }
      else if (isParent)
      {
        sql.append("parent key");
      }
      if (column.getReference()!=null)
      {
        if (isPrimary||isParent)
        {
          sql.append(" ");
        }
        sql.append("references ");
        sql.append(column.getReference().getForeignTable());
        sql.append(".");
        sql.append(column.getReference().getForeignColumn());
        if (column.getReference().getForeignKeyAction()!=null)
        {
          sql.append(" ");
          sql.append(column.getReference().getForeignKeyAction());
        }
      }
      sql.append(")");
    }
    return sql.toString();
  }
  
  /**
   * @return full name
   */
  public String getFullName()
  {
    return table.getId()+"."+column.getId();
  }

  /**
   * @param table
   * @param column
   * @return new column info
   */
  static ColumnInfo create(SqlTable table, SqlTableColumn column)
  {
    return new ColumnInfo(table, column);
  }

}
