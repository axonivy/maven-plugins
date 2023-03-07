package ch.ivyteam.db.meta.generator.internal.persistency;

import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.db.meta.generator.internal.ConstantBuilder;
import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.model.internal.SqlViewColumn;

/**
 * Info about a view column that is used in the
 * JavaClassPersistencyServiceImplemenation.ftl
 */
public class ViewColumnInfo {

  private final SqlViewColumn column;

  private ViewColumnInfo(SqlViewColumn column) {
    this.column = column;
  }

  public String getName() {
    return column.getId();
  }

  public String getAlias() {
    return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA)
            .getHintValue(JavaClassGenerator.QUERY_FIELD_NAME);
  }

  public String getConstant() {
    return new ConstantBuilder(getName()).toConstant();
  }

  public boolean isMandatoryFilter() {
    return column.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA)
            .isHintSet(JavaClassGenerator.MANDATORY_FILTER);
  }

  public static List<ViewColumnInfo> getViewColumns(SqlTable table, SqlMeta meta) {
    String queryView = table.getDatabaseManagementSystemHints(JavaClassGenerator.JAVA)
            .getHintValue(JavaClassGenerator.QUERY_TABLE_NAME);
    if (queryView == null) {
      return null;
    }
    SqlView view = meta.findView(queryView);
    if (view == null) {
      throw new IllegalStateException("Could not find view " + queryView);
    }
    return getViewColumns(view);
  }

  public static List<ViewColumnInfo> getViewColumns(SqlView view) {
    List<ViewColumnInfo> columns = new ArrayList<>();
    for (SqlViewColumn column : view.getColumns()) {
      columns.add(new ViewColumnInfo(column));
    }
    return columns;
  }
}
