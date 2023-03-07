package ch.ivyteam.db.meta.model.internal;

/**
 * Database System Hint
 * @author rwei
 */
public class SqlDatabaseSystemHint {

  /** The name of the hint */
  private String fName;
  /** The optional value of the hint */
  private String fValue;

  /**
   * Constructor
   * @param name the name of the hint
   * @param value the value of the hint
   */
  public SqlDatabaseSystemHint(String name, String value) {
    assert name != null : "Parameter name must not be null";
    fName = name;
    fValue = value;
  }

  /**
   * Gets the name
   * @return name
   */
  public String getName() {
    return fName;
  }

  /**
   * Gets the value or null
   * @return value or null
   */
  public String getValue() {
    return fValue;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return fName + "=" + fValue;
  }
}
