package ch.ivyteam.db.meta.model.internal;

/**
 * @author rwei
 * @since 02.10.2009
 */
public class SqlLiteral extends SqlAtom {

  /** Value of the literal */
  private Object fValue;

  /**
   * Constructor
   * @param value
   */
  public SqlLiteral(Object value) {
    fValue = value;
  }

  /**
   * Returns the value
   * @return the value
   */
  public Object getValue() {
    return fValue;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return fValue.toString();
  }
}
