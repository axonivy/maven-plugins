package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.xml.sax.SAXException;


@Mojo(name="modify-help-context")
public class EclipseHelpMojo extends AbstractMojo
{
  @Parameter(required = true)
  File sourceContext;
  @Parameter(required = true)
  File targetContext;
  @Parameter(required = true)
  FileSet helpFiles;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    Hashtable<String,String[]> ids = parseContextIds();
    modifyContent(ids);
  }

  /**
   * Parse the html help files and extract the contextIDs
   * e.g. <h3 class="title"><a name="ivy.samplechapter.typo"></a>Typographic Conventions</h3>
   * @return a hashtable contextID -> [fileName, title] 
   * @throws MojoExecutionException 
   */
  private Hashtable<String, String[]> parseContextIds() throws MojoExecutionException
  {
    String entry = "<a name=\"ivy.";
    Hashtable<String, String[]> cids = new Hashtable<String, String[]>();
    String[] item = new String[2];
    
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(helpFiles.getDirectory());
    ds.setIncludes(helpFiles.getIncludes().toArray(new String[0]));
    ds.scan();
    
    String[] files = ds.getIncludedFiles();
    String content;
    int idPos = 0;
    int idEnd = 0; 
    int titleEnd = 0;
    String contextid = "";
    String contextTitle = "";
    String filename = "";
    for (int i = 0; i < files.length; i++)
    {
      File helpfile = new File(ds.getBasedir(), files[i]);
      getLog().info("Help file parsed: " + helpfile.getAbsolutePath());
      try
      {
        content = FileUtils.readFileToString(helpfile);
      }
      catch(IOException ex)
      {
        throw new MojoExecutionException("Can not read input help file: "+helpfile.getAbsolutePath() , ex);
      }
      idPos = content.indexOf(entry);
      while (idPos >=0)
      {
        idEnd = content.indexOf("</a>",idPos);
        if (idEnd < 0)
        { 
          throw new MojoExecutionException("Help file " + helpfile.getAbsolutePath() + " corrupt! Anchor <a name=\"ivy. not closed with </a>");
        } 
        idPos = idPos + 9;
        idEnd = idEnd - 2;
        contextid = content.substring(idPos, idEnd);
        idEnd = idEnd + 6;
        titleEnd = content.indexOf('<', idEnd);
        if (idEnd < 0)
        { 
          throw new MojoExecutionException("Help file " + helpfile.getAbsolutePath() + "corrupt! Anchor <a name=\"ivy....</a> found but no following title text ");
        }
        contextTitle = content.substring(idEnd, titleEnd);
        filename = files[i];
        item = new String[]{filename, contextTitle};
        cids.put(contextid, item);
        
        idPos = content.indexOf(entry, idEnd);
      }
    }
    return cids;
  }  
  
  /**
   * Modifies the content. Expand the topic chapterid with the actual html.filename 
   * @param contextIds 
   * @throws MojoExecutionException 
   */
  private void modifyContent(Hashtable<String, String[]> contextIds) throws MojoExecutionException
  {  
    getLog().info("Start modifying context.xml in " + targetContext);
    try
    {
      // parse context file
      SAXParserFactory factory = SAXParserFactory.newInstance(); 
      SAXParser saxParser = factory.newSAXParser();      
      HelpContextHandler handler = new HelpContextHandler(contextIds); 
      saxParser.parse(sourceContext, handler);
      
      // copy new file 
      File newContextFile = handler.getOutputFile();
      FileUtils.copyFile(newContextFile, targetContext);
    }
    catch (ParserConfigurationException ex)
    {
      throw new MojoExecutionException("Cannot configure SAX parser" + "\n" + ex.getMessage());
    }
    catch (SAXException ex)
    {      
      throw new MojoExecutionException("Cannot parse context file " + sourceContext + "\n" + ex.getMessage());
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Could not copy new temporary context file to " + targetContext.getAbsolutePath() + "\n" + ex.getMessage());
    } 
  }
}
