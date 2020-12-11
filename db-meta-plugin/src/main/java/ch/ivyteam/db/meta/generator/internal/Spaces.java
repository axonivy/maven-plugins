package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;

public final class Spaces
{
  public void generate(PrintWriter pr, int numberOfSpaces)
  {
    for (int pos = 0; pos < numberOfSpaces; pos++)
    {
      pr.append(" ");
    }
  }
}
