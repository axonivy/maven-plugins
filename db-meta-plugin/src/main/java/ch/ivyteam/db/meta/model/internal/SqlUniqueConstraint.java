package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * Sql unique constraint definition
 * @author rwei
 */
public class SqlUniqueConstraint extends SqlTableContentDefinition
{
  /** The columns */
  private List<String> fColumns;
  
  /**
   * Constructor
   * @param id the id
   * @param columns the columns
   * @param dbSysHints 
   * @param comment 
   * @throws MetaException 
   */
  public SqlUniqueConstraint(String id, List<String> columns, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(generateId("UQ", id, columns), dbSysHints, comment);
    assert columns != null : "Parameter columns must not be null";
    fColumns = columns;
  }

  /**
   * Gets the columns 
   * @return columns
   */
  public List<String> getColumns()
  {
    return fColumns;
  }  
  
  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(512);
    builder.append("UNIQUE ");
    builder.append(getId());
    builder.append(" (");
    SqlScriptUtil.formatCommaSeparated(builder, fColumns);
    builder.append(")");
    return builder.toString();
  }
}
