package ch.ivyteam.maven.public_api_source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class PublicApiClassesFinder
{
  static final String PUBLIC_API_CLASS_DESC = "Lch/ivyteam/api/PublicAPI;";
  private File classesDir;

  public PublicApiClassesFinder(File classesDir)
  {
    this.classesDir = classesDir;
  }

  public List<File> find()
  {
    List<File> foundClasses = new ArrayList<>();
    String[] endings = {"class"};
    Collection<File> allClasses = FileUtils.listFiles(classesDir, endings, true);
    for (File classToCheck : allClasses)
    {
      if (isPublicApi(classToCheck))
      {
        foundClasses.add(classToCheck);
      }
    }
    return foundClasses;
  }

  private boolean isPublicApi(File classToCheck)
  {
    try(InputStream inputStream = classToCheck.toURI().toURL().openStream();)
    {
      PublicApiClassVisitor classVisitor = new PublicApiClassVisitor();
      new ClassReader(inputStream).accept(classVisitor, 0);
      return classVisitor.publicApiAnnotationFound;
    }
    catch (MalformedURLException ex)
    {
      ex.printStackTrace();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    return false;
  }
  
  private static class PublicApiClassVisitor extends ClassVisitor
  {
    boolean publicApiAnnotationFound = false;
    
    public PublicApiClassVisitor()
    {
      super(Opcodes.ASM5);
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible)
    {
      if (desc.equals(PUBLIC_API_CLASS_DESC))
      {
        publicApiAnnotationFound = true;
      }
      return null;
    }
  }

}
