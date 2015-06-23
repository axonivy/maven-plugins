package ch.ivyteam.db.meta.model.internal;

import java.util.List;


/**
 * An sql trigger
 * @author rwei
 * @since 12.10.2009
 */
public class SqlTrigger extends SqlTableContentDefinition
{

  /** The statements to execute if the trigger is fired for each row */
  private List<SqlDmlStatement> fStatementsForEachRow;
  /** The statements to execute if the trigger is fired for each statement */
  private List<SqlDmlStatement> fStatementsForEachStatement;
  /** The name of the table on which a delete triggers this trigger */
  private String fTableName;

  /**
   * Constructor
   * @param triggerName
   * @param tableName 
   * @param stmtsForEachRow
   * @param stmtsForEachStatement 
   * @param dbSysHints
   * @param comment
   * @throws MetaException 
   */
  public SqlTrigger(String triggerName, String tableName, List<SqlDmlStatement> stmtsForEachRow, List<SqlDmlStatement> stmtsForEachStatement, List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(generateTriggerId(triggerName, tableName), dbSysHints, comment);
    assert stmtsForEachRow != null : "Parameter stmtsForEachRow must not be null";
    fStatementsForEachRow = stmtsForEachRow;    
    assert stmtsForEachStatement != null : "Parameter stmtsForEachStatements must not be null";
    fStatementsForEachStatement = stmtsForEachStatement;    
    fTableName = tableName;
  }

  /**
   * Generates a trigger id 
   * @param triggerName the name of the trigger
   * @param table the table the trigger triggers on
   * @return trigger id
   */
  private static String generateTriggerId(String triggerName, String table)
  {
    assert table != null : "Parameter table must not be null";
    if (triggerName != null)
    {
      return triggerName;
    }
    return "TG_DELETE_"+table;
  }

  /**
   * Returns the statements that are used in for each row triggers
   * @return the statements
   */
  public List<SqlDmlStatement> getStatementsForEachRow()
  {
    return fStatementsForEachRow;
  }
  
  /**
   * Returns the statements that are used in for each statement triggers
   * @return the statements
   */
  public List<SqlDmlStatement> getStatementsForEachStatement()
  {
    return fStatementsForEachStatement;
  }
  
  /**
   * Returns the tableName
   * @return the tableName
   */
  public String getTableName()
  {
    return fTableName;
  }
  
  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(32124);
    builder.append("TRIGGER ");
    builder.append(getId());
    builder.append(" AFTER DELETE ON ");
    builder.append(fTableName);
    builder.append("\nBEGIN");
    for (SqlDmlStatement stmt : fStatementsForEachRow)
    {
      builder.append("  ");
      builder.append(stmt);
      builder.append("\n");
    }
    builder.append("END");
    
    return super.toString();
  }

}
