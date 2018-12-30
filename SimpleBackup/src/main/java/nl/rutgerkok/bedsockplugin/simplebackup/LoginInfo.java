package nl.rutgerkok.bedsockplugin.simplebackup;

import nl.rutgerkok.bedsock.config.ConfigObject;
import nl.rutgerkok.bedsock.config.InvalidConfigException;

final class LoginInfo {

    private static String fixPath(String string) {
        string = string.replace('\\', '/');
        if (string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public final String host;
    public final String user;
    public final String pass;
    public final String path;

    LoginInfo() {
        this.host = "ftp.example.com";
        this.user = "root";
        this.pass = "minecr4ft";
        this.path = "backups";
    }

    LoginInfo(ConfigObject config) throws InvalidConfigException {
        this.host = config.getString("host");
        this.user = config.getString("user");
        this.pass = config.getString("pass");
        this.path = fixPath(config.getString("path"));
    }

}
