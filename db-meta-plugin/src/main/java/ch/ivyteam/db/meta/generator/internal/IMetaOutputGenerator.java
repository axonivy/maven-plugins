package ch.ivyteam.db.meta.generator.internal;

import java.io.File;

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
   * Gets either 
   * <ul>
   *    <li>the directory this generator will create files into when calling {@link #generateMetaOutput(SqlMeta)}</li>
   *    <li>the file this generator will create when calling {@link #generateMetaOutput(SqlMeta)}</li>
   * </ul>
   * @return file or directory
   */
  public File getTargetDirectoryOrFile();
}
