/*
 * Copyright 2015-2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway.engine.logger;

import org.agrona.ErrorHandler;
import org.agrona.concurrent.IdleStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.co.real_logic.fix_gateway.builder.Encoder;
import uk.co.real_logic.fix_gateway.decoder.*;
import uk.co.real_logic.fix_gateway.fields.RejectReason;
import uk.co.real_logic.fix_gateway.replication.ClusterableSubscription;
import uk.co.real_logic.fix_gateway.util.MutableAsciiBuffer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static uk.co.real_logic.fix_gateway.CommonConfiguration.DEFAULT_NAME_PREFIX;
import static uk.co.real_logic.fix_gateway.engine.PossDupEnabler.POSS_DUP_FIELD;
import static uk.co.real_logic.fix_gateway.engine.logger.Replayer.MESSAGE_FRAME_BLOCK_LENGTH;
import static uk.co.real_logic.fix_gateway.engine.logger.Replayer.MOST_RECENT_MESSAGE;
import static uk.co.real_logic.fix_gateway.messages.MessageStatus.OK;
import static uk.co.real_logic.fix_gateway.util.AsciiBuffer.UNKNOWN_INDEX;
import static uk.co.real_logic.fix_gateway.util.CustomMatchers.sequenceEqualsAscii;

public class ReplayerTest extends AbstractLogTest
{
    public static final byte[] MESSAGE_REQUIRING_LONGER_BODY_LENGTH =
        ("8=FIX.4.4\0019=99\00135=1\00134=1\00149=LEH_LZJ02\00152=19700101-00:00:00.000\00156=CCG\001" +
            "112=a12345678910123456789101234567891012345\00110=005\001").getBytes(US_ASCII);

    private static final int MAX_CLAIM_ATTEMPTS = 100;

    private ReplayQuery replayQuery = mock(ReplayQuery.class);
    private ClusterableSubscription subscription = mock(ClusterableSubscription.class);
    private IdleStrategy idleStrategy = mock(IdleStrategy.class);
    private ErrorHandler errorHandler = mock(ErrorHandler.class);

    private Replayer replayer = new Replayer(
        replayQuery,
        publication,
        claim,
        idleStrategy,
        errorHandler,
        MAX_CLAIM_ATTEMPTS,
        subscription,
        DEFAULT_NAME_PREFIX);

    @Before
    public void setUp()
    {
        when(publication.tryClaim(anyInt(), any())).thenReturn(1L);
        when(replayQuery.query(eq(replayer), anyLong(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);
    }

    @Test
    public void shouldParseResendRequest()
    {
        final long result = bufferHasResendRequest(END_SEQ_NO);
        onMessage(ResendRequestDecoder.MESSAGE_TYPE, result);

        verifyQueriedService(END_SEQ_NO);
        verifyNoMoreInteractions(publication);
    }

    @Test
    public void shouldPublishAllRemainingMessages()
    {
        final long result = bufferHasResendRequest(MOST_RECENT_MESSAGE);
        onMessage(ResendRequestDecoder.MESSAGE_TYPE, result);

        verifyQueriedService(MOST_RECENT_MESSAGE);
        verifyNoMoreInteractions(publication);
    }

    @Test
    public void shouldPublishMessagesWithSetPossDupFlag()
    {
        bufferContainsExampleMessage(true);

        final int srcLength = fragmentLength();
        setupMessage(srcLength);

        replayer.onFragment(buffer, START, srcLength, null);

        verifyClaim(srcLength);
        assertHasSetPossDupFlag();
        verifyCommit();
    }

    @Test
    public void shouldGapFillAdminMessages()
    {
        final long result = bufferHasResendRequest(END_SEQ_NO);
        onMessage(ResendRequestDecoder.MESSAGE_TYPE, result);

        bufferContainsTestRequest(SESSION_ID, SEQUENCE_NUMBER, SEQUENCE_INDEX);
        final int offset = setupCapturingClaim();
        replayer.onFragment(buffer, START, fragmentLength(), null);

        verify(publication).tryClaim(anyInt(), eq(claim));

        assertSendsGapFillMessage(resultBuffer.capacity() - offset);
        verifyCommit();
    }

    // TODO: two admin messages
    // TODO: two admin messages followed by an application message
    // TODO: gapfill missing messages

    @Test
    public void shouldReplayMessageWithExpandingBodyLength()
    {
        bufferContainsMessage(MESSAGE_REQUIRING_LONGER_BODY_LENGTH);

        final int srcLength = fragmentLength();
        // Poss Dup Flag, and 1 longer body length
        final int newLength = srcLength + 6;
        setupMessage(newLength);

        replayer.onFragment(buffer, START, srcLength, null);

        verifyClaim(newLength);
        assertHasSetPossDupFlag();
        verifyCommit();
        hasNotOverwrittenSeperatorChar();

        assertEndsInValidChecksum(offset + 1);
    }

    @Test
    public void shouldPublishMessagesWithoutSetPossDupFlag()
    {
        bufferContainsExampleMessage(false);
        final int srcLength = fragmentLength();
        final int lengthAfterPossDupFlag = srcLength + POSS_DUP_FIELD.length;
        setupMessage(lengthAfterPossDupFlag);

        replayer.onFragment(buffer, START, srcLength, null);

        verifyClaim(lengthAfterPossDupFlag);
        assertHasSetPossDupFlag();
        verifyCommit();

        final int afterOffset = this.offset + 1;
        assertThat(resultAsciiBuffer,
            sequenceEqualsAscii("8=FIX.4.4\0019=68\001", afterOffset));

        assertThat(resultAsciiBuffer,
            sequenceEqualsAscii("8=FIX.4.4\0019=68\001", afterOffset));

        assertEndsInValidChecksum(afterOffset);
    }

    @Test
    public void shouldIgnoreIrrelevantFixMessages()
    {
        onMessage(LogonDecoder.MESSAGE_TYPE, buffer.capacity());

        verifyNoMoreInteractions(replayQuery, publication);
    }

    @Test
    public void shouldValidateResendRequestMessageSequenceNumbers()
    {
        final long result = bufferHasResendRequest(BEGIN_SEQ_NO - 1);
        onMessage(ResendRequestDecoder.MESSAGE_TYPE, result);

        verify(errorHandler).onError(any());
        verifyNoMoreInteractions(replayQuery, publication);
    }

    @After
    public void shouldHaveNoMoreErrors()
    {
        verifyNoMoreInteractions(errorHandler);
    }

    private void assertEndsInValidChecksum(final int afterOffset)
    {
        final String message = resultAsciiBuffer.getAscii(afterOffset, resultAsciiBuffer.capacity() - afterOffset);
        final Matcher matcher = Pattern.compile("10=\\d+\001").matcher(message);
        assertTrue(message, matcher.find());
    }

    private void hasNotOverwrittenSeperatorChar()
    {
        final String lengthSection = resultAsciiBuffer.getAscii(offset + 11, 11);
        assertEquals("9=104\00135=1\001", lengthSection);
    }

    private void assertSendsGapFillMessage(final int claimedLength)
    {
        final int offset = offset() + MESSAGE_FRAME_BLOCK_LENGTH;
        final int length = claimedLength - MESSAGE_FRAME_BLOCK_LENGTH;
        final String message = resultAsciiBuffer.getAscii(offset, length);
        final SequenceResetDecoder sequenceReset = new SequenceResetDecoder();
        sequenceReset.decode(resultAsciiBuffer, offset, length);
        final HeaderDecoder header = sequenceReset.header();

        if (!sequenceReset.validate())
        {
            fail(message + "\n" + sequenceReset.invalidTagId() + " " +
                RejectReason.decode(sequenceReset.rejectReason()));
        }

        assertTrue(message, sequenceReset.gapFillFlag());
        assertEquals(message, SEQUENCE_NUMBER, header.msgSeqNum());
        assertTrue(message, header.possDupFlag());
    }

    private void setupMessage(final int length)
    {
        setupClaim(length);
        setupPublication(length);
    }

    private void verifyQueriedService(final int endSeqNo)
    {
        verify(replayQuery).query(replayer, SESSION_ID, BEGIN_SEQ_NO, SEQUENCE_INDEX, endSeqNo, SEQUENCE_INDEX);
    }

    private void assertHasSetPossDupFlag()
    {
        final int possDupIndex = resultAsciiBuffer.scan(0, resultAsciiBuffer.capacity() - 1, 'Y');
        assertNotEquals("Unable to find poss dup index", UNKNOWN_INDEX, possDupIndex);
    }

    private void onMessage(final int messageType, final long result)
    {
        final int length = Encoder.length(result);
        final int offset = Encoder.offset(result);
        replayer.onMessage(
            buffer, offset, length,
            LIBRARY_ID, CONNECTION_ID, SESSION_ID, SEQUENCE_INDEX, messageType, 0L, OK, 0L);
    }

    private void bufferContainsMessage(final byte[] message)
    {
        logEntryLength = message.length;
        final MutableAsciiBuffer asciiBuffer = new MutableAsciiBuffer(message);
        bufferContainsMessage(SESSION_ID, SEQUENCE_NUMBER, asciiBuffer, ExampleMessageDecoder.MESSAGE_TYPE);
    }
}
