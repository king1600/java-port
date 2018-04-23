package com.kprotty.port.net;

public final class AsioBuffer {
    private byte[] buffer;
    private int readPosition;
    private int writePosition;
    private int livingSegments;

    public static final class Segment implements AutoCloseable {
        public final int offset;
        public final int length;
        private final AsioBuffer buf;

        public Segment(final AsioBuffer buffer, final int offset, final int length) {
            this.buf = buffer;
            this.offset = offset;
            this.length = length;
        }

        public final byte[] getBytes() {
            return buf.buffer;
        }

        @Override
        public void close() {
            buf.livingSegments -= 1;
        }
    }

    public AsioBuffer() {
        readPosition = writePosition = livingSegments = 0;
    }

    public final int size() {
        return writePosition - readPosition;
    }

    @Override
    public String toString() {
        return new String(buffer, readPosition, size());
    }

    public final Segment read(int amount) {
        amount = Math.min(amount, size());
        if (buffer == null || amount == 0)
            return null;

        final Segment segment = new Segment(this, readPosition, amount);
        readPosition += amount;
        return segment;
    }

    public void write(final byte[] data, final int length) {
        write(data, 0, length);
    }

    public void write(final byte[] data, final int offset, final int length) {
        if (buffer == null)
            buffer = new byte[Math.max(length, 8)];

        if (readPosition != 0 && livingSegments == 0) {
            System.arraycopy(buffer, readPosition, buffer, 0, size());
            writePosition -= readPosition;
            readPosition = 0;
        }

        if (writePosition + length > buffer.length) {
            final byte[] resized = new byte[(writePosition + length) * 14 / 10];
            System.arraycopy(buffer, readPosition, resized, 0, size());
            buffer = resized;
        }

        System.arraycopy(data, offset, buffer, writePosition, length);
        writePosition += length;
    }
}
