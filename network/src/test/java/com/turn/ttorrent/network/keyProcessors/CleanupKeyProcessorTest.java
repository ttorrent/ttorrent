package com.turn.ttorrent.network.keyProcessors;

import static org.mockito.Mockito.*;

import com.turn.ttorrent.MutableClock;
import com.turn.ttorrent.network.TimeoutAttachment;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

@Test
public class CleanupKeyProcessorTest {

    private MutableClock myTimeService;
    private TimeoutAttachment myTimeoutAttachment;
    private SelectionKey myKey;
    private SelectableChannel myChannel;

    @BeforeMethod
    public void setUp() throws Exception {
        myTimeService = new MutableClock();
        myTimeoutAttachment = mock(TimeoutAttachment.class);
        myKey = mock(SelectionKey.class);
        myChannel = SocketChannel.open();
        when(myKey.channel()).thenReturn(myChannel);
        when(myKey.interestOps()).thenReturn(SelectionKey.OP_READ);
        myKey.attach(myTimeoutAttachment);
    }

    public void testSelected() {

        long oldTime = 10;
        myTimeService.setTime(oldTime);

        CleanupProcessor cleanupProcessor = new CleanupKeyProcessor(myTimeService);
        cleanupProcessor.processSelected(myKey);

        verify(myTimeoutAttachment).communicatedNow(eq(oldTime));

        long newTime = 100;
        myTimeService.setTime(newTime);

        cleanupProcessor.processSelected(myKey);

        verify(myTimeoutAttachment).communicatedNow(eq(newTime));
    }

    public void testCleanupWillCloseWithTimeout() throws Exception {

        when(myTimeoutAttachment.isTimeoutElapsed(anyLong())).thenReturn(true);

        CleanupProcessor cleanupProcessor = new CleanupKeyProcessor(myTimeService);
        cleanupProcessor.processCleanup(myKey);

        verify(myKey).cancel();
        verify(myKey).channel();
        verify(myTimeoutAttachment).onTimeoutElapsed(any(SocketChannel.class));
        verifyNoMoreInteractions(myKey);
    }

    public void testCleanupWithoutClose() {
        when(myTimeoutAttachment.isTimeoutElapsed(anyLong())).thenReturn(false);

        myTimeService.setTime(200);

        CleanupProcessor cleanupProcessor = new CleanupKeyProcessor(myTimeService);
        cleanupProcessor.processCleanup(myKey);

        verify(myTimeoutAttachment).isTimeoutElapsed(myTimeService.millis());
        verify(myKey, never()).cancel();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        myChannel.close();
    }
}
