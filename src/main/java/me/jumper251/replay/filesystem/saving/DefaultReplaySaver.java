package me.jumper251.replay.filesystem.saving;


import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.utils.LogUtils;
import me.jumper251.replay.utils.fetcher.Acceptor;
import me.jumper251.replay.utils.fetcher.Consumer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DefaultReplaySaver implements IReplaySaver {

    public final static File DIR = new File(ReplaySystem.getInstance().getDataFolder() + "/replays/");
    private boolean reformatting;

    private ExecutorService pool = Executors.newCachedThreadPool(Thread.ofVirtual().factory());

    @Override
    public void saveReplay(Replay replay) {
        if (!DIR.exists()) DIR.mkdirs();
        File file = new File(DIR, replay.getId() + ".replay");

        try {
            if (!file.exists()) file.createNewFile();

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                try (FilterOutputStream out = ConfigManager.getCompressOutputStream(fileOut)) {
                    try (ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
                        objectOut.writeObject(replay.getData());
                        objectOut.flush();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadReplay(String replayName, Consumer<Replay> consumer) {

        this.pool.execute(new Acceptor<>(consumer) {
            @Override
            public Replay getValue() {
                try {
                    File file = new File(DIR, replayName + ".replay");

                    ReplayData data;

                    try (FileInputStream fileIn = new FileInputStream(file)) {
                        try (FilterInputStream in = ConfigManager.getCompressInputStream(fileIn)) {
                            try (ObjectInputStream objectIn = new ObjectInputStream(in)) {
                                data = (ReplayData) objectIn.readObject();
                            }
                        }
                    }

                    if (data != null) return new Replay(replayName, data);
                } catch (Exception e) {
                    if (!reformatting) e.printStackTrace();
                }

                return null;
            }
        });
    }

    @Override
    public boolean replayExists(String replayName) {
        File file = new File(DIR, replayName + ".replay");
        return file.exists();
    }

    @Override
    public void deleteReplay(String replayName) {
        File file = new File(DIR, replayName + ".replay");
        if (file.exists()) file.delete();
    }

    public void reformatAll() {
        this.reformatting = true;
        if (DIR.exists()) {
            Arrays.asList(DIR.listFiles()).stream()
                    .filter(file -> (file.isFile() && file.getName().endsWith(".replay")))
                    .map(File::getName)
                    .collect(Collectors.toList())
                    .forEach(file -> reformat(file.replaceAll("\\.replay", "")));
        }
        this.reformatting = false;
    }

    private void reformat(String replayName) {
        loadReplay(replayName, old -> {
            if (old == null) {
                LogUtils.log("Reformatting: " + replayName);

                try {
                    File file = new File(DIR, replayName + ".replay");

                    ReplayData data;
                    try (FileInputStream fileIn = new FileInputStream(file)) {
                        try (ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
                            data = (ReplayData) objectIn.readObject();
                        }
                    }

                    if (data != null) {
                        deleteReplay(replayName);
                        saveReplay(new Replay(replayName, data));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public List<String> getReplays() {
        List<String> files = new ArrayList<String>();

        if (DIR.exists()) {
            for (File file : Arrays.asList(DIR.listFiles())) {
                if (file.isFile() && file.getName().endsWith(".replay")) {
                    files.add(file.getName().replaceAll("\\.replay", ""));
                }
            }
        }
        return files;
    }

}
