package org.example.web.security;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

public final class ImageUploadValidator {
    private ImageUploadValidator() {}
    public record Image(String extension, byte[] bytes) {}

    public static Image validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空");
        if (file.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("图片不能超过 5 MB");
        try (var input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) throw new IllegalArgumentException("无法读取图片");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("仅支持有效的 PNG / JPEG 图片");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!java.util.Set.of("png", "jpeg", "jpg").contains(format))
                    throw new IllegalArgumentException("仅支持 PNG / JPEG 图片");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000L)
                    throw new IllegalArgumentException("图片像素过大");
                BufferedImage decoded = reader.read(0);
                var output = new ByteArrayOutputStream();
                String normalized = "png".equals(format) ? "png" : "jpg";
                if (!ImageIO.write(decoded, normalized, output)) throw new IllegalArgumentException("无法处理图片");
                return new Image("." + normalized, output.toByteArray());
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("图片内容损坏或格式不支持"); }
    }
}
