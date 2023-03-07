package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * A primary key definition
 * @author rwei
 */
public class SqlPrimaryKey extends SqlTableContentDefinition {

  /** The columns that defined the primary key */
  private List<String> fPrimaryKeyColumns;

  /**
   * Constructor
   * @param id the identifier of the primary key
   * @param primaryKeyColumns the primary key columns
   * @param dbSysHints
   * @param comment
   * @throws MetaException
   */
  public SqlPrimaryKey(String id, List<String> primaryKeyColumns, List<SqlDatabaseSystemHints> dbSysHints,
          String comment) throws MetaException {
    super(generateId("PK", id, primaryKeyColumns), dbSysHints, comment);
    fPrimaryKeyColumns = primaryKeyColumns;
  }

  /**
   * The primary key columns
   * @return primary key columns
   */
  public List<String> getPrimaryKeyColumns() {
    return fPrimaryKeyColumns;
  }

  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(512);
    builder.append("PRIMARY KEY ");
    builder.append(getId());
    builder.append(" (");
    SqlScriptUtil.formatCommaSeparated(builder, fPrimaryKeyColumns);
    return builder.toString();
  }
}
