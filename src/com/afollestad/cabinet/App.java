package com.afollestad.cabinet;

import android.app.Application;
import android.content.Context;
import com.afollestad.cabinet.file.CloudFile;
import com.afollestad.cabinet.utils.Clipboard;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Contains variables that are kept in memory throughout the life cycle of the application, even as activities open and close.
 *
 * @author Aidan Follestad (afollestad)
 */
public class App extends Application {

    private Clipboard mClipboard;
    private Session mSftpSession;
    private ChannelSftp mSftpChannel;
    private CloudFile mLastSftpTo;

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    public Clipboard getClipboard() {
        if (mClipboard == null)
            mClipboard = new Clipboard();
        return mClipboard;
    }

    public ChannelSftp getSftpChannel(CloudFile to) throws Exception {
        if ((mLastSftpTo != null && !mLastSftpTo.getHost().equals(to.getHost())) ||
                mLastSftpTo == null || (mSftpSession == null || !mSftpSession.isConnected()) || (mSftpChannel == null || !mSftpChannel.isConnected())) {
            mLastSftpTo = null;
            if (mSftpChannel != null)
                mSftpChannel.disconnect();
            if (mSftpSession != null)
                mSftpSession.disconnect();
            mLastSftpTo = to;
            JSch ssh = new JSch();
            mSftpSession = ssh.getSession(to.getUser(), to.getHost(), to.getPort());
            mSftpSession.setPassword(to.getPass());
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            mSftpSession.setConfig(config);
            mSftpSession.connect();
            mSftpChannel = (ChannelSftp) mSftpSession.openChannel("sftp");
            mSftpChannel.connect();
        }
        return mSftpChannel;
    }

    public Session getSftpSession(CloudFile to) throws Exception {
        getSftpChannel(to);
        return mSftpSession;
    }

    public void disconnectSftp() {
        if (mSftpChannel != null) {
            mSftpChannel.disconnect();
            mSftpChannel = null;
        }
        if (mSftpSession != null) {
            mSftpSession.disconnect();
            mSftpSession = null;
        }
    }
}