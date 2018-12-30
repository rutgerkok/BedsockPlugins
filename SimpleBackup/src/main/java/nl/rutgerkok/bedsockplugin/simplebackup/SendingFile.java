package nl.rutgerkok.bedsockplugin.simplebackup;

import java.util.Objects;

/**
 * A file that needs to be uploaded.
 *
 */
final class SendingFile {

    final String path;

    final long size;
    public SendingFile(String path, long size) {
        this.path = Objects.requireNonNull(path, "path");
        this.size = size;
    }

    @Override
    public String toString() {
        return "SendingFile [path=" + path + ", size=" + size + "]";
    }
}
