package me.jumper251.replay.replaysystem.data;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public enum CompressType {
    Gzip, ZSTD, LZ4, UNKNOWN;

    public static CompressType fromInt(final int i) {
        return switch (i) {
            case 1 -> ZSTD;
            case 2 -> LZ4;
            default -> Gzip;
        };
    }

    public static CompressDetectionResult detectCompression(@NotNull InputStream inputStream) throws IOException {
        Validate.notNull(inputStream, "InputStream must be a valid value.");

        PushbackInputStream pbIn = new PushbackInputStream(inputStream, 8);

        byte[] buf = new byte[8];
        int bytesRead = pbIn.read(buf, 0, buf.length);

        if (bytesRead > 0) {
            pbIn.unread(buf, 0, bytesRead);
        }

        CompressType type = doDetectByMagic(buf, bytesRead);
        return new CompressDetectionResult(type, pbIn);
    }

    private static CompressType doDetectByMagic(byte[] magicBytes, int bytesRead) {
        if (bytesRead < 2) {
            return UNKNOWN;
        }

        int gzipCheck = ((magicBytes[0] & 0xFF) << 8) | (magicBytes[1] & 0xFF);
        if (gzipCheck == 0x1F8B) {
            return Gzip;
        }

        if (bytesRead >= 4) {
            int magicAll = ((magicBytes[0] & 0xFF) << 24)
                    | ((magicBytes[1] & 0xFF) << 16)
                    | ((magicBytes[2] & 0xFF) << 8)
                    | (magicBytes[3] & 0xFF);

            if (magicAll == 0x28B52FFD) {
                return ZSTD;
            }
            if (magicAll == 0x04224D18 || magicAll == 0x184D2204) {
                return LZ4;
            }
        }

        return UNKNOWN;
    }


    public record CompressDetectionResult(CompressType compressType, InputStream inputStream) {
    }


    public int toInt() {
        return switch (this) {
            case ZSTD -> 1;
            case LZ4 -> 2;
            default -> 0;
        };
    }
}

