package ch.ivyteam.db.meta.generator.internal;

public class ConstantBuilder
{
  private String camelCaseString;

  public ConstantBuilder(String camelCaseString)
  {
    this.camelCaseString = camelCaseString;    
  }
  
  public String toConstant()
  {
    if (camelCaseString.length() <= 5)
    {
      return camelCaseString.toUpperCase();
    }
    StringBuilder sb = new StringBuilder(camelCaseString.length() + 5);
    sb.append(Character.toUpperCase(camelCaseString.charAt(0)));
    for (int i = 1; i < camelCaseString.length(); i++)
    {
      char ch = camelCaseString.charAt(i);
      if (Character.isUpperCase(ch))
      {
        sb.append('_');
        sb.append(ch);
      }
      else
      {
        sb.append(Character.toUpperCase(ch));
      }
    }
    return sb.toString();
  }  
}
