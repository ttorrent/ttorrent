/**
 * Copyright (C) 2011-2012 Turn, Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turn.ttorrent.tracker;

import com.turn.ttorrent.common.Peer;
import com.turn.ttorrent.common.TorrentLoggerFactory;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.time.Clock;

/**
 * A BitTorrent tracker peer.
 *
 * <p>
 *
 * <p>Represents a peer exchanging on a given torrent. In this implementation, we don't really care
 * about the status of the peers and how much they have downloaded / exchanged because we are not a
 * torrent exchange and don't need to keep track of what peers are doing while they're downloading.
 * We only care about when they start, and when they are done.
 *
 * <p>
 *
 * <p>We also never expire peers automatically. Unless peers send a STOPPED announce request, they
 * remain as long as the torrent object they are a part of.
 */
public class TrackedPeer extends Peer {

    private static final Logger logger = TorrentLoggerFactory.getLogger(TrackedPeer.class);

    private final Clock myTimeService;
    private final TrackedTorrent torrent;
    private long uploaded;
    private long downloaded;
    private long left;

    /**
     * Represents the state of a peer exchanging on this torrent.
     *
     * <p>
     *
     * <p>Peers can be in the STARTED state, meaning they have announced themselves to us and are
     * eventually exchanging data with other peers. Note that a peer starting with a completed file
     * will also be in the started state and will never notify as being in the completed state. This
     * information can be inferred from the fact that the peer reports 0 bytes left to download.
     *
     * <p>
     *
     * <p>Peers enter the COMPLETED state when they announce they have entirely downloaded the file.
     * As stated above, we may also elect them for this state if they report 0 bytes left to
     * download.
     *
     * <p>
     *
     * <p>Peers enter the STOPPED state very briefly before being removed. We still pass them to the
     * STOPPED state in case someone else kept a reference on them.
     */
    public enum PeerState {
        UNKNOWN,
        STARTED,
        COMPLETED,
        STOPPED
    }

    private PeerState state;
    private long lastAnnounce;

    /**
     * Instantiate a new tracked peer for the given torrent.
     *
     * @param torrent The torrent this peer exchanges on.
     * @param ip The peer's IP address.
     * @param port The peer's port.
     * @param peerId The byte-encoded peer ID.
     */
    public TrackedPeer(TrackedTorrent torrent, String ip, int port, ByteBuffer peerId) {
        this(torrent, ip, port, peerId, Clock.systemUTC());
    }

    TrackedPeer(TrackedTorrent torrent, String ip, int port, ByteBuffer peerId, Clock timeService) {
        super(ip, port, peerId);
        myTimeService = timeService;
        this.torrent = torrent;

        // Instantiated peers start in the UNKNOWN state.
        this.state = PeerState.UNKNOWN;
        this.lastAnnounce = myTimeService.millis();

        this.uploaded = 0;
        this.downloaded = 0;
        this.left = 0;
    }

    /**
     * Update this peer's state and information.
     *
     * <p>
     *
     * <p><b>Note:</b> if the peer reports 0 bytes left to download, its state will be automatically
     * be set to COMPLETED.
     *
     * @param state The peer's state.
     * @param uploaded Uploaded byte count, as reported by the peer.
     * @param downloaded Downloaded byte count, as reported by the peer.
     * @param left Left-to-download byte count, as reported by the peer.
     */
    public void update(PeerState state, long uploaded, long downloaded, long left) {
        if (PeerState.STARTED.equals(state) && left == 0) {
            state = PeerState.COMPLETED;
        }

        if (!state.equals(this.state)) {
            logger.trace(
                    "Peer {} {} download of {}.", this, state.name().toLowerCase(), this.torrent);
        }

        this.state = state;
        this.lastAnnounce = myTimeService.millis();
        this.uploaded = uploaded;
        this.downloaded = downloaded;
        this.left = left;
    }

    /** Tells whether this peer has completed its download and can thus be considered a seeder. */
    public boolean isCompleted() {
        return PeerState.COMPLETED.equals(this.state);
    }

    /** Returns how many bytes the peer reported it has uploaded so far. */
    public long getUploaded() {
        return this.uploaded;
    }

    /** Returns how many bytes the peer reported it has downloaded so far. */
    public long getDownloaded() {
        return this.downloaded;
    }

    /**
     * Returns how many bytes the peer reported it needs to retrieve before its download is
     * complete.
     */
    public long getLeft() {
        return this.left;
    }

    /**
     * Tells whether this peer has checked in with the tracker recently.
     *
     * <p>
     *
     * <p>Non-fresh peers are automatically terminated and collected by the Tracker.
     */
    public boolean isFresh(long expireTimeoutSec) {
        return this.lastAnnounce + expireTimeoutSec * 1000 > myTimeService.millis();
    }
}
