package ch.ivyteam.db.meta.generator.internal.persistency;

import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;
import ch.ivyteam.util.StringUtil;

/**
 * Info about a view column that is used in the JavaClassPersistencyServiceImplemenation.ftl
 */
public class ViewColumnInfo
{

  private SqlViewColumn column;

  /**
   * Constructor
   * @param column 
   */
  private ViewColumnInfo(SqlViewColumn column)
  {
    this.column = column;
  }
  
  /**
   * @return name
   */
  public String getName()
  {
    return column.getId();
  }
  
  /**
   * @return view column alias
   */
  public String getAlias()
  {
    return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_FIELD_NAME);
  }
  
  /**
   * @return -
   */
  public String getConstant()
  {
    return StringUtil.camelCaseToUpperCase(getName());
  }

  /**
   * @param table
   * @param meta
   * @return query view columns or null
   */
  public static List<ViewColumnInfo> getViewColumns(SqlTable table, SqlMeta meta)
  {
    String queryView =  table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA).getHintValue(JavaClassGenerator.QUERY_TABLE_NAME);
    if (queryView==null)
    {
      return null;
    }
    SqlView view = meta.findView(queryView);
    if (view == null)
    {
      throw new IllegalStateException("Could not find view "+queryView);
    }
    return getViewColumns(view);
  }

  /**
   * @param view
   * @return -
   */
  public static List<ViewColumnInfo> getViewColumns(SqlView view)
  {
    List<ViewColumnInfo> columns = new ArrayList<ViewColumnInfo>();
    for (SqlViewColumn column : view.getColumns())
    {
      columns.add(new ViewColumnInfo(column));
    }
    return columns;
  }

}
