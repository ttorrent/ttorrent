/**
 * Copyright (C) 2011-2013 Turn, Inc.
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
package com.turn.ttorrent.cli;

import com.turn.ttorrent.client.CommunicationManager;
import com.turn.ttorrent.client.SimpleClient;

import jargs.gnu.CmdLineParser;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.net.*;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Enumeration;

/** Command-line entry-point for starting a {@link CommunicationManager} */
public class ClientMain {

    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

    /** Default data output directory. */
    private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";

    /**
     * Returns a usable {@link Inet4Address} for the given interface name.
     *
     * <p>If an interface name is given, return the first usable IPv4 address for that interface. If
     * no interface name is given or if that interface doesn't have an IPv4 address, return's
     * localhost address (if IPv4).
     *
     * <p>It is understood this makes the client IPv4 only, but it is important to remember that
     * most BitTorrent extensions (like compact peer lists from trackers and UDP tracker support)
     * are IPv4-only anyway.
     *
     * @param iface The network interface name.
     * @return A usable IPv4 address as a {@link Inet4Address}.
     * @throws UnsupportedAddressTypeException If no IPv4 address was available to bind on.
     */
    private static Inet4Address getIPv4Address(String iface)
            throws SocketException, UnsupportedAddressTypeException, UnknownHostException {
        if (iface != null) {
            Enumeration<InetAddress> addresses =
                    NetworkInterface.getByName(iface).getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address) {
                    return (Inet4Address) addr;
                }
            }
        }

        InetAddress localhost = InetAddress.getLocalHost();
        if (localhost instanceof Inet4Address) {
            return (Inet4Address) localhost;
        }

        throw new UnsupportedAddressTypeException();
    }

    /** Display program usage on the given {@link PrintStream}. */
    private static void usage(PrintStream s) {
        s.println("usage: Client [options] <torrent>");
        s.println();
        s.println("Available options:");
        s.println("  -h,--help                  Show this help and exit.");
        s.println("  -o,--output DIR            Read/write data to directory DIR.");
        s.println("  -i,--iface IFACE           Bind to interface IFACE.");
        s.println(
                "  -s,--seed SECONDS          Time to seed after downloading (default:"
                    + " infinitely).");
        s.println("  -d,--max-download KB/SEC   Max download rate (default: unlimited).");
        s.println("  -u,--max-upload KB/SEC     Max upload rate (default: unlimited).");
        s.println();
    }

    /** Main client entry point for stand-alone operation. */
    public static void main(String[] args) {
        BasicConfigurator.configure(
                new ConsoleAppender(new PatternLayout("%d [%-25t] %-5p: %m%n")));

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option output = parser.addStringOption('o', "output");
        CmdLineParser.Option iface = parser.addStringOption('i', "iface");
        CmdLineParser.Option seedTime = parser.addIntegerOption('s', "seed");
        CmdLineParser.Option maxUpload = parser.addDoubleOption('u', "max-upload");
        CmdLineParser.Option maxDownload = parser.addDoubleOption('d', "max-download");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException oe) {
            System.err.println(oe.getMessage());
            usage(System.err);
            System.exit(1);
        }

        // Display help and exit if requested
        if (Boolean.TRUE.equals((Boolean) parser.getOptionValue(help))) {
            usage(System.out);
            System.exit(0);
        }

        String outputValue = (String) parser.getOptionValue(output, DEFAULT_OUTPUT_DIRECTORY);
        String ifaceValue = (String) parser.getOptionValue(iface);
        int seedTimeValue = (Integer) parser.getOptionValue(seedTime, -1);

        String[] otherArgs = parser.getRemainingArgs();
        if (otherArgs.length != 1) {
            usage(System.err);
            System.exit(1);
        }

        SimpleClient client = new SimpleClient();
        try {
            Inet4Address iPv4Address = getIPv4Address(ifaceValue);
            File torrentFile = new File(otherArgs[0]);
            File outputFile = new File(outputValue);

            client.downloadTorrent(
                    torrentFile.getAbsolutePath(), outputFile.getAbsolutePath(), iPv4Address);
            if (seedTimeValue > 0) {
                Thread.sleep(seedTimeValue * 1000);
            }

        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.exit(2);
        } finally {
            client.stop();
        }
    }
}
