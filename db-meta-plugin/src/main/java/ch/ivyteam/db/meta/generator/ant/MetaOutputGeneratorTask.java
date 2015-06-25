package ch.ivyteam.db.meta.generator.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.FileSet;

import ch.ivyteam.db.meta.generator.MetaOutputGenerator;

/**
 * Ant task that generates output from a meta definition
 * @author rwei
 * @since 01.10.2009
 */
public class MetaOutputGeneratorTask extends Task
{
  /** The output file or directory*/
  private File output;
  
  /** The generator */
  private String generator;
  
  /** The input */
  private File input;
  
  /** The inputs */
  private List<FileSet> inputFileSets = new ArrayList<FileSet>();
    
  /** Additional generator arguments */
  private List<Argument> arguments = new ArrayList<Argument>();

  /**
   * @see org.apache.tools.ant.Task#execute()
   */
  @Override
  public void execute() throws BuildException
  {
    DirectoryScanner directoryScanner;
    List<String> args = new ArrayList<String>();

    args.add("-sql");
    for (FileSet fileSet : inputFileSets)
    {
      directoryScanner = fileSet.getDirectoryScanner(getProject());
      for (String file : directoryScanner.getIncludedFiles())
      {
        args.add(new File(fileSet.getDir(), file).getAbsolutePath());
      }
    }
    if (input != null)
    {
      args.add(input.getAbsolutePath());
    }
    args.add("-generator");
    args.add(generator);
    if (output.isDirectory())
    {
      args.add("-outputDir");
      args.add(output.getAbsolutePath());
    }
    else
    {
      args.add("-outputFile");
      args.add(output.getAbsolutePath());
    }
    for (Argument arg : arguments)
    {
      for (String part: arg.getParts())
      {        
        args.add(part);
      }
    }
    try
    {
      MetaOutputGenerator.mainWithoutSystemExit(args.toArray(new String[args.size()]));
    }
    catch(Throwable ex)
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
   * Returns the input
   * @return the input
   */
  public File getInput()
  {
    return input;
  }

  /**
   * Sets the input to the given parameter
   * @param input the input to set
   */
  public void setInput(File input)
  {
    this.input = input;
  }
  
  /**
   * Creates an input file set
   * @return input file set
   */
  public FileSet createInputs()
  {
    FileSet newFileset = new FileSet();
    inputFileSets.add(newFileset);
    return newFileset; 

  }
  
  /**
   * Creates a new generator argument
   * @return argument
   */
  public Argument createGeneratorArg()
  {
    Argument newArg;
    
    newArg = new Argument();
    arguments.add(newArg);
    return newArg;
  }
  
  
  
}
