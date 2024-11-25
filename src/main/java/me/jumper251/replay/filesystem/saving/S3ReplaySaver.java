package me.jumper251.replay.filesystem.saving;

import io.minio.*;
import io.minio.messages.Item;
import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.utils.fetcher.Consumer;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class S3ReplaySaver implements IReplaySaver {
    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    private List<String> replayNameCache;

    private MinioClient minioClient;

    public S3ReplaySaver(String endpointUrl, String accessKey, String secretKey, String bucketName) {
        this.endpointUrl = endpointUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.replayNameCache = new ArrayList<>();
    }

    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Connect to S3 backend
            minioClient = MinioClient.builder()
                    .endpoint(endpointUrl)
                    .credentials(accessKey, secretKey)
                    .build();

            // 2. Check, if specified bucket exists
            try {
                minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            } catch (Exception e) {
                return false;
            }

            // 3. Update cache
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            try {
                for (Result<Item> itemResult : objects) {
                    Item item = itemResult.get();
                    if (item.objectName().contains(".replay")) {
                        replayNameCache.add(item.objectName().split(".replay")[0]);
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }


            return true;
        });
    }

    @Override
    public void saveReplay(Replay replay) {
        CompletableFuture.runAsync(() -> {
            // 1. Write replay to temporary local file (taken from DefaultReplaySaver)
            File localReplayFile = new File(
                    ReplaySystem.getInstance().getDataFolder(),
                    UUID.randomUUID().toString() + ".replaytmp"
            );

            if (!localReplayFile.exists()) {
                try {
                    localReplayFile.createNewFile();

                    FileOutputStream fileOutputStream = new FileOutputStream(localReplayFile);
                    FilterOutputStream outputStream = ConfigManager.getCompressOutputStream(fileOutputStream);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

                    objectOutputStream.writeObject(replay.getData());
                    objectOutputStream.flush();

                    objectOutputStream.close();
                    outputStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // 2. Upload file to S3 backend
            try {
                minioClient.uploadObject(
                        UploadObjectArgs.builder()
                                .bucket(bucketName)
                                .object(replay.getId() + ".replay")
                                .filename(localReplayFile.getAbsolutePath())
                                .build()
                );
            } catch (Exception exception) {
                ReplaySystem.getInstance().getLogger().log(Level.SEVERE, "Could not upload replay to S3 backend!");
                exception.printStackTrace();
            }

            // 3. Delete temporary local file
            localReplayFile.delete();

            // 4. Add name to cache
            replayNameCache.add(replay.getId());
        });
    }

    @Override
    public void loadReplay(String replayName, Consumer<Replay> consumer) {
        CompletableFuture.runAsync(() -> {
            // 1. Create temporary local file
            File temporaryReplayFile = new File(
                    ReplaySystem.getInstance().getDataFolder(),
                    replayName + ".tmp"
            );

            if (temporaryReplayFile.exists())
                temporaryReplayFile.delete();


            // 2. Download from S3 into temporary local file
            try {
                minioClient.downloadObject(
                        DownloadObjectArgs.builder()
                                .bucket(bucketName)
                                .object(replayName + ".replay")
                                .filename(temporaryReplayFile.getAbsolutePath())
                                .build()
                );
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // 3. Load replay from temporary local file
            ReplayData replayData;
            try (FileInputStream fileInputStream = new FileInputStream(temporaryReplayFile)) {
                try (FilterInputStream inputStream = ConfigManager.getCompressInputStream(fileInputStream)){
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                        replayData = (ReplayData) objectInputStream.readObject();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (replayData != null) consumer.accept(new Replay(replayName, replayData));

            // 4. Delete temporary local file
            temporaryReplayFile.delete();
        });
    }

    @Override
    public boolean replayExists(String replayName) {
        // TODO: Change return type to CompletableFuture<Boolean>, to allow async checking of backend.
        return replayNameCache.contains(replayName);
    }

    @Override
    public void deleteReplay(String replayName) {
        replayNameCache.remove(replayName);
        CompletableFuture.runAsync(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(replayName + ".replay")
                                .build()
                );
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public List<String> getReplays() {
        return replayNameCache;
    }
}
