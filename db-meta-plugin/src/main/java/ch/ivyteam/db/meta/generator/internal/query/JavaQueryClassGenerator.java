package ch.ivyteam.db.meta.generator.internal.query;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import ch.ivyteam.db.meta.generator.internal.JavaClassGenerator;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;

public class JavaQueryClassGenerator extends JavaClassGenerator
{
  
  private static final String OPTION_TEMPLATE_DIR = "templateDir";
  private static final String OPTION_SOURCE_DIR = "sourceDir";
  private File templateDir;
  private File sourceDir;

  public JavaQueryClassGenerator()
  {
    options.addOption(Option.builder().desc("Template directory (e.g. for Public API)").required().hasArg().longOpt(OPTION_TEMPLATE_DIR).build());
    options.addOption(Option.builder().desc("Source directory (e.g. for java source templates)").required().hasArg().longOpt(OPTION_SOURCE_DIR).build());
  }
  
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

    File javaSourceFile = new File(getTargetDirectory(), className+".java");
    javaSourceFile.getParentFile().mkdirs();
    
    javaQueryClassTemplate.writeToFile(javaSourceFile);
  }
}