package nl.rutgerkok.bedsockplugin.simplebackup.uploader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;

import nl.rutgerkok.bedsock.ServerFolders;

final class FtpUploader extends Uploader {

    public FtpUploader(UploadSettings uploadSettings) {
        super(uploadSettings);
    }

    @Override
    public void backupFiles(ServerFolders folders, List<SendingFile> files) throws IOException {
        Path tempZipFile = folders.getRootFolder().resolve("backup.zip");
        try (OutputStream outputStream = Files.newOutputStream(tempZipFile)) {
            createZip(folders.getWorldsFolder(), files, outputStream);
        }

        FTPClient ftp = new FTPClient();
        try {
            ftp.setRemoteHost(uploadSettings.host);
            ftp.setRemotePort(uploadSettings.port);
            ftp.connect();
            ftp.login(uploadSettings.user, uploadSettings.pass);
            ftp.chdir(uploadSettings.folder);
            ftp.setType(FTPTransferType.BINARY);
            ftp.put(tempZipFile.toString(), uploadSettings.file);
        } catch (FTPException e) {
            throw new IOException(e);
        } finally {
            try {
                ftp.quit();
            } catch (FTPException e) {
                throw new IOException(e);
            }
            Files.delete(tempZipFile);
        }
    }

}
