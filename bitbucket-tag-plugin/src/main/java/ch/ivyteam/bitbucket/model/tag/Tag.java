package ch.ivyteam.bitbucket.model.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.ivyteam.bitbucket.model.commit.Commit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag
{
  private String name;
  private String message;
  private Target target;
  
  public Tag()
  {
  }
  
  public Tag(String name, String message, Commit target)
  {
    this.name = name;
    this.message = message;
    this.target = new Target(target);
  }

  public Tag(String name, String message, String targetCommitHash)
  {
    this.name = name;
    this.message = message;
    this.target = new Target(targetCommitHash);
  }

  public String getName()
  {
    return name;
  }
  
  public void setName(String name)
  {
    this.name = name;
  }
  
  public String getMessage()
  {
    return message;
  }
  
  public void setMessage(String message)
  {
    this.message = message;
  }
  
  public Target getTarget()
  {
    return target;
  }
  
  public void setTarget(Target target)
  {
    this.target = target;
  }
  
  @Override
  public String toString()
  {
    return "Tag [name="+name+", message="+message+", target="+target+"]";
  }
}
