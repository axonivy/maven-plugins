package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * Sql Data Modification Statement
 * @author rwei
 * @since 12.10.2009
 */
public class SqlDmlStatement extends SqlArtifact
{

  /**
   * Constructor
   * @param dbSysHints
   * @param comment
   * @throws MetaException
   */
  public SqlDmlStatement(List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(dbSysHints, comment);
  }

}
