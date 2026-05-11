package cc.fascinated.fascinatedutils.systems.screenshot;

import cc.fascinated.fascinatedutils.systems.TextureManager;
import cc.fascinated.fascinatedutils.client.Client;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Iterator;

@Getter
public class Screenshot {
    private final String name;
    private final Path path;
    private final Date createdAt;
    private final long size;
    private long width;
    private long height;

    @SneakyThrows
    public Screenshot(Path path) {
        this.name = path.getFileName().toString();
        this.path = path;

        BasicFileAttributes attrs = Files.readAttributes(this.path, BasicFileAttributes.class);
        FileTime createdAt = attrs.creationTime();
        this.createdAt = new Date(createdAt.toMillis());

        this.size = path.toFile().length();

        try (ImageInputStream in = ImageIO.createImageInputStream(path.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(in);
                this.width = reader.getWidth(0);
                this.height = reader.getHeight(0);
                reader.dispose();
            }
        } catch (IOException e) {
            Client.LOG.error("Failed to load screenshot {}", path, e);
        }
    }

    /**
     * Gets the texture for this screenshot.
     *
     * @return the texture identifier
     */
    public TextureManager.LoadedTexture getTexture(int maxHeight) {
        return TextureManager.INSTANCE.getLocal(path, maxHeight, () -> {});
    }

    /**
     * Copies this screenshot image to the system clipboard.
     */
    public void copyToClipboard() {
        try {
            BufferedImage img = ImageIO.read(path.toFile());
            if (img == null) return;
            Transferable transferable = new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{DataFlavor.imageFlavor};
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return DataFlavor.imageFlavor.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                    if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                    return img;
                }
            };
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
