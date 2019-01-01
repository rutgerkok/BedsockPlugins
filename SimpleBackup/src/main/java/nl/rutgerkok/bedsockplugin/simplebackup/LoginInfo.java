package nl.rutgerkok.bedsockplugin.simplebackup;

import nl.rutgerkok.bedsock.config.ConfigObject;

final class LoginInfo {

    private static String[] fixPath(String string) {
        string = string.replace('\\', '/');
        if (string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        int lastSlash = string.lastIndexOf('/');
        if (lastSlash == -1) {
            return new String[] { ".", string };
        }
        return new String[] { string.substring(0, lastSlash), string.substring(lastSlash + 1) };
    }

    public final String host;
    public final int port;
    public final String user;
    public final String pass;
    public final String folder;
    public final String file;

    public LoginInfo() {
        this(new ConfigObject());
    }

    LoginInfo(ConfigObject config) {
        this.host = config.getOrPlaceString("host", "ftp.example.org");
        this.port = config.getOrPlaceInt("port", 22);
        this.user = config.getOrPlaceString("user", "root");
        this.pass = config.getOrPlaceString("pass", "minecr4ft");
        String[] path = fixPath(config.getOrPlaceString("path", "backups/world.zip"));
        this.folder = path[0];
        this.file = path[1];
    }

}
