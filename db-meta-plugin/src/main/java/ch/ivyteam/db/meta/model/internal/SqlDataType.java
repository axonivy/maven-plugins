package ch.ivyteam.db.meta.model.internal;

/**
 * An sql data type definition
 * @author rwei
 */
public class SqlDataType {

  /** The data type */
  private DataType fDataType;
  /** The length */
  private int fLength;
  /** The precision */
  private int fPrecision;

  /**
   * Constructor
   * @param dataType the data type
   * @param length the length of the data type
   * @param precision the precision of the data type
   */
  public SqlDataType(DataType dataType, Integer length, Integer precision) {
    assert dataType != null : "Parameter dataType must not be null";
    fDataType = dataType;
    fLength = length;
    fPrecision = precision;
  }

  /**
   * Constructor
   * @param dataType the data type
   * @param length the length of the data type
   */
  public SqlDataType(DataType dataType, Integer length) {
    this(dataType, length, -1);
  }

  /**
   * Constructor
   * @param dataType the data type
   */
  public SqlDataType(DataType dataType) {
    this(dataType, -1);
  }

  /**
   * Get the data type
   * @return data type
   */
  public DataType getDataType() {
    return fDataType;
  }

  /**
   * Gets the length
   * @return length
   */
  public int getLength() {
    return fLength;
  }

  /**
   * Gets the precision
   * @return precision
   */
  public int getPrecision() {
    return fPrecision;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(128);
    builder.append(fDataType);
    if (fLength >= 0) {
      builder.append("(");
      builder.append(fLength);
      if (fPrecision >= 0) {
        builder.append(", ");
        builder.append(fPrecision);
      }
      builder.append(")");
    }
    return builder.toString();
  }

  /**
   * Sql data type
   * @author rwei
   */
  public static enum DataType {
    /** Date time */
    DATETIME,
    /** Date */
    DATE,
    /** Time */
    TIME,
    /** CLOB */
    CLOB,
    /** BLOB */
    BLOB,
    /** INTEGER : NUMBER(10) */
    INTEGER,
    /** BIGINT : NUMBER(20) */
    BIGINT,
    /** NUMBER */
    NUMBER,
    /** DECIMAL */
    DECIMAL,
    /** FLOAT */
    FLOAT,
    /** VARCHAR */
    VARCHAR,
    /** BIT */
    BIT,
    /** CHAR */
    CHAR
  }
}
