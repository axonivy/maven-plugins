package ch.ivyteam.xml;

import java.util.StringTokenizer;

import org.apache.commons.lang3.StringEscapeUtils;


public class XmlUtil
{

  /**
   * Escapes HTML of source and replaces all newline characters by <code>&lt;br/></code>.
   * @param source the original string
   * @return The escaped string
   */
  public static String escapeHtmlAndConvertNewline(String source)
  {
    if (source==null)
    {
      return null;
    }
    source = unifyLineSeparators(source);
    
    StringBuffer result = new StringBuffer(source.length());
    StringTokenizer tokenizer = new StringTokenizer(source, "\n", true);
    while (tokenizer.hasMoreTokens())
    {
      String token = tokenizer.nextToken();
      if (token.equals("\n"))
      {
        result.append("<br />");
      }
      else
      {
        result.append(StringEscapeUtils.escapeHtml4(token));
      }
    }
    return result.toString();
  }
  
  /**
   * Replaces various line separators with standard "\n".
   * This method undoes the effect of platformLineSeparators().
   * @param source the original string
   * @return the same string with internal line ends (only '\n')
   */
  private static String unifyLineSeparators(String source)
  {
    StringBuffer result = new StringBuffer(source.length());
    int start = 0;
    int last = 0;
    while (last < source.length())
    {
      char c = source.charAt(last);
      switch (c)
      {
        case '\r':
          result.append(source.substring(start, last));
          if (last + 1 < source.length() && source.charAt(last + 1) == '\n')
          {
            last ++;
          }
          start = last + 1;
          result.append('\n');
          break;
        case '\n':
          result.append(source.substring(start, last));
          start = last + 1;
          result.append('\n');
          break;
      }
      last ++;
    }
    result.append(source.substring(start, last));
    return result.toString();
  }

}