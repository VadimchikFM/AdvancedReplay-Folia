package me.jumper251.replay.replaysystem.data;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public enum CompressType {
    Gzip, ZSTD, LZ4, UNKNOWN;

    public static CompressType fromInt(final int i) {
        return switch (i) {
            case 1 -> ZSTD;
            case 2 -> LZ4;
            default -> Gzip;
        };
    }

    public static CompressType detectCompression(@NotNull InputStream inputStream) throws IOException {
        Validate.notNull(inputStream, "InputStream must be a valid value.");
        inputStream.mark(4);

        byte[] magicBytes = new byte[4];
        int bytesRead = inputStream.read(magicBytes);
        inputStream.reset();

        if (bytesRead < 2) {
            return CompressType.UNKNOWN;
        }

        int magic = ((magicBytes[0] & 0xFF) << 8) | (magicBytes[1] & 0xFF);

        if (magic == 0x1F8B) {
            return CompressType.Gzip;
        }

        if (bytesRead >= 4) {
            int zstdMagic = ((magicBytes[0] & 0xFF) << 24) | ((magicBytes[1] & 0xFF) << 16) |
                    ((magicBytes[2] & 0xFF) << 8) | (magicBytes[3] & 0xFF);
            if (zstdMagic == 0x28B52FFD) {
                return CompressType.ZSTD;
            }
        }

        if (bytesRead >= 4) {
            int lz4Magic = ((magicBytes[0] & 0xFF) << 24) | ((magicBytes[1] & 0xFF) << 16) |
                    ((magicBytes[2] & 0xFF) << 8) | (magicBytes[3] & 0xFF);
            if (lz4Magic == 0x04224D18 || lz4Magic == 0x184D2204) {
                return CompressType.LZ4;
            }
        }

        return CompressType.UNKNOWN;
    }

    public int toInt() {
        return switch (this) {
            case ZSTD -> 1;
            case LZ4 -> 2;
            default -> 0;
        };
    }
}

