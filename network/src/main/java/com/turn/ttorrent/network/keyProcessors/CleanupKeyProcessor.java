package com.turn.ttorrent.network.keyProcessors;

import com.turn.ttorrent.common.LoggerUtils;
import com.turn.ttorrent.common.TorrentLoggerFactory;
import com.turn.ttorrent.network.TimeoutAttachment;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.time.Clock;

public class CleanupKeyProcessor implements CleanupProcessor {

    private static final Logger logger = TorrentLoggerFactory.getLogger(CleanupKeyProcessor.class);

    private final Clock myTimeService;

    public CleanupKeyProcessor(Clock timeService) {
        this.myTimeService = timeService;
    }

    @Override
    public void processCleanup(SelectionKey key) {
        TimeoutAttachment attachment = KeyProcessorUtil.getAttachmentAsTimeoutOrNull(key);
        if (attachment == null) {
            key.cancel();
            return;
        }
        if (attachment.isTimeoutElapsed(myTimeService.millis())) {

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
        attachment.communicatedNow(myTimeService.millis());
    }
}
