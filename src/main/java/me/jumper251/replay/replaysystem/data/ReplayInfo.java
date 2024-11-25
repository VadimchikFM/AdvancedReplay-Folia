package me.jumper251.replay.replaysystem.data;


public class ReplayInfo {

    private String id;

    private String creator;

    private Long time;

    private int compress;

    private int duration;


    public ReplayInfo(String id, String creator, Long time, int compress, int duration) {
        this.id = id;
        this.creator = creator;
        this.time = time;
        this.compress = compress;
        this.duration = duration;
    }

    public int getDuration() {
        return duration;
    }

    public String getCreator() {
        return creator;
    }

    public String getID() {
        return id;
    }

    public CompressType getCompress() {
        return CompressType.fromInt(compress);
    }

    public Long getTime() {
        return time;
    }


}
