package cchesser.javaperf.workshop.data;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This is a trival example of doing something unnecessary in the service and using up some heap.
 * Example was obtained from StackOverflow: http://stackoverflow.com/questions/7098972/ascii-art-java
 */
class AsciiArtConverter {

    static String convert(String text) {

        BufferedImage image = new BufferedImage(144, 32, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setFont(new Font("Dialog", Font.PLAIN, 14));
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString(text, 6, 24);

        StringBuilder rendered = new StringBuilder();
        try {
            Path tempFilePath = Files.createTempFile("text", "png");
            File tempFile = tempFilePath.toFile();
            tempFile.deleteOnExit();

            ImageIO.write(image, "png", tempFile);

            for (int y = 0; y < 32; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < 144; x++)
                    sb.append(image.getRGB(x, y) == -16777216 ? " " : image.getRGB(x, y) == -1 ? "#" : "*");
                if (sb.toString().trim().isEmpty()) continue;
                rendered.append(sb);
                rendered.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return rendered.toString();

    }

}
