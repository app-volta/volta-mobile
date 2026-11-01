package com.aula.volta.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class ImageUtils {

    private static final String TAG = "ImageUtils";

    private ImageUtils() {

    }

    public static int getExifOrientation(@Nullable String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return ExifInterface.ORIENTATION_NORMAL;
        }
        try {
            ExifInterface exif = new ExifInterface(filePath);
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Throwable t) {
            Log.w(TAG, "Falha ao ler EXIF de " + filePath + ": " + t.getMessage());
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    public static int getExifRotationDegrees(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return 270;
            default:
                return 0;
        }
    }

    @Nullable
    public static Matrix getExifTransformationMatrix(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                return matrix;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                return matrix;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                return matrix;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                return matrix;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                return matrix;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270);
                matrix.postScale(-1, 1);
                return matrix;
            default:
                return null;
        }
    }

    @Nullable
    public static Bitmap applyExifOrientation(@Nullable Bitmap bitmap, int orientation) {
        if (bitmap == null) {
            return null;
        }
        Matrix matrix = getExifTransformationMatrix(orientation);
        if (matrix == null) {
            return bitmap;
        }
        try {
            Bitmap transformed = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (transformed != bitmap) {
                bitmap.recycle();
            }
            return transformed;
        } catch (Throwable t) {
            Log.w(TAG, "Falha ao aplicar matriz de orientação: " + t.getMessage());
            return bitmap;
        }
    }

    @Nullable
    public static Bitmap decodeFileWithExifRotation(@Nullable String filePath, int maxDimen) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        try {
            int orientation = getExifOrientation(filePath);

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            int sample = 1;
            if (maxDimen > 0) {
                while (bounds.outWidth / sample > maxDimen || bounds.outHeight / sample > maxDimen) {
                    sample *= 2;
                }
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeFile(filePath, opts);
            if (bitmap == null) {
                return null;
            }

            return applyExifOrientation(bitmap, orientation);
        } catch (Throwable t) {
            Log.e(TAG, "Erro ao decodificar imagem com EXIF: " + filePath, t);
            return null;
        }
    }

    @Nullable
    public static File fixOrientationAndCompress(@NonNull Context context,
                                                @Nullable File source,
                                                int maxDimen,
                                                int quality) {
        if (source == null || !source.exists() || source.length() == 0) {
            return null;
        }

        try {
            int orientation = getExifOrientation(source.getAbsolutePath());

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            int sample = 1;
            if (maxDimen > 0) {
                while (bounds.outWidth / sample > maxDimen || bounds.outHeight / sample > maxDimen) {
                    sample *= 2;
                }
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), opts);
            if (bitmap == null) {
                return null;
            }

            Bitmap uprightBitmap = applyExifOrientation(bitmap, orientation);
            if (uprightBitmap == null) {
                return null;
            }

            File out = new File(context.getFilesDir(),
                    "volta_photo_" + System.currentTimeMillis() + ".jpg");

            try (FileOutputStream fos = new FileOutputStream(out)) {
                uprightBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                fos.flush();
            }

            uprightBitmap.recycle();

            try {
                ExifInterface destExif = new ExifInterface(out.getAbsolutePath());
                destExif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                destExif.saveAttributes();
            } catch (Throwable ignored) {
            }

            if (!source.getAbsolutePath().equals(out.getAbsolutePath())) {
                String sourcePath = source.getAbsolutePath();
                if (sourcePath.contains("cache") || source.getName().startsWith("volta_")) {
                    try {
                        source.delete();
                    } catch (Throwable ignored) {
                    }
                }
            }

            return out;
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao corrigir orientação e comprimir imagem: " + source.getAbsolutePath(), t);
            return null;
        }
    }

    @Nullable
    public static File copyUriToCache(@NonNull Context context, @NonNull Uri uri) {
        try {
            File out = new File(context.getCacheDir(),
                    "volta_gallery_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(out)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, n);
                }
                fos.flush();
            }
            return out;
        } catch (Throwable t) {
            Log.e(TAG, "Falha ao copiar Uri para cache: " + uri, t);
            return null;
        }
    }
}
