package ch.ivyteam.ivy.changelog.generator.jira;

public class Paging {

  public final int startAt;
  public final int maxResults;
  public final int total;

  public Paging(int startAt) {
    this(startAt, -1, -1);
  }

  public Paging(int startAt, int maxResults, int total) {
    this.startAt = startAt;
    this.maxResults = maxResults;
    this.total = total;
  }

  public boolean hasNext() {
    if (total == -1) {
      return true;
    }
    return total > startAt+maxResults;
  }

  public Paging next() {
    if (!hasNext()) {
      return null;
    }
    return new Paging(startAt+maxResults);
  }
}