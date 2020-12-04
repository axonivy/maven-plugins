package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;

public final class Comments
{
  public static final Comments STANDARD = new Comments("-- ");
  private final String prefix;

  public Comments(String prefix)
  {
    this.prefix = prefix;
  }

  public void generate(PrintWriter pr, String comment)
  {
    pr.append(prefix);
    pr.append(comment);
    pr.println();
  }
}
