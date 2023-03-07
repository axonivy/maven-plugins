package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class NewLinePrintWriter extends PrintWriter {

  private static final char NEW_LINE_CHAR = '\n';

  public NewLinePrintWriter(File file) throws FileNotFoundException {
    super(file);
  }

  @Override
  public void println() {
    write(NEW_LINE_CHAR);
  }
}
