/*
 * Copyright 2021 Monotonic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.artio.system_tests;

import uk.co.real_logic.artio.Side;
import uk.co.real_logic.artio.builder.HeaderEncoder;
import uk.co.real_logic.artio.session.Session;

import java.io.File;
import java.util.List;

import static uk.co.real_logic.artio.system_tests.SystemTestUtil.*;

/**
 * Makes a big archive example for {@link ArchiveScannerBenchmark}.
 */
public final class ArchiveScannerBenchmarkGenerator
{
    static final int SEND_BATCH = 5_000;

    public static void main(final String[] args)
    {
        new ArchiveScannerBenchmarkGenerator(false, true).setupBenchmark();
    }

    private ArchiveScannerBenchmarkGenerator(final boolean connectOtherSessions, final boolean onlySendMessages)
    {
        this.connectOtherSessions = connectOtherSessions;
        this.onlySendMessages = onlySendMessages;
    }

    // Reuse infrastructure from IT
    private final ArchiveScannerIntegrationTest test = new ArchiveScannerIntegrationTest();

    private final boolean connectOtherSessions;
    private final boolean onlySendMessages;

    private ReportFactory reportFactory;

    private void setupBenchmark()
    {
        System.out.println("Running in: " + new File(".").getAbsolutePath());

        test.launch();

        test.acquireAcceptingSession();

        try
        {
            final int otherSessionCount = 4;
            final int initialSuffix = 2;
            final Session[] sessions = new Session[otherSessionCount];

            if (connectOtherSessions)
            {
                for (int i = 0; i < otherSessionCount; i++)
                {
                    sessions[i] = test.completeConnectSessions(initiate(
                        test.initiatingLibrary, test.port, initiatorCompId(initialSuffix, i), ACCEPTOR_ID));
                }
            }
            else // Just create them
            {
                for (int i = 0; i < otherSessionCount; i++)
                {
                    final HeaderEncoder headerEncoder = new HeaderEncoder()
                        .senderCompID(ACCEPTOR_ID)
                        .targetCompID(initiatorCompId(initialSuffix, i));

                    test.testSystem.awaitCompletedReply(
                        test.acceptingLibrary.followerSession(headerEncoder, 10_000));
                }
            }

            for (int i = 0; i < 900; i++)
            {
                exchangeMessages(SEND_BATCH);

                System.out.println(i);
            }

            final long start = test.nanoClock.nanoTime();

            if (connectOtherSessions)
            {
                for (int i = 0; i < otherSessionCount; i++)
                {
                    exchangeMessages(SEND_BATCH, sessions[i]);
                }
            }

            exchangeMessages(SEND_BATCH);

            final long end = test.nanoClock.nanoTime();

            for (int i = 0; i < 900; i++)
            {
                exchangeMessages(SEND_BATCH);

                System.out.println(i);
            }

            System.out.println("start = " + start);
            System.out.println("end = " + end);

            System.out.println("mediaDriver = " + test.mediaDriver.mediaDriver().aeronDirectoryName());
            final File archiveDir = test.mediaDriver.archive().context().archiveDir();
            System.out.println("mediaDriver.archive().context().archiveDir() = " + archiveDir);
            System.out.println("acceptingEngine = " + test.acceptingEngine.configuration().logFileDir());
            System.out.println("new File(\".\") = " + new File(".").getAbsolutePath());
        }
        finally
        {
            test.close();
        }
    }

    private String initiatorCompId(final int initialSuffix, final int i)
    {
        return INITIATOR_ID + (i + initialSuffix);
    }

    private void exchangeMessages(final int n)
    {
        exchangeMessages(n, test.initiatingSession);
    }

    private void exchangeMessages(final int n, final Session initSession)
    {
        final List<FixMessage> receivedMessages = test.initiatingOtfAcceptor.messages();
        receivedMessages.clear();

        if (onlySendMessages)
        {
            ReportFactory reportFactory = this.reportFactory;
            if (reportFactory == null)
            {
                reportFactory = this.reportFactory = new ReportFactory();
            }

            for (int i = 0; i < n; i++)
            {
                reportFactory.sendReport(test.testSystem, test.acceptingSession, Side.SELL);
            }

            test.testSystem.await("Failed to receive messages", () -> receivedMessages.size() >= n);
        }
        else
        {
            String testReqID = null;
            for (int i = 0; i < n; i++)
            {
                testReqID = testReqId();
                sendTestRequest(test.testSystem, initSession, testReqID);
            }
            assertReceivedSingleHeartbeat(test.testSystem, test.initiatingOtfAcceptor, testReqID);
        }

        receivedMessages.clear();
    }
}
