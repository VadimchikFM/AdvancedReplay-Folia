package me.jumper251.replay.filesystem.saving;


import me.jumper251.replay.database.DatabaseRegistry;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.replaysystem.data.ReplayInfo;
import me.jumper251.replay.utils.fetcher.Acceptor;
import me.jumper251.replay.utils.fetcher.Consumer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseReplaySaver implements IReplaySaver {

    public static Map<String, ReplayInfo> replayCache = new HashMap<>();

    public static ReplayInfo getInfo(String replay) {
        if (replayCache != null && replayCache.containsKey(replay)) return replayCache.get(replay);

        return null;
    }

    @Override
    public void saveReplay(Replay replay) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
            try (FilterOutputStream out = ConfigManager.getCompressOutputStream(byteOut)) {
                try (ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
                    objectOut.writeObject(replay.getData());
                    objectOut.flush();
                    byte[] data = byteOut.toByteArray();

                    if (replay.getReplayInfo() == null)
                        replay.setReplayInfo(new ReplayInfo(replay.getId(), null, System.currentTimeMillis(), ConfigManager.RECORD_COMPRESSION.toInt(), replay.getData().getDuration()));
                    DatabaseRegistry.getDatabase().getService().addReplay(replay.getId(), replay.getReplayInfo().getCreator(), replay.getReplayInfo().getDuration(), ConfigManager.RECORD_COMPRESSION.toInt(), replay.getReplayInfo().getTime(), data);

                    updateCache(replay.getId(), replay.getReplayInfo());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void loadReplay(String replayName, Consumer<Replay> consumer) {

        DatabaseRegistry.getDatabase().getService().getPool().execute(new Acceptor<>(consumer) {

            @Override
            public Replay getValue() {
                try {
                    byte[] data = DatabaseRegistry.getDatabase().getService().getReplayData(replayName);

                    try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data)) {
                        try (FilterInputStream in = ConfigManager.getCompressInputStream(byteIn)) {
                            try (ObjectInputStream objectIn = new ObjectInputStream(in)) {
                                ReplayData replayData = (ReplayData) objectIn.readObject();
                                return new Replay(replayName, replayData);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }

    @Override
    public boolean replayExists(String replayName) {
        return replayCache.containsKey(replayName);
    }

    @Override
    public void deleteReplay(String replayName) {
        DatabaseRegistry.getDatabase().getService().deleteReplay(replayName);

        updateCache(replayName, null);
    }

    @Override
    public List<String> getReplays() {
        return new ArrayList<>(replayCache.keySet());
    }

    private void updateCache(String id, ReplayInfo info) {
        if (info != null && id != null) {
            replayCache.put(id, info);
        } else if (replayCache.containsKey(id)) {
            replayCache.remove(id);
        }
    }
}
