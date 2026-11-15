package cc.fascinated.fascinatedutils.systems.screenshot;

import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.systems.TextureManager;
import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.client.Client;
import net.minecraft.network.chat.Component;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

@Getter
public class Screenshot {
    private final String name;
    private final Path path;
    private final Date createdAt;
    private final long size;

    @SneakyThrows
    public Screenshot(Path path) {
        this.name = path.getFileName().toString();
        this.path = path;

        BasicFileAttributes attrs = Files.readAttributes(this.path, BasicFileAttributes.class);
        FileTime createdAt = attrs.creationTime();
        this.createdAt = new Date(createdAt.toMillis());

        this.size = path.toFile().length();
    }

    /**
     * Gets the texture for this screenshot.
     *
     * @return the loaded texture, or {@code null} while it is still loading
     */
    public TextureManager.LoadedTexture getTexture(int maxHeight) {
        return TextureManager.INSTANCE.getLocal(path, maxHeight);
    }

    /**
     * Copies this screenshot image to the system clipboard.
     */
    public void copyToClipboard() {
        Constants.EXECUTORS.execute(() -> {
            try {
                BufferedImage img = ImageIO.read(path.toFile());
                if (img == null) {
                    Client.LOG.warn("Failed to decode screenshot {} for clipboard copy", path);
                    return;
                }
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
                    public @NonNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                        return img;
                    }
                };
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
                Toast.show().message(Component.translatable("alumite.screenshot.copied").getString()).info();
            } catch (Exception exception) {
                Client.LOG.warn("Failed to copy screenshot {} to clipboard", path, exception);
            }
        });
    }
}
