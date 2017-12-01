package com.turn.ttorrent.client.network;

import com.turn.ttorrent.client.peer.PeerActivityListener;
import com.turn.ttorrent.common.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class OutgoingConnectionListener implements ConnectionListener {

  private DataProcessor myNext;
  private final PeersStorageProvider myPeersStorageProvider;
  private final TorrentsStorageProvider myTorrentsStorageProvider;
  private final SharingPeerRegister mySharingPeerRegister;
  private final SharingPeerFactory mySharingPeerFactory;
  private final TorrentHash torrentHash;
  private final InetSocketAddress mySendAddress;

  public OutgoingConnectionListener(PeersStorageProvider myPeersStorageProvider,
                                    TorrentsStorageProvider myTorrentsStorageProvider,
                                    SharingPeerRegister sharingPeerRegister,
                                    SharingPeerFactory mySharingPeerFactory,
                                    TorrentHash torrentHash,
                                    InetSocketAddress sendAddress) {
    this.myPeersStorageProvider = myPeersStorageProvider;
    this.myTorrentsStorageProvider = myTorrentsStorageProvider;
    this.mySharingPeerRegister = sharingPeerRegister;
    this.mySharingPeerFactory = mySharingPeerFactory;
    this.torrentHash = torrentHash;
    this.mySendAddress = sendAddress;
  }

  @Override
  public void onNewDataAvailable(SocketChannel socketChannel) throws IOException {
    this.myNext = this.myNext.processAndGetNext(socketChannel);
  }

  @Override
  public void onConnectionEstablished(SocketChannel socketChannel) throws IOException {
    HandshakeSender handshakeSender = new HandshakeSender(
            torrentHash,
            myPeersStorageProvider,
            myTorrentsStorageProvider,
            mySharingPeerRegister,
            mySendAddress, mySharingPeerFactory);
    this.myNext = handshakeSender.processAndGetNext(socketChannel);
  }

  @Override
  public void onError(SocketChannel socketChannel, Throwable ex) throws IOException {
    this.myNext.handleError(socketChannel, ex);
  }
}
