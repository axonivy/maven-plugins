package ch.ivyteam.bitbucket.model.paged;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResult<T>
{
  private List<T> values;
  private URI next;

  public List<T> getValues()
  {
    return values;
  }

  public void setValues(List<T> values)
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
