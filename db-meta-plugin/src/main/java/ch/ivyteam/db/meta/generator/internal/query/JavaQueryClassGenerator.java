package ch.ivyteam.db.meta.generator.internal.query;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

/**
 */
public class JavaQueryClassGenerator extends JavaClassGenerator
{
  
  private static final String OPTION_TEMPLATE_DIR = "templateDir";
  private static final String OPTION_SOURCE_DIR = "sourceDir";
  private File templateDir;
  private File sourceDir;

  /**
   * Constructor
   */
  @SuppressWarnings("static-access")
  public JavaQueryClassGenerator()
  {
    OPTIONS.addOption(OptionBuilder.withDescription("Template directory (e.g. for Public API)").isRequired().hasArg().create(OPTION_TEMPLATE_DIR));
    OPTIONS.addOption(OptionBuilder.withDescription("Source directory (e.g. for java source templates)").isRequired().hasArg().create(OPTION_SOURCE_DIR));
  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.JavaClassGenerator#analyseAdditionalArgs(org.apache.commons.cli.CommandLine)
   */
  @Override
  protected void analyseAdditionalArgs(CommandLine commandLine) throws Exception
  {
    templateDir = new File(commandLine.getOptionValue(OPTION_TEMPLATE_DIR));
    if (!templateDir.exists())
    {
      throw new IllegalArgumentException("Template directory '"+ templateDir.getAbsolutePath() +"' does not exist");
    }
    sourceDir = new File(commandLine.getOptionValue(OPTION_SOURCE_DIR));
    if (!sourceDir.exists())
    {
      throw new IllegalArgumentException("Source directory '"+ sourceDir.getAbsolutePath() +"' does not exist");
    }

  }
  
  /**
   * @see ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator#generateMetaOutput(ch.ivyteam.db.meta.model.internal.SqlMeta)
   */
  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    for (String tableName : getTablesToGenerateJavaClassFor())
    {
      SqlTable table = metaDefinition.findTable(tableName);
      if (table == null)
      {
        throw new MetaException("Could not find table "+tableName);
      }
      writeJavaQueryClass(metaDefinition, table);
    }
  }

  private void writeJavaQueryClass(SqlMeta meta, SqlTable table) throws MetaException, IOException
  {
    JavaQueryClassTemplateWriter javaQueryClassTemplate = new JavaQueryClassTemplateWriter(meta, table, getTargetPackage(), templateDir, sourceDir);
    String className = javaQueryClassTemplate.getTableInfo().getQueryClassName();

    File javaSourceFile = new File(getOutputDirectory(), getTargetPackage().replace('.', File.separatorChar)+File.separator+className+".java");
    javaSourceFile.getParentFile().mkdirs();
    
    javaQueryClassTemplate.writeToFile(javaSourceFile);
  }
}