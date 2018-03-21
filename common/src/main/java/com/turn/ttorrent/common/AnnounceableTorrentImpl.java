package com.turn.ttorrent.common;

import java.util.Collections;
import java.util.List;

public class AnnounceableTorrentImpl implements AnnounceableFileTorrent {

  private final TorrentStatistic myTorrentStatistic;
  private final String myHexInfoHash;
  private final byte[] myInfoHash;
  private final List<List<String>> myAnnounceUrls;
  private final String myAnnounce;
  private final String myDownloadDirPath;
  private final String myDotTorrentFilePath;
  private final boolean myIsSeeded;
  private final boolean myIsLeeched;

  public AnnounceableTorrentImpl(TorrentStatistic torrentStatistic,
                                 String hexInfoHash,
                                 byte[] infoHash,
                                 List<List<String>> announceUrls,
                                 String announce,
                                 String downloadDirPath,
                                 String dotTorrentFilePath,
                                 boolean isSeeded,
                                 boolean isLeeched) {
    myTorrentStatistic = torrentStatistic;
    myHexInfoHash = hexInfoHash;
    myInfoHash = infoHash;
    myAnnounceUrls = Collections.unmodifiableList(announceUrls);
    myAnnounce = announce;
    myDotTorrentFilePath = dotTorrentFilePath;
    myDownloadDirPath = downloadDirPath;
    myIsSeeded = isSeeded;
    myIsLeeched = isLeeched;
  }

  @Override
  public boolean isLeeched() {
    return myIsLeeched;
  }

  @Override
  public boolean isSeeded() {
    return myIsSeeded;
  }

  @Override
  public String getDownloadDirPath() {
    return myDownloadDirPath;
  }

  @Override
  public String getDotTorrentFilePath() {
    return myDotTorrentFilePath;
  }

  @Override
  public long getUploaded() {
    return myTorrentStatistic.getUploadedBytes();
  }

  @Override
  public long getDownloaded() {
    return myTorrentStatistic.getDownloadedBytes();
  }

  @Override
  public long getLeft() {
    return myTorrentStatistic.getLeftBytes();
  }

  @Override
  public List<List<String>> getAnnounceList() {
    return myAnnounceUrls;
  }

  @Override
  public byte[] getInfoHash() {
    return myInfoHash;
  }

  @Override
  public String getHexInfoHash() {
    return myHexInfoHash;
  }

  @Override
  public String getAnnounce() {
    return myAnnounce;
  }

  @Override
  public TorrentStatistic getTorrentStatistic() {
    return myTorrentStatistic;
  }

  @Override
  public String toString() {
    return "AnnounceableTorrentImpl{" +
            "download directory='" + myDownloadDirPath + '\'' +
            ", dot torrent file='" + myDotTorrentFilePath + '\'' +
            '}';
  }
}
