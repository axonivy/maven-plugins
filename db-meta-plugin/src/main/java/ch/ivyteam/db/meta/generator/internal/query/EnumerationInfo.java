package ch.ivyteam.db.meta.generator.internal.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class EnumerationInfo
{
  private final String enumerationName;

  public EnumerationInfo(String enumerationName)
  {
    this.enumerationName = enumerationName;
  }
  
  public String getName()
  {
    return enumerationName;
  }

  public String getSimpleName()
  {
    return StringUtils.substringAfterLast(enumerationName, ".");
  }
  
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

  @Override
  public int hashCode()
  {
    return new HashCodeBuilder().append(getName()).toHashCode();
  }
}