package ch.ivyteam.bitbucket.model.commit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Author
{
  private String raw;

  public String getRaw()
  {
    return raw;
  }

  public void setRaw(String raw)
  {
    this.raw = raw;
  }
  
  @Override
  public String toString()
  {
    return "Author [raw="+raw+"]";
  }
  
  
}
