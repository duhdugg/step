package com.tyndalehouse.step.core.models;

import java.io.Serializable;

public class HomeDirectoryInfo implements Serializable {
    private String path;
    private long mtime;
    private long size;

    public HomeDirectoryInfo(String path, long mtime, long size) {
        this.path = path;
        this.mtime = mtime;
        this.size = size;
    }

    // Getters are required for JSON serialization
    public String getPath() { return path; }
    public long getMtime() { return mtime; }
    public long getSize() { return size; }
}
