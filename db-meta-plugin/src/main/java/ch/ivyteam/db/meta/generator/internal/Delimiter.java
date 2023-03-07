package ch.ivyteam.db.meta.generator.internal;

import java.io.PrintWriter;

public final class Delimiter {

  public static final Delimiter STANDARD = new Delimiter(";");
  private final String delimiter;

  public Delimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public void generate(PrintWriter pr) {
    pr.append(delimiter);
  }
}
