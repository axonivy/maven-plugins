package ch.ivyteam.ivy.maven;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Displays a BufferedImage.
 */
public class ImageViewer extends JPanel {

  private static final long serialVersionUID = 1L;

  public static void show(File imageFile) {
    try {
      BufferedImage image = ImageIO.read(imageFile);
      if (image == null) {
        throw new RuntimeException("Failed to read image '" + imageFile + "'.");
      }
      show(image);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static void show(BufferedImage image) {
    JFrame f = new JFrame();
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    f.add(new ImageViewer(image));
    f.pack();
    f.setVisible(true);
  }

  private final BufferedImage image;

  private ImageViewer(BufferedImage image) {
    this.image = image;
    setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(image, 0, 0, null);
  }
}
