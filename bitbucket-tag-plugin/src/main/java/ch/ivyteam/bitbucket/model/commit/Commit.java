package ch.ivyteam.bitbucket.model.commit;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Commit
{
  private String hash;
  private Summary summary;
  private Author author;
  private String date;

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
    return "Commit [hash="+hash+", summary="+summary+", author="+author+", date="+date+"]";
  }

  public Summary getSummary()
  {
    return summary;
  }

  public void setSummary(Summary summary)
  {
    this.summary = summary;
  }

  public Author getAuthor()
  {
    return author;
  }

  public void setAuthor(Author author)
  {
    this.author = author;
  }

  public String getDate()
  {
    return date;
  }

  public void setDate(String date)
  {
    this.date = date;
  }

  public String getShortDisplayString()
  {
    return getHash()
        + " " 
        + getAuthor().getRaw()
        + " "
        + getDate() 
        + " "
        + StringUtils.substringBefore(getSummary().getRaw(), "\n"); 
  }
}
