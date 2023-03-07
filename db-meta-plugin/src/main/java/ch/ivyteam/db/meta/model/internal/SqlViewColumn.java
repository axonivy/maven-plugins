package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * A sql view column definition
 * @author rwei
 * @since 02.10.2009
 */
public class SqlViewColumn extends SqlObject {

  /**
   * Constructor
   * @param id
   * @param dbSysHints
   * @param comment
   * @throws MetaException
   */
  public SqlViewColumn(String id, List<SqlDatabaseSystemHints> dbSysHints, String comment)
          throws MetaException {
    super(id, dbSysHints, comment);
  }
}
