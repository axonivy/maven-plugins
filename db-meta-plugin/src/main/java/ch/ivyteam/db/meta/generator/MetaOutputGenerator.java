package ch.ivyteam.db.meta.generator;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;

import ch.ivyteam.db.meta.generator.internal.IMetaOutputGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.parser.internal.Parser;
import ch.ivyteam.db.meta.parser.internal.Scanner;

public class MetaOutputGenerator
{
  private static final String OPTION_GENERATOR = "generator";
  private static final String OPTION_SQL = "sql";
  private List<File> sqlMetaDefinitionFiles = new ArrayList<File>();
  private IMetaOutputGenerator generator;
  private SqlMeta sqlMetaDefinition;

  private Options OPTIONS = new Options().
    addOption(Option.builder().desc("*.meta input files").required().hasArgs().longOpt(OPTION_SQL).build()).
    addOption(Option.builder().desc("Name of the generator class").required().hasArg().longOpt(OPTION_GENERATOR).build());

  /**
   * Main method
   * <code>-sql {file}</code>
   * <code>-generator {class}</code>
   * <code>{generator args}</code>
   * @param args
   */
  public static void main(String[] args)
  {
    try
    {
      mainWithoutSystemExit(args);
      System.exit(0);
    }
    catch(Throwable ex)
    {
      System.exit(-1);
    }
  }

  /**
   * Main method
   * <code>-sql {file}</code>
   * <code>-generator {class}</code>
   * <code>{generator args}</code>
   * @param args
   * @throws Throwable
   */
  public static void mainWithoutSystemExit(String[] args) throws Throwable
  {
    MetaOutputGenerator generator = new MetaOutputGenerator();
    try
    {
      generator.analyseArgs(args);
      generator.parseMetaDefinition();
      generator.generateMetaOutput();
      System.out.println("Successful generated meta information output");
    }
    catch(Throwable ex)
    {
      System.err.println("Error:");
      System.err.println(ex.getMessage());
      ex.printStackTrace();
      generator.printHelp();
      throw ex;
    }
  }

  public void parseMetaDefinition() throws Exception
  {
    FileReader fr;
    Parser parser;
    Scanner scanner;

    for (File sqlMetaDefinitionFile : sqlMetaDefinitionFiles)
    {
      fr = new FileReader(sqlMetaDefinitionFile);
      try
      {
        scanner = new Scanner(fr);
        parser = new Parser(scanner);
        if (sqlMetaDefinition == null)
        {
          sqlMetaDefinition = (SqlMeta)parser.parse().value;
        }
        else
        {
          sqlMetaDefinition.merge((SqlMeta)parser.parse().value);
        }

      }
      finally
      {
        IOUtils.closeQuietly(fr);
      }
    }
  }

  /**
   * Generators the meta output
   * @throws Exception if generation fails
   */
  public void generateMetaOutput() throws Exception
  {
    assert generator != null;
    generator.generateMetaOutput(sqlMetaDefinition);
  }

  /**
   * Prints the help
   */
  public void printHelp()
  {
    new HelpFormatter().printHelp(getClass().getSimpleName(), OPTIONS);
    if (generator != null)
    {
      System.out.println();
      System.out.print("Generator ");
      generator.printHelp();
    }
  }

  public void analyseArgs(String[] args) throws Exception
  {
    var commandLine = new DefaultParser().parse(OPTIONS, args, true);
    for (String sqlFile : commandLine.getOptionValues(OPTION_SQL))
    {
      File sqlMetaDefinitionFile = new File(sqlFile);
      if (!sqlMetaDefinitionFile.exists())
      {
        throw new Exception("Sql file '"+sqlMetaDefinitionFile.getPath() +"' does not exists");
      }
      sqlMetaDefinitionFiles.add(sqlMetaDefinitionFile);
    }
    var generatorClass = Class.forName(commandLine.getOptionValue(OPTION_GENERATOR));
    if (IMetaOutputGenerator.class.isAssignableFrom(generatorClass))
    {
      generator = (IMetaOutputGenerator) generatorClass.getDeclaredConstructor().newInstance();
    }
    generator.analyseArgs(commandLine.getArgs());
  }

  public Target getTarget() {
    return generator.getTarget();
  }
}
