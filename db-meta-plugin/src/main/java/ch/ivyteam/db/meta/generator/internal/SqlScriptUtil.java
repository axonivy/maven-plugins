package ch.ivyteam.db.meta.generator.internal;

import java.util.Collection;

/**
 * Utility class for sql script generation
 * @author rwei
 * @since 12.10.2009
 */
public class SqlScriptUtil
{
  /**
   * Formats a comma separated list with the entries of the given collection. 
   * The entry of the collection are formated using the {@link java.lang.Object#toString()} method 
   * @param builder the builder to format the list into 
   * @param collection the collection to format
   */
  public static void formatCommaSeparated(StringBuilder builder, Collection<?> collection)
  {
    boolean first = true;
    for (Object entry: collection)
    {
      if (!first)
      {
        builder.append(", "); 
      }
      first = false;
      builder.append(entry);
    }
  }
}
