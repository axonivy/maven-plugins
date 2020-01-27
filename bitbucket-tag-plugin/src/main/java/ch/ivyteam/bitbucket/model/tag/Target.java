package ch.ivyteam.bitbucket.model.tag;

import ch.ivyteam.bitbucket.model.commit.Commit;

public class Target
{
  private String hash;

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
