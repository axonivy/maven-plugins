package ch.ivyteam.db.meta.generator.internal.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Publishes information for Enumerations
 */
public class EnumerationInfo
{
  private final String enumerationName;

  /**
   * Constructor
   * @param enumerationName 
   */
  public EnumerationInfo(String enumerationName)
  {
    this.enumerationName = enumerationName;
  }
  
  /**
   * @return -
   */
  public String getName()
  {
    return enumerationName;
  }

  /**
   * @return -
   */
  public String getSimpleName()
  {
    return StringUtils.substringAfterLast(enumerationName, ".");
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj)
  {
    if (!(obj instanceof EnumerationInfo))
    {
      return false;
    }
    EnumerationInfo otherObject = (EnumerationInfo) obj; 
    return new EqualsBuilder().append(getName(), otherObject.getName()).isEquals();
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return new HashCodeBuilder().append(getName()).toHashCode();
  }
}