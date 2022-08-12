package ${packageName};

import ch.ivyteam.db.sql.ColumnName;

public class ${className}
{
  public static final String VIEWNAME = "${view.id}";
<#list columns as column>

  private static final String COLUMN_NAME_${column.constant} = "${column.name}";

  public static final ColumnName COLUMN_${column.constant} = new ColumnName(VIEWNAME, COLUMN_NAME_${column.constant});
</#list>
}
