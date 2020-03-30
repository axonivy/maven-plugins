package ch.ivyteam.bitbucket.model.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import ch.ivyteam.bitbucket.model.commit.Commit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Target
{
  private String hash;

  public Target()
  {
  }
  
  public Target(Commit target)
  {
    this(target.getHash());
  }

  public Target(String hash)
  {
    this.hash = hash;
  }

  public String getHash()
  {
    return hash;
  }

  public void setHash(String hash)
  {
    this.hash = hash;
  }
  
  @Override
  public String toString()
  {
    return "Target [hash="+hash+"]";
  }
}
