package ch.ivyteam.db.meta.model.internal;

/**
 * SQL Null
 * @author rwei
 * @since 01.10.2009
 */
public class SqlNull {

  /** The sql null value */
  private static final SqlNull fInstance = new SqlNull();

  /**
   * Constructor
   */
  private SqlNull() {
    super();
  }

  /**
   * Gets the instance
   * @return instance
   */
  public static final SqlNull getInstance() {
    return fInstance;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "NULL";
  }
}
