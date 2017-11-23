package com.turn.ttorrent.client.network;

import com.turn.ttorrent.client.peer.PeerActivityListener;
import com.turn.ttorrent.common.LoggerUtils;
import com.turn.ttorrent.common.PeersStorageProvider;
import com.turn.ttorrent.common.TorrentsStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.*;

public class ConnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

  public static final int PORT_RANGE_START = 6881;
  public static final int PORT_RANGE_END = 6889;

  private final Selector selector;
  private final InetAddress inetAddress;
  private final CountDownLatch myWorkerShutdownChecker;
  private final ChannelListenerFactory channelListenerFactory;
  private volatile ConnectionWorker myConnectionWorker;
  private volatile ServerSocketChannel myServerSocketChannel;
  private InetSocketAddress myBindAddress;
  private final ExecutorService myExecutorService;
  private volatile Future<?> myWorkerFuture;

  public ConnectionManager(InetAddress inetAddress,
                           PeersStorageProvider peersStorageProvider,
                           TorrentsStorageProvider torrentsStorageProvider,
                           PeerActivityListener peerActivityListener,
                           ExecutorService executorService) throws IOException {
    this(inetAddress, new ChannelListenerFactoryImpl(peersStorageProvider, torrentsStorageProvider, peerActivityListener), executorService);
  }

  public ConnectionManager(InetAddress inetAddress,
                           ChannelListenerFactory channelListenerFactory,
                           ExecutorService executorService) throws IOException {
    this.myExecutorService = executorService;
    this.selector = Selector.open();
    this.inetAddress = inetAddress;
    this.channelListenerFactory = channelListenerFactory;
    this.myWorkerShutdownChecker = new CountDownLatch(1);
  }

  public void initAndRunWorker() throws IOException {
    myServerSocketChannel = selector.provider().openServerSocketChannel();
    myServerSocketChannel.configureBlocking(false);

    for (int port = PORT_RANGE_START; port < PORT_RANGE_END; port++) {
      try {
        InetSocketAddress tryAddress = new InetSocketAddress(inetAddress, port);
        myServerSocketChannel.socket().bind(tryAddress);
        myServerSocketChannel.register(selector, SelectionKey.OP_ACCEPT, channelListenerFactory);
        this.myBindAddress = tryAddress;
        break;
      } catch (IOException e) {
        //try next port
        logger.debug("Could not bind to port {}, trying next port...", port);
      }
    }
    if (this.myBindAddress == null) {
      throw new IOException("No available port for the BitTorrent client!");
    }
    myConnectionWorker = new ConnectionWorker(selector, myServerSocketChannel.getLocalAddress().toString(), myWorkerShutdownChecker);
    myWorkerFuture = myExecutorService.submit(myConnectionWorker);
  }

  public boolean connect(ConnectTask connectTask, int timeout, TimeUnit timeUnit) {
    if (myConnectionWorker == null) {
      return false;
    }
    return myConnectionWorker.connect(connectTask, timeout, timeUnit);
  }

  public InetSocketAddress getBindAddress() {
    return myBindAddress;
  }

  public void close(boolean await, int timeout, TimeUnit timeUnit) {
    logger.debug("try close connection manager...");
    boolean successfullyClosed = true;
    if (myConnectionWorker != null) {
      myWorkerFuture.cancel(true);
      myConnectionWorker.stop();
      if (await) {
        try {
          boolean shutdownCorrectly = this.myWorkerShutdownChecker.await(timeout, timeUnit);
          if (!shutdownCorrectly) {
            successfullyClosed = false;
            logger.warn("unable to terminate worker in {} {}", timeout, timeUnit);
          }
        } catch (InterruptedException e) {
          successfullyClosed = false;
          LoggerUtils.warnAndDebugDetails(logger, "unable to await termination worker, thread was interrupted", e);
        }
      }
    }
    try {
      this.myServerSocketChannel.close();
    } catch (Throwable e) {
      LoggerUtils.errorAndDebugDetails(logger, "unable to close server socket channel", e);
      successfullyClosed = false;
    }
    for (SelectionKey key : this.selector.keys()) {
      try {
        if (key.isValid()) {
          key.channel().close();
        }
      } catch (Throwable e) {
        logger.error("unable to close socket channel {}", key.channel());
        successfullyClosed = false;
        logger.debug("", e);
      }
    }
    try {
      this.selector.close();
    } catch (Throwable e) {
      LoggerUtils.errorAndDebugDetails(logger, "unable to close selector channel", e);
      successfullyClosed = false;
    }
    if (successfullyClosed) {
      logger.debug("connection manager is successfully closed");
    } else {
      logger.error("connection manager wasn't closed successfully");
    }
  }

  public void close(boolean await) {
    close(await, 1, TimeUnit.MINUTES);
  }

}
