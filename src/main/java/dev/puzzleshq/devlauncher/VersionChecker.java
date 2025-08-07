package dev.puzzleshq.devlauncher;

import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class VersionChecker {

    public static final URL vManifestUrl;

    static {
        try {
            vManifestUrl = new URL("https://raw.githubusercontent.com/PuzzlesHQ/puzzle-loader-cosmic/refs/heads/versioning/versions.json");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static final File vManifest = new File(Main.base, "puzzle-cosmic-version-manifest.json");
    public static final File refreshFile = new File(Main.base, "puzzle-cosmic-version-manifest.json.refresh");

    public static JsonObject check() {
        if (needsRefresh() || !vManifest.exists()) download();

        return getManifestData();
    }

    public static boolean needsRefresh() {
        String date = Main.formatter.format(LocalDateTime.now());

        boolean doRefresh = shouldRefresh(date);

        if (doRefresh) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(refreshFile);
                byte[] bytes = date.getBytes(StandardCharsets.UTF_8);
                fileOutputStream.write(bytes, 0, bytes.length);
                fileOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        return doRefresh;
    }

    private static JsonObject getManifestData() {
        try {
            FileInputStream stream = new FileInputStream(vManifest);
            byte[] bytes = stream.readAllBytes();
            stream.close();

            return JsonValue.readJSON(new String(bytes)).asObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean shouldRefresh(String date) {
        boolean doRefresh = !refreshFile.exists();

        if (doRefresh) {
            try {
                refreshFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        try {
            FileInputStream stream = new FileInputStream(refreshFile);
            byte[] bytes = stream.readAllBytes();
            stream.close();

            doRefresh = !new String(bytes).equals(date);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return doRefresh;
    }

    public static void download() {
        download(vManifestUrl.toString(), vManifest);
    }

    public static String read(File file) {
        try {
            FileInputStream stream = new FileInputStream(file);
            byte[] bytes = stream.readAllBytes();
            stream.close();

            return new String(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void download(String url, File file) {
        try {
            if (!file.exists()) file.createNewFile();

            InputStream stream = new URL(url).openStream();
            byte[] bytes = stream.readAllBytes();
            stream.close();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
