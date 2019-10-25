package ch.ivyteam.db.meta.generator.internal.persistency;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/** 
 * Info about a table that is used in the JavaClassPersistencyServiceImplemenation.ftl
 */
public class TableInfo
{
  private final SqlTable table;
  private final String entityPackage;

  public TableInfo(SqlTable table, String entityPackage)
  {
    this.table = table;
    this.entityPackage = entityPackage;
  }
  
  public String getEntityClass()
  {
    return entityPackage+"."+getSimpleEntityClass();
  }

  public String getSimpleEntityClass()
  {
    return JavaClassGeneratorUtil.getEntityClassName(table);
  }

  public String getName()
  {
    return table.getId();
  }
  
  public String getQuery()
  {
    return table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_TABLE_NAME);
  }
  
  public String getComment()
  {
    return JavaClassGeneratorUtil.convertToJavaDoc(table.getComment(), 1);
  }
  
  public ColumnInfo getPrimaryKey()
  {
    return ColumnInfo.create(table, JavaClassGeneratorUtil.getPrimaryKeyColumn(table));
  }
  
  public ColumnInfo getParentKey()
  {
    String parentKey = JavaClassGeneratorUtil.getParentKey(table);
    if (parentKey == null)
    {
      return null;
    }
    return ColumnInfo.create(table, table.findColumn(parentKey));
  }
  
  public ColumnInfo getFieldForOptimisticLocking()
  {
    String fieldForOptimisticLocking = JavaClassGeneratorUtil.getFieldForOptimisticLocking(table);
    if (fieldForOptimisticLocking == null)
    {
      return null;
    }
    return ColumnInfo.create(table, table.findColumn(fieldForOptimisticLocking));
  }
  
  public String getKeyType()
  {
    return getPrimaryKey().getJavaDataType();
  }

  static TableInfo create(SqlTable table, String entityPackage)
  {
    return new TableInfo(table, entityPackage);
  }

}
