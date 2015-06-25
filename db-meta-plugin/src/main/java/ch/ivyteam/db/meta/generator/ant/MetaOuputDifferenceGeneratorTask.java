package ch.ivyteam.db.meta.generator.ant;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import ch.ivyteam.db.meta.generator.MetaOutputDifferenceGenerator;
import ch.ivyteam.db.meta.generator.internal.Db2LuwSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.Db2iSeriesSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.Db2zOsSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.HsqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.MsSqlServerSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.MySqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.NewLinePrintWriter;
import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.PostgreSqlSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.SqlAnywhereSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;

/**
 * Ant task to execute the {@link MetaOutputDifferenceGenerator} from ANT.
 * @author fs
 * @since 29.11.2011
 */
public class MetaOuputDifferenceGeneratorTask extends Task
{
  private File inputFrom;
  private File inputTo;
  private File output;
  private String oldVersionId;
  private String generator;

  /**
   * Generates all conversion scripts to the temp folder
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException
  {
    int oldVersion = 28; // change version if needed
    int newVersion = oldVersion + 1;
    File workspace = new File("..").getCanonicalFile();
    File persistenePlugin = new File(workspace, "ch.ivyteam.ivy.persistence");

    MetaOuputDifferenceGeneratorTask task = new MetaOuputDifferenceGeneratorTask();
    task.setInputFrom(new File(persistenePlugin, "src/ch/ivyteam/ivy/persistence/db/SystemDatabaseVersion" + oldVersion + ".meta"));
    task.setInputTo(new File(persistenePlugin, "meta/SystemDatabase.meta"));
    task.setOldVersionId(String.valueOf(oldVersion));
    
    Map<String, Class<?>> generators = new LinkedHashMap<String, Class<?>>();
    generators.put("Hsql", HsqlSqlScriptGenerator.class);
    generators.put("PostgreSql", PostgreSqlSqlScriptGenerator.class);
    generators.put("SqlAnywhere", SqlAnywhereSqlScriptGenerator.class);
    generators.put("MsSqlServer", MsSqlServerSqlScriptGenerator.class);
    generators.put("MySql", MySqlSqlScriptGenerator.class);
    generators.put("Oracle", OracleSqlScriptGenerator.class);
    generators.put("Db2iSeries", Db2iSeriesSqlScriptGenerator.class);
    generators.put("Db2Luw", Db2LuwSqlScriptGenerator.class);
    generators.put("Db2zOs", Db2zOsSqlScriptGenerator.class);
    
    for (Entry<String, Class<?>> entry : generators.entrySet())
    {
      String name = entry.getKey();
      Class<?> generatorClass = entry.getValue();
      task.setGenerator(generatorClass.getName());
      String ouputFileName = "ConvertSystemDatabase" + name + "ToVersion" + newVersion + ".sql";
      File outputFile = new File(FileUtils.getTempDirectory(), ouputFileName);
      System.out.println("Update script for '" +name + "' generated to: " + outputFile);
      task.setOutput(outputFile);
      try 
      {
        task.execute();
      }
      catch(Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }

  /**
   * @see org.apache.tools.ant.Task#execute()
   */
  @Override
  public void execute() throws BuildException
  {
    super.execute();
    
    try
    {
      SqlMeta metaFrom = MetaOutputDifferenceGenerator.parseMetaDefinition(inputFrom);
      SqlMeta metaTo = MetaOutputDifferenceGenerator.parseMetaDefinition(inputTo);
      PrintWriter pr = new NewLinePrintWriter(output);
      try
      {
        SqlScriptGenerator scriptGenerator = MetaOutputDifferenceGenerator.findGeneratorClass(generator);
        int newVersionId = Integer.parseInt(oldVersionId) +1;
        MetaOutputDifferenceGenerator differenceGenerator = new MetaOutputDifferenceGenerator(metaFrom, metaTo, scriptGenerator, newVersionId);
        differenceGenerator.generate(pr);
      }
      finally
      {
        IOUtils.closeQuietly(pr);
      }
    }
    catch(Exception ex)
    {
      log(ex, Project.MSG_ERR);
      throw new ExitStatusException(ex.getMessage(), -1);
    }
    
  }
  
  /**
   * Returns the output
   * @return the output
   */
  public File getOutput()
  {
    return output;
  }

  /**
   * Sets the output to the given parameter
   * @param output the output to set
   */
  public void setOutput(File output)
  {
    this.output = output;
  }

  /**
   * Returns the newVersionId
   * @return the newVersionId
   */
  public String getOldVersionId()
  {
    return oldVersionId;
  }

  /**
   * Sets the newVersionId to the given parameter
   * @param oldVersionId the newVersionId to set
   */
  public void setOldVersionId(String oldVersionId)
  {
    this.oldVersionId = oldVersionId;
  }

  /**
   * Returns the generator
   * @return the generator
   */
  public String getGenerator()
  {
    return generator;
  }

  /**
   * Sets the generator to the given parameter
   * @param generator the generator to set
   */
  public void setGenerator(String generator)
  {
    this.generator = generator;
  }

  /**
   * Returns the inputFrom
   * @return the inputFrom
   */
  public File getInputFrom()
  {
    return inputFrom;
  }

  /**
   * Sets the inputFrom to the given parameter
   * @param inputFrom the inputFrom to set
   */
  public void setInputFrom(File inputFrom)
  {
    this.inputFrom = inputFrom;
  }

  /**
   * Returns the inputTo
   * @return the inputTo
   */
  public File getInputTo()
  {
    return inputTo;
  }

  /**
   * Sets the inputTo to the given parameter
   * @param inputTo the inputTo to set
   */
  public void setInputTo(File inputTo)
  {
    this.inputTo = inputTo;
  }
}