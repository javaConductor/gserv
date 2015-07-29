package io.github.javaconductor.gserv.utils

import groovy.util.logging.Slf4j

/**
 * Created by lcollins on 7/27/2015.
 */
@Slf4j
class ByteUtils {

    List<byte[]> splitBytes(byte[] buffer, String boundary) {
        def parts = []
        def start = 0
        log.trace("Buffer as text ${new String(buffer)}")
        for (int currentPosition = 0; currentPosition != (buffer.length - boundary.bytes.length); ++currentPosition) {
            if (compareBytes(buffer, currentPosition, boundary.bytes)) {
                if (currentPosition > 0) {
                    def end = currentPosition - 1
                    // check for \r\n
                    if (buffer[currentPosition - 1] == '\n' && buffer[currentPosition - 2] == '\r') {
                        end = currentPosition - 3
                    }
                    // check for  just \n
                    else if (buffer[currentPosition - 1] == '\n') {
                        end = currentPosition - 2
                    }
                    parts.add([
                            start: start,
                            end: end
                    ]);
                }
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
