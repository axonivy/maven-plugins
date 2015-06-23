package ch.ivyteam.db.meta.model.internal;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** 
 * Sql table column definition
 * @author rwei
 */
public class SqlTableColumn extends SqlTableContentDefinition
{
  /** The data tpye of the column */
  private SqlDataType fDataType;
  /** Flag indicating if the column can store null values */
  private boolean fCanBeNull;
  /** The default value of the column */
  private SqlLiteral fDefaultValue;
  /** Reference definition of the column */
  private SqlReference fReference;
    
  /**
   * Constructor
   * @param id the name
   * @param dataType the data type
   * @param nullOpt can column be null
   * @param defaultOpt the default value
   * @param reference reference definition
   * @param dbSysHints db system hints
   * @param comment comment
   * @throws MetaException if meta information are not consistent
   */
  public SqlTableColumn(String id, SqlDataType dataType, Boolean nullOpt,
          SqlLiteral defaultOpt, SqlReference reference,
          List<SqlDatabaseSystemHints> dbSysHints, String comment) throws MetaException
  {
    super(id, dbSysHints, comment);
    assert dataType != null : "Parameter dataType must not be null";
    fDataType = dataType;
    fCanBeNull = nullOpt != null ? nullOpt : true;
    fDefaultValue = defaultOpt;
    fReference = reference;
  }
    
  /**
   * Data type 
   * @return data type
   */
  public SqlDataType getDataType()
  {
    return fDataType;
  }
  
  /**
   * Gets the default value
   * @return default value
   */
  public SqlLiteral getDefaultValue()
  {
    return fDefaultValue;
  }
  
  /**
   * Gets the reference defintion
   * @return reference defintion. Mey be null.
   */
  public SqlReference getReference()
  {
    return fReference;
  }

  /**
   * Can be null
   * @return can be null
   */
  public boolean isCanBeNull()
  {
    return fCanBeNull;
  }

  /**
   * Sets the reference
   * @param reference the reference to set
   */
  void setReference(SqlReference reference)
  {
    assert reference != null : "Parameter reference must not be null";
    fReference = reference;
  }
  
  /**
   * @see ch.ivyteam.db.meta.model.internal.SqlObject#toString()
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder(512);
    builder.append(getId());
    builder.append(" ");
    builder.append(fDataType);
    if (fDefaultValue != null)
    {
      builder.append(" DEFAULT ");
      builder.append(fDefaultValue);
    }
    builder.append(" ");
    if (fCanBeNull)
    {
      builder.append("NULL");
    }
    else
    {
      builder.append("NOT NULL");
    }
    return builder.toString();
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return new HashCodeBuilder()
      .append(fCanBeNull)
      .append(fDataType)
      .append(fDefaultValue)
      .append(fReference).toHashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    SqlTableColumn other = (SqlTableColumn) obj;
    return new EqualsBuilder()
      .append(other.fCanBeNull, fCanBeNull)
      .append(other.fDataType, fDataType)
      .append(other.fDefaultValue, fDefaultValue)
      .append(other.fReference, fReference).isEquals();
  }
}