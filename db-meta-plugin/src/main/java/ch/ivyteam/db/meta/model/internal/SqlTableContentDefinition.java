package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * Table content definition
 * @author rwei
 * @since 01.10.2009
 */
public class SqlTableContentDefinition extends SqlObject {

  /**
   * Constructor
   * @param id
   * @param dbSysHints
   * @param comment
   * @throws MetaException
   */
  public SqlTableContentDefinition(String id, List<SqlDatabaseSystemHints> dbSysHints, String comment)
          throws MetaException {
    super(id, dbSysHints, comment);
  }
}
