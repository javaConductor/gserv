package io.github.javaconductor.gserv.utils

/**
 * Created by lcollins on 7/27/2015.
 */
class ByteUtils {

    List<byte[]> splitBytes(byte[] buffer, String boundary) {
        def parts = []
        def start = 0
        for (int currentPosition = 0; currentPosition != (buffer.length - boundary.bytes.length); ++currentPosition) {
            if (compareBytes(buffer, currentPosition, boundary.bytes)) {
                if (currentPosition > 0)
                    parts.add([
                            start: start,
                            end  : buffer[currentPosition - 1] == '\n' ? (currentPosition - 2) : (currentPosition - 1)
                    ]);
                start = currentPosition;
            }
            if (compareBytes(buffer, currentPosition, (boundary + '--').bytes)) {
                break
            }
        }
        parts.collect { part ->
            Arrays.copyOfRange(buffer, part.start, part.end + 1)
        }
    }

    boolean compareBytes(byte[] bytes, int offset, byte[] boundaryBytes) {
        boolean ok = (bytes[offset] == boundaryBytes[0] && (bytes[offset + boundaryBytes.length - 1 as int] == boundaryBytes[boundaryBytes.length - 1 as int]))
        if (!ok) return false
        Arrays.equals(bytes[offset..(offset + boundaryBytes.length - 1)] as byte[], boundaryBytes);
    }
}
