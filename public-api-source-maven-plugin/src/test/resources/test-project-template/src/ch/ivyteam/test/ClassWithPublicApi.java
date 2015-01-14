package ch.ivyteam.test;

import ch.ivyteam.api.IvyScriptVisibility;
import ch.ivyteam.api.PublicAPI;

@PublicAPI(IvyScriptVisibility.ADVANCED)
public class ClassWithPublicApi
{
  public String doSomething()
  {
    String aString = "hallo";
    System.out.println(aString);
    return aString;
  }
}
