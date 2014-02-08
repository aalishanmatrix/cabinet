package com.afollestad.cabinet.file;

import android.content.Context;
import com.afollestad.cabinet.App;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class CloudFile extends File {

    private final static String COLON_ENTITY = "&#58;";

    private transient Context mContext;
    private String mHost;
    private int mPort;
    private String mUser;
    private String mPass;
    private boolean isDirectory = true;
    private long mSize;
    private java.io.File mFile;

    public CloudFile(Context context, String path) {
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

    public CloudFile(Context context, CloudFile parent, java.io.File file) {
        mContext = context;
        mHost = parent.getHost();
        mPort = parent.getPort();
        mUser = parent.getUser();
        mPass = parent.getPass();
        mFile = file;
    }

    public CloudFile(Context context, CloudFile parent, String name) {
        mContext = context;
        mHost = parent.getHost();
        mPort = parent.getPort();
        mUser = parent.getUser();
        mPass = parent.getPass();
        mFile = new java.io.File(parent.mFile, name);
    }

    public CloudFile(Context context, CloudFile parent, ChannelSftp.LsEntry lsEntry) {
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

    public CloudFile(Context context, String host, String port, String user, String pass, String path) {
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

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public void setSize(long size) {
        mSize = size;
    }

    @Override
    public String getName() {
        if (mFile.getName() == null || mFile.getName().trim().isEmpty() || isStorageDirectory() || isRootDirectory())
            return getHost();
        return mFile.getName();
    }

    @Override
    public String getDisplayName() {
        String path = getAbsolutePath();
        if (!path.startsWith("/")) path = "/" + path;
        return getUser() + "@" + getHost() + path;
    }

    @Override
    public String getAbsolutePath() {
        return mFile.getAbsolutePath();
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
        return false; //TODO
    }

    @Override
    public long length() {
        return mSize;
    }

    @Override
    public boolean isRemoteFile() {
        return true;
    }

    @Override
    public boolean exists() throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        try {
            channel.lstat(getAbsolutePath());
        } catch (SftpException e) {
            if (e.id == 2) return false;
            else throw e;
        }
        return true;
    }

    @Override
    public boolean mkdir() throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        channel.mkdir(getAbsolutePath());
        return true;
    }

    @Override
    public boolean mkdirs() throws Exception {
        return mkdir();
    }

    @Override
    public boolean renameTo(File newPath) throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        channel.rename(getAbsolutePath(), newPath.getAbsolutePath());
        return true;
    }

    @Override
    public File getParentFile() {
        return new CloudFile(mContext, this, mFile.getParentFile());
    }

    @Override
    public java.io.File getFile() {
        return mFile;
    }

    @Override
    public boolean delete() throws Exception {
        ChannelSftp channel = App.get(mContext).getSftpChannel(this);
        if (isDirectory()) {
//            String path = getAbsolutePath();
//            if (!path.endsWith("/")) path += "/";
//            channel.rm(path + "*"); // remove all files first
//            channel.rmdir(getAbsolutePath()); // remove directory itself
            ChannelExec exec = (ChannelExec) App.get(mContext).getSftpSession(this).openChannel("exec");
            exec.setCommand("rm -rf \"" + getAbsolutePath() + "\"");
            exec.setErrStream(System.err);
            exec.connect();
            exec.disconnect();

        } else channel.rm(getAbsolutePath());
        return true;
    }

    @Override
    public boolean deleteNonRecursive() throws Exception {
        return delete();
    }

    @Override
    public File[] listFilesUnthreaded() throws Exception {
        List<CloudFile> results = new ArrayList<CloudFile>();
        ChannelSftp channel = App.get(mContext).getSftpChannel(CloudFile.this);
        Vector<ChannelSftp.LsEntry> ls = channel.ls(getAbsolutePath());
        for (ChannelSftp.LsEntry entry : ls)
            results.add(new CloudFile(mContext, CloudFile.this, entry));
        return results.toArray(new File[results.size()]);
    }

    @Override
    public Object getSilkId() {
        return "REMOTE:" + getHost() + ":" + getAbsolutePath();
    }

    @Override
    public String toString() {
        return "REMOTE:" + getHost().replace(":", COLON_ENTITY) + ":" +
                getPort() + ":" +
                getUser().replace(":", COLON_ENTITY) + ":" +
                getPass().replace(":", COLON_ENTITY) + ":" +
                getAbsolutePath().replace(":", COLON_ENTITY);
    }

    @Override
    public String getMountedAs() {
        return null; //TODO
    }
}
