package nl.rutgerkok.bedsockplugin.simplebackup.uploader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import nl.rutgerkok.bedsock.ServerFolders;

final class SftpUploader extends Uploader {

    public SftpUploader(UploadSettings uploadSettings) {
        super(uploadSettings);
    }

    @Override
    public void backupFiles(ServerFolders folders, List<SendingFile> files) throws IOException {
        Session session = null;
        Channel channel = null;
        try {
            JSch ssh = new JSch();
            session = ssh.getSession(uploadSettings.user, uploadSettings.host, uploadSettings.port);
            session.setPassword(uploadSettings.pass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            sftp.cd(uploadSettings.folder);
            try (OutputStream uploadStream = sftp.put(uploadSettings.file)) {
                createZip(folders.getWorldsFolder(), files, uploadStream);
            }
        } catch (JSchException | SftpException e) {
            throw new IOException("Backup failed", e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
