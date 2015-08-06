package ch.ivyteam.db.meta.generator.internal;

import ch.ivyteam.db.meta.generator.Target;
import ch.ivyteam.db.meta.model.internal.SqlMeta;

/**
 * Defines an interface for meta output generators
 * @author rwei
 * @since 01.10.2009
 */
public interface IMetaOutputGenerator
{
  /**
   * Generates the meta output
   * @param metaDefinition the meta definition
   * @throws Exception if generation fails
   */
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception;

  /** 
   * Analysis the generator arguments
   * @param generatorArgs generator arguments
   * @throws Exception if analyses fails
   */
  public void analyseArgs(String[] generatorArgs) throws Exception;
  
  /**
   * Prints a help about the generator arguments
   */
  public void printHelp();

  /**
   * Gets information about the files that are generated when calling {@link #generateMetaOutput(SqlMeta)}
   * @return target
   */
  public Target getTarget();
}
