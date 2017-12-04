package com.turn.ttorrent.client.network.keyProcessors;

import com.turn.ttorrent.client.network.TimeoutAttachment;
import com.turn.ttorrent.common.LoggerUtils;
import com.turn.ttorrent.common.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class CleanupKeyProcessor implements CleanupProcessor {

  private final static Logger logger = LoggerFactory.getLogger(CleanupKeyProcessor.class);

  private final TimeService myTimeService;

  public CleanupKeyProcessor(TimeService timeService) {
    this.myTimeService = timeService;
  }

  @Override
  public void processCleanup(SelectionKey key) {
    TimeoutAttachment attachment = KeyProcessorUtil.getAttachmentAsTimeoutOrNull(key);
    if (attachment == null) {
      key.cancel();
      return;
    }
    if (attachment.isTimeoutElapsed(myTimeService.now())) {

      SocketChannel channel = KeyProcessorUtil.getCastedChannelOrNull(key);
      if (channel == null) {
        key.cancel();
        return;
      }

      logger.debug("channel {} was inactive in specified timeout. Close channel...", channel);
      try {
        channel.close();
        key.cancel();
        attachment.onTimeoutElapsed(channel);
      } catch (IOException e) {
        LoggerUtils.errorAndDebugDetails(logger, "unable close channel {}", channel, e);
      }
    }
  }

  @Override
  public void processSelected(SelectionKey key) {
    TimeoutAttachment attachment = KeyProcessorUtil.getAttachmentAsTimeoutOrNull(key);
    if (attachment == null) {
      key.cancel();
      return;
    }
    attachment.communicatedNow(myTimeService.now());
  }
}
