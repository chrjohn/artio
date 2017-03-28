package uk.co.real_logic.fix_gateway.engine.logger;

import uk.co.real_logic.fix_gateway.builder.HeaderEncoder;
import uk.co.real_logic.fix_gateway.builder.SequenceResetEncoder;
import uk.co.real_logic.fix_gateway.decoder.HeaderDecoder;
import uk.co.real_logic.fix_gateway.fields.UtcTimestampEncoder;
import uk.co.real_logic.fix_gateway.util.MutableAsciiBuffer;

class GapFillEncoder
{
    private static final int ENCODE_BUFFER_SIZE = 1024;

    private final SequenceResetEncoder sequenceResetEncoder = new SequenceResetEncoder();
    private final UtcTimestampEncoder timestampEncoder = new UtcTimestampEncoder();
    private final MutableAsciiBuffer buffer = new MutableAsciiBuffer(new byte[ENCODE_BUFFER_SIZE]);

    GapFillEncoder()
    {
        sequenceResetEncoder.header().possDupFlag(true);
        sequenceResetEncoder.gapFillFlag(true);
    }

    long encode(final HeaderDecoder reqHeader, final int beginSeqNo, final int endSeqNo)
    {
        final HeaderEncoder respHeader = sequenceResetEncoder.header();
        respHeader.targetCompID(reqHeader.senderCompID(), reqHeader.senderCompIDLength());
        respHeader.senderCompID(reqHeader.targetCompID(), reqHeader.targetCompIDLength());
        if (reqHeader.hasSenderLocationID())
        {
            respHeader.targetLocationID(reqHeader.senderLocationID(), reqHeader.senderLocationIDLength());
        }
        if (reqHeader.hasSenderSubID())
        {
            respHeader.targetSubID(reqHeader.senderSubID(), reqHeader.senderSubIDLength());
        }
        if (reqHeader.hasTargetLocationID())
        {
            respHeader.senderLocationID(reqHeader.targetLocationID(), reqHeader.targetLocationIDLength());
        }
        if (reqHeader.hasTargetSubID())
        {
            respHeader.senderSubID(reqHeader.targetSubID(), reqHeader.targetSubIDLength());
        }
        respHeader.sendingTime(timestampEncoder.buffer(), timestampEncoder.encode(System.currentTimeMillis()));
        respHeader.msgSeqNum(beginSeqNo);
        sequenceResetEncoder.newSeqNo(endSeqNo);

        return sequenceResetEncoder.encode(buffer, 0);
    }

    MutableAsciiBuffer buffer()
    {
        return buffer;
    }
}
