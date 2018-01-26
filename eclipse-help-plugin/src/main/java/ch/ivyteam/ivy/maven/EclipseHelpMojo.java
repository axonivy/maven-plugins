package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

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
    Map<String, Link> ids = parseContextIds();
    modifyContent(ids);
  }

  /**
   * Parse the html help files and extract the contextIDs
   * e.g. <h3 class="title"><a name="ivy.samplechapter.typo"></a>Typographic Conventions</h3>
   * @return a hashtable contextID -> [fileName, title] 
   * @throws MojoExecutionException 
   */
  private Map<String, Link> parseContextIds() throws MojoExecutionException
  {
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(helpFiles.getDirectory());
    ds.setIncludes(helpFiles.getIncludes().toArray(new String[0]));
    ds.scan();
    
    Map<String, Link> cids = new Hashtable<>();
    for (String includeFilePath : ds.getIncludedFiles())
    {
      File helpFile = new File(ds.getBasedir(), includeFilePath);
      String helpContent = getContent(helpFile);
      includeFilePath = includeFilePath.replace("\\", "/");
      parseHelpFile(cids, includeFilePath, helpContent);
    }
    return cids;
  }

  private String getContent(File helpFile) throws MojoExecutionException
  {
    getLog().debug("Reading help file: " + helpFile.getAbsolutePath());
    try
    {
      return FileUtils.readFileToString(helpFile);
    }
    catch(IOException ex)
    {
      throw new MojoExecutionException("Can not read input help file: "+ helpFile.getAbsolutePath() , ex);
    }
  }
  
  private void parseHelpFile(Map<String, Link> cids, String relativeFilePath, String content)
          throws MojoExecutionException
  {
    String ivyLinkStart = "<a name=\"ivy.";
    int idPos = content.indexOf(ivyLinkStart);
    while (idPos >=0)
    {
      int idEnd = content.indexOf("</a>",idPos);
      if (idEnd < 0)
      { 
        throw new MojoExecutionException("Help file " + relativeFilePath + " corrupt! Anchor <a name=\"ivy. not closed with </a>");
      } 
      idPos = idPos + 9;
      idEnd = idEnd - 2;
      String contextid = content.substring(idPos, idEnd);
      idEnd = idEnd + 6;
      int titleEnd = content.indexOf('<', idEnd);
      if (idEnd < 0)
      { 
        throw new MojoExecutionException("Help file " + relativeFilePath + "corrupt! Anchor <a name=\"ivy....</a> found but no following title text ");
      }
      String contextTitle = content.substring(idEnd, titleEnd);
      cids.put(contextid, new Link(contextTitle, relativeFilePath));
      
      idPos = content.indexOf(ivyLinkStart, idEnd);
    }
  }
  
  /**
   * Modifies the content. Expand the topic chapterid with the actual html.filename 
   * @param contextIds 
   * @throws MojoExecutionException 
   */
  private void modifyContent(Map<String, Link> contextIds) throws MojoExecutionException
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

  static class Link
  {
    private final String label;
    private final String href;
    
    public Link(String label, String href)
    {
      this.label = label;
      this.href = href;
    }
    
    public String getLabel()
    {
      return label;
    }
    
    public String getHref()
    {
      return href;
    }
  }
}
