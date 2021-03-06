package nl.rutgerkok.bedsockplugin.simplebackup;

import nl.rutgerkok.bedsock.config.ConfigObject;
import nl.rutgerkok.bedsockplugin.simplebackup.uploader.UploadSettings;

final class Settings {

    final UploadSettings uploadSettings;
    final int automaticBackupHours;

    Settings(ConfigObject config) {
        this.uploadSettings = new UploadSettings(config.getOrPlaceObject("upload"));
        this.automaticBackupHours = config.getOrPlaceInt("automatic_backup_hours", 12);
    }
}
