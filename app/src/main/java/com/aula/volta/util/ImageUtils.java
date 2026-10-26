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

/**
 * Utilitário central para processamento, correção de rotação EXIF e compressão de imagens no VOLTA.
 * Garante que fotos tiradas pela câmera em modo retrato (portrait) permaneçam verticais (em pé)
 * tanto no preview, quanto na análise por IA, envio ao Cloudinary e exibição nos cards.
 */
public final class ImageUtils {

    private static final String TAG = "ImageUtils";

    private ImageUtils() {
        // Classe utilitária
    }

    /**
     * Obtém a orientação EXIF do arquivo de imagem.
     */
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

    /**
     * Converte o atributo de orientação EXIF em graus de rotação (0, 90, 180 ou 270).
     */
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

    /**
     * Constrói a Matrix de transformação correspondente à orientação EXIF informada.
     * Retorna null se nenhuma rotação ou espelhamento for necessário.
     */
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

    /**
     * Aplica a orientação EXIF ao Bitmap, girando/espelhando os pixels caso necessário.
     * Recicla o bitmap original caso um novo bitmap rotacionado seja criado.
     */
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

    /**
     * Decodifica um arquivo de imagem aplicando o downsampling (inSampleSize) e a rotação EXIF,
     * garantindo que a imagem seja retornada sempre com a orientação correta (upright).
     */
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

    /**
     * Processa a foto capturada (seja pelo CameraX, câmera nativa do sistema ou galeria):
     * 1. Lê a orientação EXIF original do arquivo fonte.
     * 2. Decodifica com inSampleSize adequado para não estourar memória (maxDimen).
     * 3. Gira fisicamente os pixels para que fiquem verticais / na orientação correta.
     * 4. Salva o JPEG resultante no diretório de arquivos permanente do app (filesDir).
     * 5. Grava ORIENTATION_NORMAL no cabeçalho EXIF do arquivo de destino.
     * 6. Deleta arquivos intermediários/temporários do cache para economizar armazenamento.
     *
     * @param context Contexto da aplicação.
     * @param source Arquivo de origem (ex: cache de captura ou cópia da galeria).
     * @param maxDimen Dimensão máxima desejada (largura ou altura).
     * @param quality Qualidade do JPEG (0 a 100).
     * @return Novo arquivo permanente na orientação correta ou null se falhar.
     */
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

            // Grava EXIF ORIENTATION_NORMAL no arquivo final para clientes que leem tags EXIF
            try {
                ExifInterface destExif = new ExifInterface(out.getAbsolutePath());
                destExif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                destExif.saveAttributes();
            } catch (Throwable ignored) {
            }

            // Remove o arquivo temporário de origem se não for o mesmo e estiver no cache
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

    /**
     * Copia um conteúdo de Uri (galeria) para arquivo temporário no cache.
     */
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
