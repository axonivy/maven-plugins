package ch.ivyteam.bitbucket.model.commit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Commits
{
  private List<Commit> values;

  public List<Commit> getValues()
  {
    return values;
  }

  public void setValues(List<Commit> values)
  {
    this.values = values;
  }
}
