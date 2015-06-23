package ch.ivyteam.db.meta.generator.internal;

import java.io.File;

import ch.ivyteam.db.meta.model.internal.SqlForeignKey;


/**
 * Abstract generator class that is used to generated sorted table content files
 * @author rwei
 * @since 15.09.2011
 */
public abstract class AbstractSortedTableContentListGenerator implements IMetaOutputGenerator
{
  /** The output file */
  File fOutputFile;
  
  /** The database system to generate */
  protected String fDatabaseSystem;
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#analyseArgs(java.lang.String[])
   */
  @Override
  public void analyseArgs(String[] generatorArgs) throws Exception
  {
    if (generatorArgs.length < 4)
    {
      throw new Exception("There must be at least 4 generator options");
    }
    if (!generatorArgs[0].equalsIgnoreCase("-outputFile"))
    {
      throw new Exception("First generator option must be -outputFile");
    }
    fOutputFile = new File(generatorArgs[1]);
    if (!fOutputFile.exists())
    {
      fOutputFile.getParentFile().mkdirs();
    }
    if (!generatorArgs[2].equalsIgnoreCase("-databaseSystem"))
    {
      throw new Exception("Second generator option must be -databaseSystem");
    }
    fDatabaseSystem = generatorArgs[3];
    
  }

  /**
   * Checks if the foreign key gets generated for the given database system
   * @param foreignKey the foreign key
   * @return true if foreign key is generated, otherwise false
   */
  protected boolean isForeignKeyGenerated(SqlForeignKey foreignKey)
  {
    return !foreignKey.getDatabaseManagementSystemHints(fDatabaseSystem).isHintSet(SqlScriptGenerator.NO_REFERENCE);
  }

}
