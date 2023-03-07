package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import ch.ivyteam.db.meta.generator.internal.SqlScriptUtil;

/**
 * An SQL Index definition
 * @author rwei
 */
public class SqlIndex extends SqlTableContentDefinition {

  /** The columns of the index */
  private List<String> fColumns;

  /**
   * Constructor
   * @param id index identifier
   * @param columns the colums of the index
   * @param dbSysHints
   * @param comment
   * @throws MetaException
   */
  public SqlIndex(String id, List<String> columns, List<SqlDatabaseSystemHints> dbSysHints, String comment)
          throws MetaException {
    super(generateId("IX", id, columns), dbSysHints, comment);
    assert columns != null : "Parameter columns must not be null";
    assert columns.size() > 0 : "Paramter columns must contain at least one element";
    fColumns = columns;
  }

  /**
   * Gets the columns of the index
   * @return columns
   */
  public List<String> getColumns() {
    return fColumns;
  }

  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(512);
    builder.append("INDEX ");
    builder.append(getId());
    builder.append(" (");
    SqlScriptUtil.formatCommaSeparated(builder, fColumns);
    builder.append(")");
    return builder.toString();
  }
}
