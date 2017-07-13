package ch.ivyteam.db.meta.model.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An sql view definition
 * @author rwei
 * @since 02.10.2009
 */
public class SqlView extends SqlObject
{
  /** column definition of the view */
  private List<SqlViewColumn> fColumns;

  /** The selects used to define the view. If more than one select exists combine them with UNION ALL. */
  private List<SqlSelect> fSelects;

  public SqlView(String id, List<SqlViewColumn> columns, List<SqlSelect> selects,
          List<SqlDatabaseSystemHints> dbSysHints,
          String comment) throws MetaException
  {
    super(id, dbSysHints, comment);
    
    assert columns != null : "Parameter columns must not be null";
    assert selects != null : "Parameter selects must not be null";
    
    fColumns = columns;
    fSelects = selects;
    
    for (SqlSelect select : fSelects)
    {     
      if (columns.size() > select.getExpressions().size())        
      { 
        throw new MetaException("Missing expression(s) in view "+id+". There are more columns defined than expressions");
      }
      else if (columns.size() < select.getExpressions().size())
      {
        throw new MetaException("Missing column(s) in view "+id+". There are more expression defined than columns");
      }
    }
  }

  public List<SqlViewColumn> getColumns()
  {
    return fColumns;
  }

  public boolean hasColumn(String columnName)
  {
    return findColumn(columnName) != null;
  }

  public SqlViewColumn findColumn(String columnName)
  {
    Optional<SqlViewColumn> optionalColumn = fColumns.stream()
            .filter(column -> column.getId().equals(columnName))
            .findFirst();
    return optionalColumn.orElse(null);
  }

  public List<SqlSelect> getSelects()
  {
    return fSelects;
  }

  @Override
  public String toString()
  {
    boolean first = true;
    StringBuilder builder = new StringBuilder(32512);
    builder.append("CREATE VIEW ");
    builder.append(getId());
    builder.append("(\n");
    for (SqlViewColumn column : fColumns)
    {
      if (!first)
      {
        builder.append(",\n");
      }
      first = false;
      builder.append("  ");
      builder.append(column);
    }
    builder.append(")\n");
    builder.append("AS ");
    for (SqlSelect select : fSelects)
    {
      builder.append(select);      
    }
    return builder.toString();
  }

  /**
   * @return all tables used by the view
   */
  public Set<String> getTables()
  {
    Set<String> tables = new HashSet<>();
    for (SqlSelect select : fSelects)
    {
      for (SqlJoinTable joinTable : select.getJoinTables())
      {
        tables.add(joinTable.getTable().getName());
      }
    }
    return tables;
  }

  public Map<String, String> getTableAliases()
  {
    Map<String, String> aliases = new HashMap<>();
    for (SqlSelect select : fSelects)
    {
      aliases.putAll(select.getTableAliases());
    }

    return aliases;
  }
}
