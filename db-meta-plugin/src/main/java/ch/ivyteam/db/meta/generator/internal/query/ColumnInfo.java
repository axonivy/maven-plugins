package ch.ivyteam.db.meta.generator.internal.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.db.meta.generator.internal.ConstantBuilder;
import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.SqlAtom;
import ch.ivyteam.db.meta.model.internal.SqlCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlComplexCaseExpr;
import ch.ivyteam.db.meta.model.internal.SqlDataType;
import ch.ivyteam.db.meta.model.internal.SqlDataType.DataType;
import ch.ivyteam.db.meta.model.internal.SqlFullQualifiedColumnName;
import ch.ivyteam.db.meta.model.internal.SqlLiteral;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlTableColumn;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;

/**
 * This class contains information to generate java code
 * depending on a {@link SqlTableColumn}.
 * @author fs
 * @since 11.01.2012
 */
public abstract class ColumnInfo
{
  private final TableInfo tableInfo;

  public static List<ColumnInfo> getColumns(SqlMeta meta, TableInfo tableInfo)
  {
    SqlView view = JavaClassGeneratorUtil.getQueryView(meta, tableInfo.getTable());
    if (view != null)
    {
      return getColumns(meta, tableInfo, view);
    }
    return getColumns(tableInfo);
  }

  private static List<ColumnInfo> getColumns(TableInfo tableInfo)
  {
    List<ColumnInfo> result = new ArrayList<>();
    for(SqlTableColumn column: tableInfo.getTable().getColumns())
    {
      if (! column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.HIDE_FIELD_ON_QUERY))
      {
        result.add(new TableColumnInfo(tableInfo, column));
      }
    }
    return result;
  }

  private static List<ColumnInfo> getColumns(SqlMeta meta, TableInfo tableInfo, SqlView view)
  {
    List<ColumnInfo> result = new ArrayList<ColumnInfo>();
    int pos = 0;
    for (SqlViewColumn column : view.getColumns())
    {
      if (! column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.HIDE_FIELD_ON_QUERY))
      {
        result.add(new ViewColumnInfo(meta, view.getTableAliases(), tableInfo, column, view.getSelects().get(0).getExpressions().get(pos).getExpression()));
      }
      pos++;
    }
    return result;
  }

  public static Set<EnumerationInfo> getEnumerationInfos(List<ColumnInfo> columnInfos)
  {
    Set<EnumerationInfo> enumerationsInfos = new HashSet<>();
    for (ColumnInfo column : columnInfos)
    {
      if (column.isEnumeration())
      {
        enumerationsInfos.add(column.getEnumerationInfo());
      }
    }
    return enumerationsInfos;
  }

  public ColumnInfo(TableInfo tableInfo)
  {
    this.tableInfo = tableInfo;
  }

  public String getName()
  {
    return getColumn().getId();
  }

  public String getJavaColumnConstantName()
  {
    StringBuilder builder = new StringBuilder();
    builder.append(getTableInfo().getJavaDataClassName());
    builder.append(".COLUMN_");
    builder.append(new ConstantBuilder(getColumn().getId()).toConstant());
    return builder.toString();
  }

  public String getAdditionalComments()
  {
    return null;
  }

  public boolean isGroupAndOrderBySupported()
  {
    return true;
  }

  public boolean isSumSupported()
  {
    return !isForeignOrPrimaryKey() &&
      ( supportsIntegerOption() || supportsDecimalOption());
  }

  public boolean isAvgSupported()
  {
    return !isForeignOrPrimaryKey() &&
    (
      supportsIntegerOption() ||
      supportsDecimalOption() ||
      supportsDateTimeOption()
    );
  }

  public boolean isMinSupported()
  {
    return !isForeignKey() &&
    (
      supportsIntegerOption() ||
      supportsDecimalOption() ||
      supportsStringOption() ||
      supportsDateTimeOption()
    );
  }

  public boolean isMaxSupported()
  {
    return !isForeignKey() &&
    (
      supportsIntegerOption() ||
      supportsDecimalOption() ||
      supportsStringOption() ||
      supportsDateTimeOption()
    );
  }

  public boolean supportsDecimalOption()
  {
    DataType dataType = getDataType();
    return dataType == DataType.DECIMAL ||
           dataType == DataType.FLOAT ||
           dataType == DataType.NUMBER;
  }

  public boolean supportsIntegerOption()
  {
    if (isEnumeration())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.INTEGER || dataType == DataType.BIGINT;
  }

  public boolean supportsStringOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.CHAR ||
           dataType == DataType.VARCHAR;
  }
  
  public boolean supportsClobOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.CLOB;
  }

  public boolean supportsDateTimeOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.DATE ||
           dataType == DataType.DATETIME;
  }

  public boolean supportsBooleanOption()
  {
    if (isForeignOrPrimaryKey())
    {
      return false;
    }
    DataType dataType = getDataType();
    return dataType == DataType.BIT;
  }

  private DataType getDataType()
  {
    if (getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.FILTER_QUERY_DATA_TYPE))
    {
      return DataType.valueOf(getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.FILTER_QUERY_DATA_TYPE));
    }
    return getColumn().getDataType().getDataType();
  }

  protected abstract SqlTableColumn getColumn();

  public boolean isEnumeration()
  {
    return getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.ENUM);
  }

  public EnumerationInfo getEnumerationInfo()
  {
    String enumName = getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.ENUM);
    return new EnumerationInfo(enumName);
  }

  private boolean isForeignOrPrimaryKey()
  {
    return isForeignKey() || isPrimaryKey();
  }

  private boolean isForeignKey()
  {
    return getColumn().getReference() != null;
  }

  private boolean isPrimaryKey()
  {
    return tableInfo.getTable().getPrimaryKey().getPrimaryKeyColumns().contains(getColumn().getId());
  }
  
  protected TableInfo getTableInfo()
  {
    return tableInfo;
  }
  
  public boolean isDeprecated()
  {
    return getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).isHintSet(JavaClassGenerator.DEPRECATED);
  }

  public String getDeprecatedUseColumnInstead()
  {
    String useColumnNameInstead = getColumn().getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.DEPRECATED);
    if (StringUtils.isBlank(useColumnNameInstead))
    {
      throw new IllegalStateException("No column defined which should be used instead of deprecated column "+getName()); 
    }
    SqlTableColumn useColumnInstead = tableInfo.getTable().findColumn(useColumnNameInstead);
    if (useColumnInstead == null)
    {
      throw new IllegalStateException("Column '"+useColumnNameInstead+"' defined to used instead of deprecated column "+getName()+" not found");
    }
    return useColumnInstead.getId();
  }

  private static class TableColumnInfo extends ColumnInfo
  {
    private final SqlTableColumn column;

    public TableColumnInfo(TableInfo tableInfo, SqlTableColumn column)
    {
      super(tableInfo);
      this.column = column;
    }

    @Override
    protected SqlTableColumn getColumn()
    {
      return column;
    }
  }

  private static class ViewColumnInfo extends ColumnInfo
  {
    private SqlViewColumn column;
    private SqlMeta meta;
    private SqlAtom expression;
    private Map<String, String> tableAliases;

    public ViewColumnInfo(SqlMeta meta, Map<String, String> tableAliases, TableInfo tableInfo, SqlViewColumn column, SqlAtom expression)
    {
      super(tableInfo);
      this.meta = meta;
      this.tableAliases = tableAliases;
      this.column = column;
      this.expression = expression;
    }

    @Override
    public String getName()
    {
      String id = column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_FIELD_NAME);
      if (id != null)
      {
        return id;
      }
      return column.getId();
    }

    @Override
    public String getAdditionalComments()
    {
      if (expression instanceof SqlFullQualifiedColumnName)
      {
        SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName)expression;
        if (!getTableInfo().getTable().getId().equals(columnName.getTable()))
        {
          return JavaClassGeneratorUtil.convertToJavaDoc(
                  "<p>This is a virtual column. It contains the same value as the column "+
                  "<code>"+columnName.getColumn()+"</code> of the referenced <code>"+
                  JavaClassGeneratorUtil.removeTablePrefix(columnName.getTable())+"</code>.</p>", 5);
        }
      }
      return null;
    }

    @Override
    protected SqlTableColumn getColumn()
    {
      if (expression instanceof SqlFullQualifiedColumnName)
      {
        SqlFullQualifiedColumnName columnName = (SqlFullQualifiedColumnName)expression;
        return getColumn(columnName);
      }
      else if (expression instanceof SqlCaseExpr)
      {
        SqlCaseExpr caseExpr = (SqlCaseExpr)expression;
        return getColumn(caseExpr.getWhenThenList().get(0).getColumnName());
      }
      else if (expression instanceof SqlComplexCaseExpr)
      {
        SqlComplexCaseExpr caseExpr = (SqlComplexCaseExpr) expression;
        SqlAtom atom = caseExpr.getWhenThenList().get(0).getAction();
        if (atom instanceof SqlFullQualifiedColumnName)
        {
          return getColumn((SqlFullQualifiedColumnName) atom);
        }
        else if (atom instanceof SqlLiteral)
        {
          SqlLiteral sqlLiteral = (SqlLiteral) atom;
          String typeName = sqlLiteral.getValue().getClass().getSimpleName();
          return new SqlTableColumn(column.getId(),
                                    new SqlDataType(DataType.valueOf(typeName.toUpperCase())),
                                    false,
                                    sqlLiteral,
                                    null,
                                    column.getDatabaseManagementSystemHints(),
                                    column.getComment());
        }
        throw new IllegalStateException("Unsupported view expression "+expression);
      }
      else
      {
        throw new IllegalStateException("Unsupported view expression "+expression);
      }
    }

    private SqlTableColumn getColumn(SqlFullQualifiedColumnName columnName)
    {
      SqlTable table = findTable(columnName.getTable());
      SqlTableColumn tableColumn = table.findColumn(columnName.getColumn());
      if (tableColumn == null)
      {
        throw new IllegalStateException("Unknown column  "+columnName);
      }
      return tableColumn;
    }

    private SqlTable findTable(String tableOrAlias)
    {
      SqlTable table = meta.findTable(tableOrAlias);
      if (table != null)
      {
        return table;
      }
      String tableName = tableAliases.get(tableOrAlias);
      if (tableName == null)
      {
        throw new IllegalStateException("Unknown table "+tableOrAlias);
      }
      table = meta.findTable(tableName);
      if (table == null)
      {
        throw new IllegalStateException("Unknown table "+tableOrAlias);
      }
      return table;
    }

    @Override
    public String getJavaColumnConstantName()
    {
      StringBuilder builder = new StringBuilder();
      builder.append(getTableInfo().getJavaDataClassName());
      builder.append(".QueryView.VIEW_COLUMN_");
      builder.append(new ConstantBuilder(column.getId()).toConstant());
      return builder.toString();
    }
  }
}
