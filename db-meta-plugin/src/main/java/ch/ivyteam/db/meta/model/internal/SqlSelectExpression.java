package ch.ivyteam.db.meta.model.internal;

import java.util.List;

/**
 * @author rwei
 * @since 02.10.2009
 */
public class SqlSelectExpression extends SqlArtifact
{
  /** The expression */  
  private SqlAtom fExpression;

  /**
   * Constructor
   * @param expression  
   * @param dbSysHints 
   * @param comment
   * @throws MetaException
   */
  public SqlSelectExpression(SqlAtom expression, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(dbSysHints, comment);
    assert expression != null : "Parameter expression must not be null";
    fExpression = expression;
  }

  /**
   * Returns the fExpression
   * @return the fExpression
   */
  public SqlAtom getExpression()
  {
    return fExpression;
  }
  
  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return fExpression.toString();
  }


}
