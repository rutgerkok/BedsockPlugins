package nl.rutgerkok.bedsockplugin.simplelogger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import nl.rutgerkok.bedsock.InactiveServer;
import nl.rutgerkok.bedsock.logger.Logger.LogLevel;
import nl.rutgerkok.bedsock.logger.PersistentLogger;
import nl.rutgerkok.bedsock.plugin.ActivePlugin;
import nl.rutgerkok.bedsock.plugin.Plugin;

public class Main implements Plugin {

    private static class ToJulLogger implements PersistentLogger {

        private final java.util.logging.Logger julLogger;

        ToJulLogger() throws IOException {
            julLogger = java.util.logging.Logger.getLogger("Bedsock");

            System.setProperty("java.util.logging.SimpleFormatter.format",
                    "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s %5$s %6$s%n");

            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setLevel(Level.FINEST);
            fileHandler.setFormatter(new SimpleFormatter());

            julLogger.setLevel(Level.FINEST);
            julLogger.setUseParentHandlers(false);
            julLogger.addHandler(fileHandler);
        }

        @Override
        public void chat(String name, String message) {
            julLogger.info("<" + name + "> " + message);
        }

        @Override
        public void error(String message, Throwable e) {
            julLogger.log(Level.SEVERE, message, e);
        }

        @Override
        public void log(LogLevel level, String message) {
            julLogger.log(toJulLevel(level), message);
        }

    }

    private static Level toJulLevel(LogLevel level) {
        switch (level) {
        case DEBUG:
            return Level.FINE;
        case ERROR:
            return Level.SEVERE;
        case INFO:
            return Level.INFO;
        case WARNING:
            return Level.WARNING;
        default:
            return Level.INFO;
        
        }
    }

    @Override
    public void onEnable(InactiveServer server, ActivePlugin plugin) {
        try {
            server.getServerLogger().getListeners().add(new ToJulLogger());
        } catch (IOException e) {
            plugin.getLogger().error("Failed to initialize logger", e);
        }
    }

}
