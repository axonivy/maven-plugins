package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;

public final class Spaces
{
  /**
   * Prints spaces
   * @param pr
   * @param numberOfSpaces
   */
  public void generate(PrintWriter pr, int numberOfSpaces)
  {
    for (int pos = 0; pos < numberOfSpaces; pos++)
    {
      pr.append(" ");
    }
  }
}
