package com.aula.volta.util;

import android.media.ExifInterface;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Testes unitários para o utilitário de rotação e processamento de imagem ImageUtils.
 * Valida conversão de rotação EXIF e comportamento seguro com arquivos nulos/inexistentes.
 */
public class ImageUtilsTest {

    @Test
    public void testGetExifRotationDegrees() {
        assertEquals(0, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_NORMAL));
        assertEquals(0, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_UNDEFINED));
        assertEquals(90, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_ROTATE_90));
        assertEquals(180, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_ROTATE_180));
        assertEquals(270, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_ROTATE_270));
        assertEquals(90, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_TRANSPOSE));
        assertEquals(270, ImageUtils.getExifRotationDegrees(ExifInterface.ORIENTATION_TRANSVERSE));
    }

    @Test
    public void testGetExifOrientation_handlesNullAndEmpty() {
        assertEquals(ExifInterface.ORIENTATION_NORMAL, ImageUtils.getExifOrientation(null));
        assertEquals(ExifInterface.ORIENTATION_NORMAL, ImageUtils.getExifOrientation(""));
        assertEquals(ExifInterface.ORIENTATION_NORMAL, ImageUtils.getExifOrientation("   "));
        assertEquals(ExifInterface.ORIENTATION_NORMAL, ImageUtils.getExifOrientation("/non/existent/file.jpg"));
    }

    @Test
    public void testDecodeFileWithExifRotation_handlesInvalidPathsSafely() {
        assertNull(ImageUtils.decodeFileWithExifRotation(null, 1280));
        assertNull(ImageUtils.decodeFileWithExifRotation("", 1280));
        assertNull(ImageUtils.decodeFileWithExifRotation("invalid_path_that_does_not_exist.jpg", 1280));
    }

    @Test
    public void testFixOrientationAndCompress_handlesNullOrNonExistentFile() {
        assertNull(ImageUtils.fixOrientationAndCompress(null, null, 1280, 80));
        File nonExistent = new File("non_existent_raw_photo.jpg");
        assertNull(ImageUtils.fixOrientationAndCompress(null, nonExistent, 1280, 80));
    }
}
