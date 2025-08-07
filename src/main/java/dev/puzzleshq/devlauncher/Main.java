package dev.puzzleshq.devlauncher;

import org.hjson.JsonObject;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Main {

    public static DevLauncher launcher;
    public static final File base = new File(".dev-launcher");
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.ENGLISH).withZone(ZoneId.of("UTC"));

    public static JsonObject puzzleManifestData;
    public static JsonObject gameManifestData;

    public static void main(String[] args) {
        if (!base.exists()) base.mkdir();

        puzzleManifestData = VersionChecker.check();
        gameManifestData = VersionCheckerCosmic.check();

        launcher = new DevLauncher();

        launcher.setVisible(true);
    }

}
