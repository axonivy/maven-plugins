package ch.ivyteam.db.meta.generator.internal;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import ch.ivyteam.db.meta.generator.internal.persistency.JavaClassForViewTemplateWriter;
import ch.ivyteam.db.meta.generator.internal.persistency.JavaClassPersistencyServiceImplementationTemplateWriter;
import ch.ivyteam.db.meta.model.internal.MetaException;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlTable;
import ch.ivyteam.db.meta.model.internal.SqlView;

/**
 * A Meta data generator that creates persistency service implementation classes for entity java classes
 * @author rwei
 * @since 16.10.2009
 */
public class JavaClassPersistencyServiceImplementationGenerator extends JavaClassGenerator implements IMetaOutputGenerator
{
  private static final String OPTION_ENTITY_PACKAGE = "entityPackage";
  private String fEntityPackage;

  public JavaClassPersistencyServiceImplementationGenerator()
  {
    options.addOption(Option.builder().desc("Package where the entity classes are located.").required().hasArg().longOpt(OPTION_ENTITY_PACKAGE).build());
  }

  @Override
  protected void analyseAdditionalArgs(CommandLine commandLine) throws Exception
  {
    fEntityPackage = commandLine.getOptionValue(OPTION_ENTITY_PACKAGE);
  }

  @Override
  public void generateMetaOutput(SqlMeta metaDefinition) throws Exception
  {
    for (String tableName : getTablesToGenerateJavaClassFor())
    {
      SqlTable table = metaDefinition.findTable(tableName);
      if (table != null)
      {
        writeJavaClassPersistencyServiceImplementationGenerator(table, metaDefinition);
      }
      else 
      {
        SqlView view = metaDefinition.findView(tableName);
        if (view == null)
        {
          throw new MetaException("Could not find table or view "+tableName);
        }
        writeJavaClassPersistencyServiceImplementationGenerator(view, metaDefinition);
      }
    }
  }

  private void writeJavaClassPersistencyServiceImplementationGenerator(SqlView view, SqlMeta meta) throws Exception
  {
    JavaClassForViewTemplateWriter templateWriter = new JavaClassForViewTemplateWriter(
            view, meta, getTargetPackage());
    
    String entityClassName = JavaClassGeneratorUtil.getEntityClassName(view);
    String className = "Db"+entityClassName;

    File javaSourceFile = new File(getTargetDirectory(), className+".java");
    javaSourceFile.getParentFile().mkdirs();
    
    templateWriter.writeToFile(javaSourceFile);
  }

  private void writeJavaClassPersistencyServiceImplementationGenerator(SqlTable table, SqlMeta meta) throws Exception
  {
    JavaClassPersistencyServiceImplementationTemplateWriter templateWriter = new JavaClassPersistencyServiceImplementationTemplateWriter(
            table, meta, getTargetPackage(), fEntityPackage);
    
    String entityClassName = getEntityClassName(table);
    String className = "Db"+entityClassName;

    File javaSourceFile = new File(getTargetDirectory(), className+".java");
    javaSourceFile.getParentFile().mkdirs();
    
    templateWriter.writeToFile(javaSourceFile);
  }
}
