package nl.rutgerkok.bedsockplugin.simplebackup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import nl.rutgerkok.bedsock.ActiveServer;
import nl.rutgerkok.bedsock.InactiveServer;
import nl.rutgerkok.bedsock.Scheduler;
import nl.rutgerkok.bedsock.ServerFolders;
import nl.rutgerkok.bedsock.command.CommandArgs;
import nl.rutgerkok.bedsock.command.CommandException;
import nl.rutgerkok.bedsock.command.CommandSender;
import nl.rutgerkok.bedsock.command.WrapperCommand;
import nl.rutgerkok.bedsock.config.ConfigObject;
import nl.rutgerkok.bedsock.config.JsonConfigs;
import nl.rutgerkok.bedsock.event.EventHandler;
import nl.rutgerkok.bedsock.event.Listener;
import nl.rutgerkok.bedsock.event.wrapper.ServerLaunchEvent;
import nl.rutgerkok.bedsock.logger.Logger;
import nl.rutgerkok.bedsock.plugin.ActivePlugin;
import nl.rutgerkok.bedsock.plugin.Plugin;
import nl.rutgerkok.bedsock.util.TimeSpan;
import nl.rutgerkok.bedsockplugin.simplebackup.uploader.SendingFile;
import nl.rutgerkok.bedsockplugin.simplebackup.uploader.Uploader;

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
            boolean success = startBackup(server, getLogger());

            if (!success) {
                throw new CommandException("Failed to backup files - could not parse command output");
            }
        }

    }

    private class ServerStartListener extends Listener {

        protected ServerStartListener(ActivePlugin plugin) {
            super(plugin);
        }

        @EventHandler
        public void onServerLaunch(ServerLaunchEvent event) {
            ActiveServer server = event.getServer();
            server.getScheduler().runOnMainThreadRepeating(getPlugin(), () -> {
                try {
                    startBackup(server, getLogger());
                } catch (CommandException e) {
                    getLogger().error("Error starting backup", e);
                }
            },
                    TimeSpan.hours(configuration.automaticBackupHours));
            getLogger().info("Running automated backups every " + configuration.automaticBackupHours + " hours");
        }

    }

    private Settings configuration;

    private void backupFiles(ServerFolders folders, List<SendingFile> files) {
        Uploader uploader = Uploader.fromSettings(this.configuration.uploadSettings);
        try {
            uploader.backupFiles(folders, files);
        } catch (IOException e) {
            throw new RuntimeException("Backup failed", e);
        }
    }

    @Override
    public void onEnable(InactiveServer server, ActivePlugin plugin) {
        server.getCommandRegistry().register("backup", new BackupCommand(plugin));
        ConfigObject config = JsonConfigs.loadForPlugin(server.getFolders(), plugin);
        this.configuration = new Settings(config);
        JsonConfigs.saveForPlugin(server.getFolders(), plugin, config);

        if (this.configuration.automaticBackupHours > 0) {
            server.getEventRegistry().registerHandler(new ServerStartListener(plugin));
        }
    }

    private boolean startBackup(ActiveServer server, Logger logger) throws CommandException {
        logger.info("Starting backup...");
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
                        .runAsync(() -> backupFiles(server.getFolders(), files), scheduler.workerThreadExecutor())
                        .handleAsync((Void r, Throwable exception) -> {
                            if (exception != null) {
                                logger.error("Backup failed", exception.getCause());
                            } else {
                                logger.info("Backup complete!");
                            }

                            try {
                                server.getBedrockCommandRunner()
                                        .runAndRecordCommand(server.parseCommand("save resume"));
                            } catch (CommandException e) {
                                logger.error("Error resuming saving", e);
                            }
                            return r;
                        }, scheduler.mainThreadExecutor());
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        return success;
    }

}
