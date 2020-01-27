package ch.ivyteam.bitbucket.model.repo;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Repositories
{
  private List<Repository> values;
  private URI next;

  public List<Repository> getValues()
  {
    return values;
  }

  public void setValues(List<Repository> values)
  {
    this.values = values;
  }

  public URI getNext()
  {
    return next;
  }

  public void setNext(URI next)
  {
    this.next = next;
  }
}
