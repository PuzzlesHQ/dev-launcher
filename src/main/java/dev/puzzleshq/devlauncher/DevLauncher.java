package dev.puzzleshq.devlauncher;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DevLauncher extends JFrame {

    public JPanel panel = new JPanel();

    public DevLauncher() {
        super("Puzzle Dev-Launcher");

        setSize(400, 150);
        setResizable(false);

        initPanel();

        add(panel);
    }

    JsonObject versions = new JsonObject();

    public void initPanel() {
        String[] data = Main.puzzleManifestData.get("versions").asObject().names().toArray(new String[0]);
        String[] inv = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            inv[i] = data[data.length - 1 - i];
        }
        JComboBox<String> loaderVersions = new JComboBox<>(inv);
        JsonArray jVersions = Main.gameManifestData.get("versions").asArray();
        for (int i = 0; i < jVersions.size(); i++) {
            try {
                versions.add(jVersions.get(i).asObject().get("id").asString(), jVersions.get(i).asObject().get("client").asObject().get("url").asString());
            } catch (Exception ignore) {}
        }
        JComboBox<String> gameVersions = new JComboBox<>(versions.names().toArray(new String[0]));

        JButton button = new JButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadAction(loaderVersions);
            }
        });
        button.setText("Download Loader");
        JButton button2 = new JButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadGameAction(gameVersions);
            }
        });
        button2.setText("Download Cosmic Version");
        JButton run = new JButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File vFolder = new File(Main.base, ".puzzle-cosmic/" + loaderVersions.getSelectedItem());
                if (!vFolder.exists()) vFolder.mkdirs();
                File gFolder = new File("../../.cosmic-reach/" + gameVersions.getSelectedItem());
                if (!gFolder.exists()) gFolder.mkdirs();

                try {
                    new File(Main.base, "log.txt").createNewFile();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                Thread thread = new Thread(() -> {
                    ProcessBuilder builder = new ProcessBuilder("java", "-cp", "\"" + "lib/*" + File.pathSeparator + gFolder + "/game.jar" + "\"", "dev.puzzleshq.puzzleloader.loader.launch.pieces.ClientPiece");
                    builder.directory(vFolder);
                    builder.redirectOutput();
                    builder.redirectError();
                    try {
                        builder.start();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                thread.setName("Cosmic Thread");
                thread.setDaemon(false);
                thread.start();
            }
        });
        run.setText("Run Game");

        panel.add(loaderVersions);
        panel.add(button);
        panel.add(gameVersions);
        panel.add(button2);
        panel.add(run);
    }

    private void downloadGameAction(JComboBox<String> gameVersions) {
        File vFolder = new File(Main.base, ".cosmic-reach/" + gameVersions.getSelectedItem());
        if (!vFolder.exists()) vFolder.mkdirs();

        VersionChecker.download(versions.get((String) gameVersions.getSelectedItem()).asString(), new File(vFolder, "game.jar"));
    }

    private void downloadAction(JComboBox<String> versions) {
        File vFolder = new File(Main.base, ".puzzle-cosmic/" + versions.getSelectedItem());
        if (!vFolder.exists()) vFolder.mkdirs();
        File vLib = new File(vFolder, "lib");
        if (!vLib.exists()) vLib.mkdirs();
        File deps = new File(vFolder, versions.getSelectedItem() + "-deps.json");
        try {
            if (!deps.exists()) deps.createNewFile();

            JsonObject version = Main.gameManifestData.get("versions").asObject().get(versions.getSelectedItem().toString()).asObject();
            downloadMaven("https://repo1.maven.org/maven2/", version.get("maven-central").asString() + ":client", new File(vLib, "puzzle-loader-cosmic-"+versions.getSelectedItem().toString()+"-client.jar"));

            InputStream stream = new URL(version.get("dependencies").asString()).openStream();
            byte[] bytes = stream.readAllBytes();
            stream.close();

            FileOutputStream fileOutputStream = new FileOutputStream(deps);
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.close();

            String depStr = new String(bytes);
            JsonObject object = JsonValue.readJSON(depStr).asObject();
            JsonArray repos = object.get("repos").asArray();
            repos.add(new JsonObject(){{
                add("url", "https://repo1.maven.org/maven2/");
            }});

            download(vLib, repos, object.get("common").asArray());
            download(vLib, repos, object.get("client").asArray());
        } catch (IOException ea) {
            throw new RuntimeException(ea);
        }
    }

    private void download(File vLib, JsonArray repos, JsonArray common) {
        common.forEach(a -> {
            JsonObject artifact = a.asObject();
            if (!artifact.get("type").asString().equals("implementation")) return;
            try {
                for (JsonValue repo : repos) {
                    if (artifact.get("classifier") == null || artifact.get("classifier").isNull()) {
                        downloadMaven(repo.asObject().get("url").asString(),
                                artifact.get("groupId").asString() + ":" +
                                        artifact.get("artifactId").asString() + ":" +
                                        artifact.get("version").asString()
                                , new File(vLib, artifact.get("artifactId").asString() + "-" + artifact.get("version").asString() + ".jar"));
                    } else {
                        downloadMaven(repo.asObject().get("url").asString(),
                                artifact.get("groupId").asString() + ":" +
                                        artifact.get("artifactId").asString() + ":" +
                                        artifact.get("version").asString() + ":" +
                                        artifact.get("classifier").asString()
                                , new File(vLib, artifact.get("artifactId").asString() + "-" + artifact.get("version").asString() + "-" + artifact.get("classifier").asString() + ".jar"));

                        downloadManifest(vLib.getParentFile(), vLib, artifact);
                    }
                }
            } catch (Exception e) {
            }
        });
    }

    private void downloadManifest(File f, File libs, JsonObject artifact) {
        File manifest = new File(f, artifact.get("artifactId").asString() + "-version-manifest.json");
        File depFile = new File(f, artifact.get("artifactId").asString() + "-dependencies.json");

        try {
            new URL("https://raw.githubusercontent.com/PuzzlesHQ/" + artifact.get("artifactId").asString() + "/refs/heads/versioning/versions.json").openStream().close();
        } catch (Exception ignore) {
            return;
        }

        VersionChecker.download(
                "https://raw.githubusercontent.com/PuzzlesHQ/" + artifact.get("artifactId").asString() + "/refs/heads/versioning/versions.json",
                manifest
        );

        JsonObject object = JsonValue.readJSON(VersionChecker.read(manifest)).asObject();
        String depUrl = object.get("versions").asObject().get(artifact.get("version").asString()).asObject().get("dependencies").asString();

        VersionChecker.download(
                depUrl,
                depFile
        );

        object = JsonValue.readJSON(VersionChecker.read(depFile)).asObject();
        JsonArray repos = object.get("repos").asArray();

        repos.add(new JsonObject(){{
            add("url", "https://repo1.maven.org/maven2/");
        }});

        download(libs, repos, object.get(artifact.get("classifier").asString()).asArray());
    }

    private void downloadMaven(String url, String string, File file) {
        String[] strings = string.split(":");
        if (strings.length == 3) {
            url += strings[0].replaceAll("\\.", "/");
            url += "/" + strings[1];
            url += "/" + strings[2];

            url += "/" + strings[1] + "-" + strings[2] + ".jar";
        } else {
            url += strings[0].replaceAll("\\.", "/");
            url += "/" + strings[1];
            url += "/" + strings[2];

            url += "/" + strings[1] + "-" + strings[2] + "-" + strings[3] + ".jar";
        }

        try {
            if (!file.exists()) file.createNewFile();
            else return;
            InputStream stream = new URL(url).openStream();
            byte[] bytes = stream.readAllBytes();
            stream.close();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes, 0, bytes.length);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(string);
    }

}
