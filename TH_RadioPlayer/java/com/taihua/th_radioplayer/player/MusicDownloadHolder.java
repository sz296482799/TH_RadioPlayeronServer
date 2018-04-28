package com.taihua.th_radioplayer.player;

import com.taihua.th_radioplayer.download.DownloadHolder;
import com.taihua.th_radioplayer.download.DownloadInfo;
import com.taihua.th_radioplayer.download.DownloadState;
import com.taihua.th_radioplayer.manager.MusicManager;
import com.taihua.th_radioplayer.utils.LogUtil;
import org.xutils.common.Callback;

import java.io.File;

public class MusicDownloadHolder extends DownloadHolder {

    private MusicManager manager;
    private MusicItem musicItem;

    public MusicDownloadHolder(MusicManager manager, MusicItem item, DownloadInfo downloadInfo) {
        super(downloadInfo);

        this.manager = manager;
        this.musicItem = item;
    }

    private void setState(DownloadState state) {
        if(musicItem != null) {
            musicItem.setDowanloadStatus(state);
        }
    }

    private void touchDownloadFinish() {
        if(this.manager != null)
            this.manager.touchDownloadFinish(this.musicItem, this.downloadInfo);
    }

    @Override
    public void onWaiting() {
        LogUtil.d("DOWANLOAD_HOLDER", "onWaiting");
        setState(DownloadState.WAITING);
    }

    @Override
    public void onStarted() {
        LogUtil.d("DOWANLOAD_HOLDER", "onStarted");
        setState(DownloadState.STARTED);
    }

    @Override
    public void onLoading(long total, long current) {
        LogUtil.d("DOWANLOAD_HOLDER", "onLoading:" + current * 100 / total + "%");
    }

    @Override
    public void onSuccess(File result) {
        LogUtil.d("DOWANLOAD_HOLDER", "onSuccess");
        touchDownloadFinish();
        setState(DownloadState.FINISHED);
    }

    @Override
    public void onError(Throwable ex, boolean isOnCallback) {
        LogUtil.e("DOWANLOAD_HOLDER", "onError");
        touchDownloadFinish();
        setState(DownloadState.ERROR);
    }

    @Override
    public void onCancelled(Callback.CancelledException cex) {
        LogUtil.d("DOWANLOAD_HOLDER", "onCancelled");
    }
}
