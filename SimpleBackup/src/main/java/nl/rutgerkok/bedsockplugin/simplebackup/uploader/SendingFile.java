package nl.rutgerkok.bedsockplugin.simplebackup.uploader;

import java.util.Objects;

/**
 * A file that needs to be uploaded.
 *
 */
public final class SendingFile {

    public final String path;
    public final long size;

    public SendingFile(String path, long size) {
        this.path = Objects.requireNonNull(path, "path");
        this.size = size;
    }

    @Override
    public String toString() {
        return "SendingFile [path=" + path + ", size=" + size + "]";
    }
}
