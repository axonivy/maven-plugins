package ch.ivyteam.db.meta.generator.internal.persistency;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.generator.internal.JavaClassGeneratorUtil;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/** 
 * Info about a table that is used in the JavaClassPersistencyServiceImplemenation.ftl
 */
public class TableInfo
{
  private SqlTable table;
  private String entityPackage;

  /**
   * Constructor
   * @param table
   * @param entityPackage 
   */
  public TableInfo(SqlTable table, String entityPackage)
  {
    this.table = table;
    this.entityPackage = entityPackage;
  }
  
  /**
   * @return full qualified entity class name
   */
  public String getEntityClass()
  {
    return entityPackage+"."+getSimpleEntityClass();
  }

  /**
   * @return simple entity class name
   */
  public String getSimpleEntityClass()
  {
    return JavaClassGeneratorUtil.getEntityClassName(table);
  }
  
  /**
   * @return name
   */
  public String getName()
  {
    return table.getId();
  }
  
  /**
   * @return the name of the query view or null
   */
  public String getQuery()
  {
    return table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_TABLE_NAME);
  }
  
  /**
   * @return comment
   */
  public String getComment()
  {
    return JavaClassGeneratorUtil.convertToJavaDoc(table.getComment(), 1);
  }
  
  /**
   * @return primary key
   */
  public ColumnInfo getPrimaryKey()
  {
    return ColumnInfo.create(table, JavaClassGeneratorUtil.getPrimaryKeyColumn(table));
  }
  
  /**
   * @return parent key or null
   */
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
  
  /**
   * @return key type
   */
  public String getKeyType()
  {
    return getPrimaryKey().getJavaDataType();
  }

  /**
   * @param table
   * @param entityPackage
   * @return new table info
   */
  static TableInfo create(SqlTable table, String entityPackage)
  {
    return new TableInfo(table, entityPackage);
  }

}
