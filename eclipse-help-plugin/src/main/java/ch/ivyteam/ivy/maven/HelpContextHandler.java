package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>This class implements the SAX handler for reading/writing the context
 * XML file used by eclipse to link help entries with the context sensitive
 * calls in the source code.</p>
 * 
 * <p>The context.xml is written by the build of designer online help plugin.</p>
 * @author mda
 * @since 09.03.2009
 */
public class HelpContextHandler extends DefaultHandler
{
  /** the id of the current element */
  private String topicId;
  
  /** the output file */
  private File output;
  
  /** the output xml stream */
  private FileOutputStream outputStream;
  
  /** the output content handler */
  private ContentHandler outputHandler;
  
  /** Hashtable with contextIDs*/
  private Hashtable<String, String[]> fContextIDs = new Hashtable<String, String[]>();
  
  /** 
   * Constructor
   * @param contextIDs the id's used in the source files
   */
  public HelpContextHandler(Hashtable<String, String[]> contextIDs)
  {
    super();
    fContextIDs = contextIDs;
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#startDocument()
   */
  @Override
  public void startDocument() throws SAXException
  {   
    try
    {
      output = File.createTempFile("linked_context", ".xml");
      System.out.println(output.getAbsolutePath());
      outputStream = new FileOutputStream(output);
      OutputFormat format = new OutputFormat("XML","ISO-8859-1", true);
      format.setIndent(1);
      format.setIndenting(true);
      XMLSerializer serializer = new XMLSerializer(outputStream, format);
      outputHandler = serializer.asContentHandler();
      outputHandler.startDocument();
    }
    catch (IOException ex)
    {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }    
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
  {    
    if (name.equals("context"))
    {
      topicId = null;
    }
    else if (name.equals("topic") && (topicId == null))
    {
      topicId = attributes.getValue(uri, "id");
      String[] sourceAnchors = fContextIDs.get(topicId);
      if (sourceAnchors != null)
      {
        // copy attributes 
        AttributesImpl newAttributes = new AttributesImpl();
        // add normal ID attribute
        newAttributes.addAttribute(uri, "id", "", "CDATA", topicId);
        // add additional attributes for the linking
        newAttributes.addAttribute(uri, "href", "", "CDATA", sourceAnchors[0] + "#" + topicId);
        newAttributes.addAttribute(uri, "label", "", "CDATA", sourceAnchors[1]);
        // copy back to original set
        attributes = newAttributes;
      }
    }
    
    // write the element in the output
    outputHandler.startElement(uri, localName, name, attributes);
  }
  
  /**
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  @Override
  public void characters(char[] ch, int start, int length) throws SAXException
  {    
    outputHandler.characters(ch, start, length);
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void endElement(String uri, String localName, String name) throws SAXException
  {
    outputHandler.endElement(uri, localName, name); 
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endDocument()
   */
  @Override
  public void endDocument() throws SAXException
  {    
    outputHandler.endDocument();
        
    try
    {
      outputStream.flush();
      outputStream.close();
    }
    catch (IOException ex)
    {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }  
  }

  /**
   * Callback to return the generated new context file 
   * @return the adapted context xml file
   */
  public File getOutputFile()
  {
    return output;
  }
}
