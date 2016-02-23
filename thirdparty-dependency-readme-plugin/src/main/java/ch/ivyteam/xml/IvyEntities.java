package ch.ivyteam.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * This class knows a few entities and can be used in sax parser to access them.
 * <b>The problem particularly with our own DTDs is that they reside on the file
 * system and that they are refered via relative references.</b><br>
 * The default behaviour of an xml parser is to resolve either the URL or try
 * to find that local file (in which case they have to be in the "right" place).
 * If we give the parser an own EntityResolver we can load the DTDs from the
 * resource pool, no matter where the xml is located.
 *
 * @version po 10.10.2000
 * @author Marco Poli
 */
public class IvyEntities implements EntityResolver
{
  /** Maps a public id to a local resource name */
  private static HashMap<String,String> publicId2Ressource = new HashMap<String,String>();

  static
  {
    // Public DTDs for JavaHelp
    publicId2Ressource.put(
        "-//Sun Microsystems Inc.//DTD JavaHelp TOC Version 1.0//EN",
        "/ivyteam/dtd/javahelp/toc_1_0.dtd");
    publicId2Ressource.put(
        "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 1.0//EN",
        "/ivyteam/dtd/javahelp/helpset_1_0.dtd");
    publicId2Ressource.put(
        "-//Sun Microsystems Inc.//DTD JavaHelp Map Version 1.0//EN",
        "/ivyteam/dtd/javahelp/map_1_0.dtd");
    publicId2Ressource.put(
        "-//Sun Microsystems Inc.//DTD JavaHelp Index Version 1.0//EN",
        "/ivyteam/dtd/javahelp/index_1_0.dtd");

    // Public DTDs for XHTML
    publicId2Ressource.put(
        "-//W3C//DTD XHTML 1.0 Strict//EN",
        "/ivyteam/dtd/xhtml/xhtml1-strict.dtd");
    publicId2Ressource.put(
        "-//W3C//DTD XHTML 1.0 Transitional//EN",
        "/ivyteam/dtd/xhtml/xhtml1-transitional.dtd");
    publicId2Ressource.put(
        "-//W3C//DTD XHTML 1.0 Frameset//EN",
        "/ivyteam/dtd/xhtml/xhtml1-frameset.dtd");
    publicId2Ressource.put(
        "-//W3C//ENTITIES Latin 1 for XHTML//EN",
        "/ivyteam/dtd/xhtml/xhtml-lat1.ent");
    publicId2Ressource.put(
        "-//W3C//ENTITIES Symbols for XHTML//EN",
        "/ivyteam/dtd/xhtml/xhtml-symbol.ent");
    publicId2Ressource.put(
        "-//W3C//ENTITIES Special for XHTML//EN",
        "/ivyteam/dtd/xhtml/xhtml-special.ent");

    // Public DTDs for ivyTeam
    publicId2Ressource.put(
        "-//ivyTeam//DTD ivyGrid Runtime Statistic//EN",
        "/ivyteam/dtd/IvyGrid_Runtime_Statistic.dtd");
  }
  
  
  /**
   * Resolves an entity
   * @return either an input source of the entity or null. Null means that we
   * want the parser to use its default behaviour.
   */
  @Override
  public InputSource resolveEntity (String publicId, String systemId)
  {
    //if there's a public id (for a DTD for example), try this one...
    if (publicId != null)
    {
      //look up in hashtable
      String res = publicId2Ressource.get(publicId);
      if (res != null)
      {
        InputStream is = getClass().getResourceAsStream(res);
        if (is != null)
        {
          return new InputSource(is);
        }
      }
    }

    //find the entity (for example a dtd) by system id
    else if(systemId != null)
    {
      try
      {
        URL url = new URL(systemId);
        URLConnection connection = url.openConnection();
        InputStream stream = connection.getInputStream();
        if (stream != null)
        {
          return new InputSource(stream);
        }
      }
      catch (MalformedURLException ex)
      {
        throw new RuntimeException("Could not load DTD with systemId '"+systemId+"'", ex);
      }
      catch (IOException ex)
      {
        throw new RuntimeException("Could not load DTD with systemId '"+systemId+"'", ex);
      }
    }
    
    throw new RuntimeException("No entity was resolved for publicId '"+publicId+"' and systemId '"+systemId+"'");
  }
  
}
