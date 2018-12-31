package nl.rutgerkok.bedsockplugin.simplebackup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;

import nl.rutgerkok.bedsock.ActiveServer;
import nl.rutgerkok.bedsock.InactiveServer;
import nl.rutgerkok.bedsock.Scheduler;
import nl.rutgerkok.bedsock.command.CommandArgs;
import nl.rutgerkok.bedsock.command.CommandException;
import nl.rutgerkok.bedsock.command.CommandSender;
import nl.rutgerkok.bedsock.command.WrapperCommand;
import nl.rutgerkok.bedsock.config.ConfigObject;
import nl.rutgerkok.bedsock.config.JsonConfigs;
import nl.rutgerkok.bedsock.plugin.ActivePlugin;
import nl.rutgerkok.bedsock.plugin.Plugin;

public class Main implements Plugin {

    private class BackupCommand extends WrapperCommand {

        protected BackupCommand(ActivePlugin plugin) {
            super(plugin);
        }

        @Override
        public String getDescription() {
            return "Back-ups the world";
        }

        @Override
        public String getUsage() {
            return "";
        }

        @Override
        public void run(CommandSender sender, CommandArgs args) throws CommandException {
            ActiveServer server = args.getServer();
            server.getBedrockCommandRunner().runAndRecordCommand(server.parseCommand("save hold"));

            CommandArgs query = server.parseCommand("save query");
            boolean success = false;
            for (int i = 0; i < 10; i++) {
                String result = server.getBedrockCommandRunner().runAndRecordCommand(query);
                String[] lines = result.split("\n");
                if (lines[0].contains("Files are now ready to be copied")) {
                    // Found a file list
                    success = true;
                    String[] filesWithSize = lines[1].split(", ");
                    List<SendingFile> files = new ArrayList<>(filesWithSize.length);
                    for (String fileWithSize : filesWithSize) {
                        String[] parts = fileWithSize.split(":");
                        files.add(new SendingFile(parts[0], Long.parseLong(parts[1])));
                    }

                    // Process all files in the future.
                    Scheduler scheduler = server.getScheduler();
                    CompletableFuture
                            .runAsync(() -> backupFiles(server, files), scheduler.workerThreadExecutor(getPlugin()))
                            .handleAsync((Void r, Throwable exception) -> {
                                if (exception != null) {
                                    getLogger().error("Backup failed", exception.getCause());
                                } else {
                                    getLogger().info("Backup complete!");
                                }

                                try {
                                    server.getBedrockCommandRunner()
                                            .runAndRecordCommand(server.parseCommand("save resume"));
                                } catch (CommandException e) {
                                    getLogger().error("Error resuming saving", e);
                                }
                                return r;
                            }, scheduler.mainThreadExecutor(getPlugin())).exceptionally(exception -> {
                                getLogger().error("Backup failed", exception.getCause());
                                return null;
                            });
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }

            if (!success) {
                throw new CommandException("Failed to backup files - could not parse command output");
            }
        }

    }

    private static void transfer(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
    }

    private LoginInfo loginInfo = new LoginInfo();

    private void backupFiles(ActiveServer server, List<SendingFile> files) {
        server.getServerLogger().info("Test! Backing up " + files);

        try {
            Path zipFile = server.getFolders().getRootFolder().resolve("backup.zip");
            createZip(server, files, zipFile);

            FTPClient ftp = new FTPClient();
            try {
                ftp.setRemoteHost(loginInfo.host);
                ftp.connect();
                ftp.login(loginInfo.user, loginInfo.pass);
                ftp.chdir(loginInfo.folder);
                ftp.put(zipFile.toString(), loginInfo.file);
            } finally {
                ftp.quit();
                Files.delete(zipFile);
            }
        } catch (IOException | FTPException e) {
            throw new RuntimeException("Backup failed", e);
        }
    }

    private void createZip(ActiveServer server, List<SendingFile> files, Path zipFile) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(zipFile)) {
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
            for (SendingFile file : files) {
                zipOutputStream.putNextEntry(new ZipEntry(file.path));

                Path filePath = server.getFolders().getWorldsFolder().resolve(file.path);
                try (InputStream stream = Files.newInputStream(filePath)) {
                    transfer(stream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
            zipOutputStream.close();
        }
    }

    @Override
    public void onEnable(InactiveServer server, ActivePlugin plugin) {
        server.getCommandRegistry().register("backup", new BackupCommand(plugin));
        ConfigObject config = JsonConfigs.loadForPlugin(server.getFolders(), plugin);
        this.loginInfo = new LoginInfo(config);
        JsonConfigs.saveForPlugin(server.getFolders(), plugin, config);
    }

}
