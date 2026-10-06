package org.example.web.security;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class ImageUploadValidatorTest {
    @Test void rejectsTextDisguisedAsPng() {
        assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(
            new MockMultipartFile("file", "a.png", "image/png", "harmless text".getBytes())));
    }
    @Test void rejectsEmptyAndOversizedFiles() {
        assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(new MockMultipartFile("file",new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(new MockMultipartFile("file",new byte[5*1024*1024+1])));
    }
    @Test void normalizesExtensionAndStripsTrailingNonImageContent() throws Exception {
        var output=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",output);
        byte[] clean=output.toByteArray();
        output.write("<harmless-trailing-marker>".getBytes());
        var image=ImageUploadValidator.validate(new MockMultipartFile("file","../../test.html","text/html",output.toByteArray()));
        assertEquals(".png",image.extension());
        assertArrayEquals(clean,image.bytes());
    }
    @Test void rejectsTruncatedPng() {
        assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(
            new MockMultipartFile("file","bad.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10})));
    }
}
