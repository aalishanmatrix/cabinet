package com.afollestad.cabinet.file;

import android.content.Context;
import com.afollestad.cabinet.App;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class RemoteFile extends File {

    private final static String COLON_ENTITY = "&#58;";

    private transient Context mContext;
    private String mHost;
    private int mPort;
    private String mUser;
    private String mPass;
    private boolean isDirectory = true;
    private long mSize;

    public RemoteFile(Context context, String path) {
        mContext = context;
        if (path.startsWith("REMOTE:")) {
            String[] splitColons = path.split(":");
            mHost = splitColons[1].replace(COLON_ENTITY, ":");
            mPort = Integer.parseInt(splitColons[2]);
            mUser = splitColons[3].replace(COLON_ENTITY, ":");
            mPass = splitColons[4].replace(COLON_ENTITY, ":");
            mFile = new java.io.File(splitColons[5].replace(COLON_ENTITY, ":"));
        } else throw new IllegalArgumentException("Path doesn't represent a remote file.");
    }

    public RemoteFile(Context context, RemoteFile parent, java.io.File file) {
        mContext = context;
        mHost = parent.getHost();
        mPort = parent.getPort();
        mUser = parent.getUser();
        mPass = parent.getPass();
        mFile = file;
    }

    public RemoteFile(Context context, RemoteFile parent, String name) {
        mContext = context;
        mHost = parent.getHost();
        mPort = parent.getPort();
        mUser = parent.getUser();
        mPass = parent.getPass();
        mFile = new java.io.File(parent.mFile, name);
    }

    public RemoteFile(Context context, RemoteFile parent, ChannelSftp.LsEntry lsEntry) {
        mContext = context;
        mHost = parent.getHost();
        mPort = parent.getPort();
        mUser = parent.getUser();
        mPass = parent.getPass();
        mFile = new java.io.File(parent.mFile, lsEntry.getFilename());
        SftpATTRS attrs = lsEntry.getAttrs();
        isDirectory = attrs.isDir();
        mSize = attrs.getSize();
    }

    public RemoteFile(Context context, String host, String port, String user, String pass, String path) {
        mContext = context;
        mHost = host;
        mPort = Integer.parseInt(port);
        mUser = user;
        mPass = pass;
        mFile = new java.io.File(path);
    }

    public String getHost() {
        return mHost;
    }

    public int getPort() {
        return mPort;
    }

    public String getUser() {
        return mUser;
    }

    public String getPass() {
        return mPass;
    }

    @Override
    public String getName() {
        if (super.getName().trim().isEmpty() || super.getName().trim().equals("/"))
            return getHost();
        return super.getName();
    }

    @Override
    public String getDisplayName() {
        String path = getAbsolutePath();
        if (!path.startsWith("/")) path = "/" + path;
        return getUser() + "@" + getHost() + path;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public boolean isStorageDirectory() {
        return false;
    }

    @Override
    public boolean isRootDirectory() {
        return false;
    }

    @Override
    public boolean requiresRootAccess() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public boolean isHidden() {
        return super.isHidden();
    }

    @Override
    public long length() {
        return mSize;
    }

    @Override
    public boolean exists() throws Exception {
        return true; // assume it exists since it was just retrieved and doesn't get cached
    }

    @Override
    public void mkdir() throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        channel.mkdir(getAbsolutePath());
    }

    @Override
    public void mkdirs() throws Exception {
        mkdir();
    }

    @Override
    public void renameTo(File newPath) throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        channel.rename(getAbsolutePath(), newPath.getAbsolutePath());
    }

    @Override
    public File getParentFile() {
        return new RemoteFile(mContext, this, mFile.getParentFile());
    }

    @Override
    public boolean delete() throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        if (isDirectory()) channel.rmdir(getAbsolutePath());
        else channel.rm(getAbsolutePath());
        return true;
    }

    @Override
    public boolean deleteNonRecursive() throws Exception {
        return delete();
    }

    @Override
    public File[] listFiles() throws Exception {
        List<File> results = new ArrayList<File>();
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        Vector<ChannelSftp.LsEntry> ls = channel.ls(getAbsolutePath());
        for (ChannelSftp.LsEntry entry : ls)
            results.add(new RemoteFile(mContext, this, entry));
        return results.toArray(new File[results.size()]);
    }

    @Override
    public Object getSilkId() {
        return super.getSilkId();
    }

    @Override
    public String toString() {
        return "REMOTE:" + getHost().replace(":", COLON_ENTITY) + ":" +
                getPort() + ":" +
                getUser().replace(":", COLON_ENTITY) + ":" +
                getPass().replace(":", COLON_ENTITY) + ":" +
                getAbsolutePath().replace(":", COLON_ENTITY);
    }
}
