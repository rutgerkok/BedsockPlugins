package nl.rutgerkok.bedsockplugin.simplebackup.uploader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.rutgerkok.bedsock.ServerFolders;

public abstract class Uploader {

    /**
     * Creates the appropriate uploader class for the given settings.
     *
     * @param settings
     *            The settings.
     * @return The uploader.
     * @throws IllegalArgumentException
     *             If an unsupported protocol is used.
     */
    public static Uploader fromSettings(UploadSettings settings) {
        switch (settings.protocol) {
            case "sftp":
                return new SftpUploader(settings);
            case "ftp":
                return new FtpUploader(settings);
            default:
                throw new IllegalArgumentException("Unknown protocol: " + settings.protocol);
        }
    }

    private static void transfer(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
    }

    protected final UploadSettings uploadSettings;

    public Uploader(UploadSettings uploadSettings) {
        this.uploadSettings = Objects.requireNonNull(uploadSettings);
    }

    /**
     * Backs up the given list of files to the external server.
     * 
     * @param folders
     *            The server folder structure.
     * @param files
     *            The world files to back up.
     * @throws IOException
     *             If an IO error occurs.
     */
    public abstract void backupFiles(ServerFolders folders, List<SendingFile> files) throws IOException;

    /**
     * Creates a ZIP file of the given file list.
     *
     * @param folder
     *            The root folder where the files to be zipped are currently in.
     * @param files
     *            The files to be zipped.
     * @param outputStream
     *            The ZIP file to write to.
     * @throws IOException
     *             If an IO error occurs.
     */
    protected void createZip(Path folder, List<SendingFile> files, OutputStream outputStream)
            throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
        for (SendingFile file : files) {
            zipOutputStream.putNextEntry(new ZipEntry(file.path));

            Path filePath = folder.resolve(file.path);
            try (InputStream stream = Files.newInputStream(filePath)) {
                transfer(stream, zipOutputStream);
            }
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
    }
}
