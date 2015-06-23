package ch.ivyteam.db.meta.generator.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Always pirnts a <code>\n</code> for a new line under all operating systems.
 * @author Christian Strebel
 * @since 12.12.2011
 */
public class NewLinePrintWriter extends PrintWriter
{
  /** New line character */
  private static final char NEW_LINE_CHAR = '\n';
  
  /**
   * Constructor
   * @param out
   */
  public NewLinePrintWriter(OutputStream out)
  {
    super(out);
  }

  /**
   * Constructor
   * @param file
   * @param csn
   * @throws FileNotFoundException
   * @throws UnsupportedEncodingException
   */
  public NewLinePrintWriter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException
  {
    super(file, csn);
  }

  /**
   * Constructor
   * @param file
   * @throws FileNotFoundException
   */
  public NewLinePrintWriter(File file) throws FileNotFoundException
  {
    super(file);
  }

  /**
   * Constructor
   * @param out
   * @param autoFlush
   */
  public NewLinePrintWriter(OutputStream out, boolean autoFlush)
  {
    super(out, autoFlush);
  }

  /**
   * Constructor
   * @param fileName
   * @param csn
   * @throws FileNotFoundException
   * @throws UnsupportedEncodingException
   */
  public NewLinePrintWriter(String fileName, String csn) throws FileNotFoundException,
          UnsupportedEncodingException
  {
    super(fileName, csn);
  }

  /**
   * Constructor
   * @param fileName
   * @throws FileNotFoundException
   */
  public NewLinePrintWriter(String fileName) throws FileNotFoundException
  {
    super(fileName);
  }

  /**
   * Constructor
   * @param out
   * @param autoFlush
   */
  public NewLinePrintWriter(Writer out, boolean autoFlush)
  {
    super(out, autoFlush);
  }

  /**
   * Constructor
   * @param out
   */
  public NewLinePrintWriter(Writer out)
  {
    super(out);
  }

  /**
   * @see java.io.PrintWriter#println()
   */
  @Override
  public void println()
  {
    write(NEW_LINE_CHAR);
  }
}
