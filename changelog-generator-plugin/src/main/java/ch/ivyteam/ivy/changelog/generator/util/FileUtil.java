package ch.ivyteam.ivy.changelog.generator.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileUtil
{
  public static List<Path> getAllFiles(String dir)
  {
    List<Path> files = new ArrayList<>();

    Path p = Paths.get(dir);
    FileVisitor<Path> fv = new SimpleFileVisitor<Path>()
      {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
        {
          files.add(file);
          return FileVisitResult.CONTINUE;
        }
      };

    try
    {
      Files.walkFileTree(p, fv);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
    return files;
  }
}
