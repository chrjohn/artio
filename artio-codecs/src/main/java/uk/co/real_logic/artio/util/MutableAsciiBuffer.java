/*
 * Copyright 2015-2017 Real Logic Ltd.
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
package uk.co.real_logic.artio.util;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.artio.fields.*;

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.US_ASCII;

public final class MutableAsciiBuffer extends UnsafeBuffer implements AsciiBuffer
{
    private static final byte ZERO = '0';
    private static final byte DOT = (byte)'.';
    private static final byte SPACE = ' ';

    private static final byte Y = (byte)'Y';
    private static final byte N = (byte)'N';

    private static final byte[] MIN_INTEGER_VALUE = String.valueOf(Integer.MIN_VALUE).getBytes(US_ASCII);
    private static final byte[] MIN_LONG_VALUE = String.valueOf(Long.MIN_VALUE).getBytes(US_ASCII);

    public MutableAsciiBuffer()
    {
        super(0, 0);
    }

    public MutableAsciiBuffer(final byte[] buffer)
    {
        super(buffer);
    }

    public MutableAsciiBuffer(final byte[] buffer, final int offset, final int length)
    {
        super(buffer, offset, length);
    }

    public MutableAsciiBuffer(final ByteBuffer buffer)
    {
        super(buffer);
    }

    public MutableAsciiBuffer(final ByteBuffer buffer, final int offset, final int length)
    {
        super(buffer, offset, length);
    }

    public MutableAsciiBuffer(final DirectBuffer buffer)
    {
        super(buffer);
    }

    public MutableAsciiBuffer(final DirectBuffer buffer, final int offset, final int length)
    {
        super(buffer, offset, length);
    }

    public MutableAsciiBuffer(final long address, final int length)
    {
        super(address, length);
    }

    public int getNatural(final int startInclusive, final int endExclusive)
    {
        return super.parseNaturalIntAscii(startInclusive, endExclusive - startInclusive);
    }

    public long getNaturalLong(final int startInclusive, final int endExclusive)
    {
        return super.parseNaturalLongAscii(startInclusive, endExclusive - startInclusive);
    }

    @SuppressWarnings("FinalParameters")
    public int getInt(int startInclusive, final int endExclusive)
    {
        return super.parseIntAscii(startInclusive, endExclusive - startInclusive);
    }

    public int getDigit(final int index)
    {
        final byte value = getByte(index);
        return getDigit(index, value);
    }

    public boolean isDigit(final int index)
    {
        final byte value = getByte(index);
        return value >= 0x30 && value <= 0x39;
    }

    private int getDigit(final int index, final byte value)
    {
        if (value < 0x30 || value > 0x39)
        {
            throw new NumberFormatException("'" + ((char)value) + "' isn't a valid digit @ " + index);
        }

        return value - 0x30;
    }

    public char getChar(final int index)
    {
        return (char)getByte(index);
    }

    public boolean getBoolean(final int index)
    {
        return YES == getByte(index);
    }

    public byte[] getBytes(final byte[] oldBuffer, final int offset, final int length)
    {
        final byte[] resultBuffer = oldBuffer.length < length ? new byte[length] : oldBuffer;
        getBytes(offset, resultBuffer, 0, length);
        return resultBuffer;
    }

    public char[] getChars(final char[] oldBuffer, final int offset, final int length)
    {
        final char[] resultBuffer = oldBuffer.length < length ? new char[length] : oldBuffer;
        for (int i = 0; i < length; i++)
        {
            resultBuffer[i] = getChar(i + offset);
        }
        return resultBuffer;
    }

    /**
     * Not at all a performant conversion: don't use this on a critical application path.
     *
     * @param offset The offset within the buffer to start at.
     * @param length the length in bytes to convert to a String
     * @return a String
     */
    public String getAscii(final int offset, final int length)
    {
        final byte[] buff = new byte[length];
        getBytes(offset, buff);
        return new String(buff, 0, length, US_ASCII);
    }

    public int getMessageType(final int offset, final int length)
    {
        // message types can only be 1 or 2 bytes in size
        if (length == 1)
        {
            return getByte(offset);
        }
        else
        {
            return getShort(offset);
        }
    }

    @SuppressWarnings("FinalParameters")
    public DecimalFloat getFloat(final DecimalFloat number, int offset, int length)
    {
        // Throw away trailing spaces or zeros
        int end = offset + length;
        for (int index = end - 1; isSpace(index) && index > offset; index--)
        {
            end--;
        }

        int endDiff = 0;
        for (int index = end - 1; isZero(index) && index > offset; index--)
        {
            endDiff++;
        }

        boolean isFloatingPoint = false;
        for (int index = end - endDiff - 1; index > offset; index--)
        {
            if (getByte(index) == DOT)
            {
                isFloatingPoint = true;
                break;
            }
        }

        if (isFloatingPoint)
        {
            end -= endDiff;
        }

        // Throw away leading spaces
        for (int index = offset; isSpace(index) && index < end; index++)
        {
            offset++;
        }

        // Is it negative?
        final boolean negative = getByte(offset) == '-';
        if (negative)
        {
            offset++;
        }

        // Throw away leading zeros
        for (int index = offset; isZero(index) && index < end; index++)
        {
            offset++;
        }

        int scale = 0;
        long value = 0;
        for (int index = offset; index < end; index++)
        {
            final byte byteValue = getByte(index);
            if (byteValue == DOT)
            {
                // number of digits after the dot
                scale = end - (index + 1);
            }
            else
            {
                final int digit = getDigit(index, byteValue);
                value = value * 10 + digit;
            }
        }

        number.set(negative ? -1 * value : value, scale);
        return number;
    }

    private boolean isSpace(final int index)
    {
        return getByte(index) == SPACE;
    }

    private boolean isZero(final int index)
    {
        return getByte(index) == ZERO;
    }

    public int getLocalMktDate(final int offset, final int length)
    {
        return LocalMktDateDecoder.decode(this, offset, length);
    }

    public long getUtcTimestamp(final int offset, final int length)
    {
        return UtcTimestampDecoder.decode(this, offset, length);
    }

    public long getUtcTimeOnly(final int offset, final int length)
    {
        return UtcTimeOnlyDecoder.decode(this, offset, length);
    }

    public int getUtcDateOnly(final int offset)
    {
        return UtcDateOnlyDecoder.decode(this, offset);
    }

    public int scanBack(final int startInclusive, final int endExclusive, final char terminatingCharacter)
    {
        return scanBack(startInclusive, endExclusive, (byte)terminatingCharacter);
    }

    public int scanBack(final int startInclusive, final int endExclusive, final byte terminator)
    {
        for (int index = startInclusive; index >= endExclusive; index--)
        {
            final byte value = getByte(index);
            if (value == terminator)
            {
                return index;
            }
        }

        return UNKNOWN_INDEX;
    }

    public int scan(final int startInclusive, final int endInclusive, final char terminatingCharacter)
    {
        return scan(startInclusive, endInclusive, (byte)terminatingCharacter);
    }

    public int scan(final int startInclusive, final int endInclusive, final byte terminator)
    {
        int indexValue = UNKNOWN_INDEX;
        for (int i = startInclusive; i <= endInclusive; i++)
        {
            final byte value = getByte(i);
            if (value == terminator)
            {
                indexValue = i;
                break;
            }
        }

        return indexValue;
    }

    public int computeChecksum(final int offset, final int end)
    {
        int total = 0;
        for (int index = offset; index < end; index++)
        {
            total += (int)getByte(index);
        }

        return total % 256;
    }

    public int putAscii(final int index, final String string)
    {
        final byte[] bytes = string.getBytes(US_ASCII);
        putBytes(index, bytes);

        return bytes.length;
    }

    public void putSeparator(final int index)
    {
        putByte(index, SEPARATOR);
    }

    public int putBooleanAscii(final int offset, final boolean value)
    {
        putByte(offset, value ? Y : N);
        return 1;
    }

    public static int lengthInAscii(final int value)
    {
        int characterCount = 0;
        for (int remainder = value; remainder > 0; remainder = remainder / 10)
        {
            characterCount++;
        }
        return characterCount;
    }

    public int putCharAscii(final int index, final char value)
    {
        putByte(index, (byte)value);
        return 1;
    }

    /**
     *
     * @see Integer#DigitTens
     */
    static final byte[] INTEGER_DIGIT_TENS = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
        '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
        '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
        '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
        '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
        '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
        '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
        '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
        '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
        '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    };

    /**
     *
     * @see Integer#DigitOnes
     */
    static final byte[] INTEGER_DIGIT_ONES = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };

    /**
     * All possible chars for representing a number as a String
     *
     * @see Integer#digits
     */
    static final byte[] INTEGER_DIGITS = {
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z'
    };

    /**
     *
     * @param offset to 1 plus the right digit of given value
     * @param value entire value, without decimal point
     * @return offset to the left digit
     *
     * @see Long#getChars(long, int, char[])
     */
    private int handleDigits(final int offset, final long value)
    {
        long q;
        int r;
        int charPos = offset;
        long i = value;
        if (i < 0)
        {
            i = -i;
        }
        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE)
        {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            putByte(charPos--, INTEGER_DIGIT_ONES[r]);
            putByte(charPos--, INTEGER_DIGIT_TENS[r]);
        }
        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536)
        {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            putByte(charPos--, INTEGER_DIGIT_ONES[r]);
            putByte(charPos--, INTEGER_DIGIT_TENS[r]);
        }
        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;)
        {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            putByte(charPos--, INTEGER_DIGITS[r]);
            i2 = q2;
            if (i2 == 0)
            {
                break;
            }
        }
        return charPos + 1;
    }

    /**
     *
     * @param offset position in buffer to put at.
     * @param value significant digits.
     * @param scale how many spaces to shift the decimal point. (negative value will shift it to the right)
     * @return the number of bytes put to the buffer.
     *
     * for scale &gt; 0 :
     * @see java.math.BigDecimal#getValueString
     *
     * @see Long#getChars(long, int, char[])
     * @see java.math.BigDecimal#toPlainString
     *
     * Note: unlike putFloatAscii(offset, DecimalFloat), this method will respect the scale.
     * so for input of 0, -2 value returned will be "0.00" and not only "0".
     */
    public int putFloatAscii(final int offset, final long value, final int scale)
    {
//      final int rightDigitPosAtEnd = offset + Math.max(Math.abs(scale), LONGEST_LONG_LENGTH) + 1;
        final int rightDigitPosAtEnd = offset + LONGEST_LONG_LENGTH + 1;
        final int leftDigitPosAtEnd = handleDigits(rightDigitPosAtEnd, value);

        final int numDigits = rightDigitPosAtEnd - leftDigitPosAtEnd + 1;
        int charPos = offset;
        int lengthDigitsIncludingMinus = numDigits;
        if (value < 0)
        {
            lengthDigitsIncludingMinus++;
            putByte(charPos++, NEGATIVE);
        }
        if (scale <= 0)
        {
            putBytes(charPos, this, leftDigitPosAtEnd, numDigits);
            if (scale < 0)
            {
                charPos += numDigits;
                final int numberOfZeros = Math.abs(scale);
                for (int ix = 1; ix <= numberOfZeros; ix++)
                {
                    putByte(charPos++, ZERO);
                }
                return lengthDigitsIncludingMinus + numberOfZeros;
            }
            return lengthDigitsIncludingMinus;
        }
        else
        {   // scale > 0
            final int insertionPoint = numDigits - scale;
            if (insertionPoint == 0)
            {   /* Point goes right before digits */
                putByte(charPos++, ZERO);
                putByte(charPos++, DOT);
                putBytes(charPos, this, leftDigitPosAtEnd, numDigits);
                return 2 + lengthDigitsIncludingMinus;
            }
            else
            {
                if (insertionPoint > 0)
                {   /* Point goes inside intVal */
                    putBytes(charPos, this, leftDigitPosAtEnd, insertionPoint);
                    putByte(charPos + insertionPoint, DOT);
                    putBytes(charPos + insertionPoint + 1, this,
                        leftDigitPosAtEnd + insertionPoint, numDigits - insertionPoint);
                    return 1 + lengthDigitsIncludingMinus;
                }
                else
                {   /* We must insert zeros between point and intVal */
                    putByte(charPos++, ZERO);
                    putByte(charPos++, DOT);
                    final int numberOfZeros = Math.abs(insertionPoint);
                    for (int ix = 1; ix <= numberOfZeros; ix++)
                    {
                        putByte(charPos++, ZERO);
                    }
                    putBytes(charPos, this, leftDigitPosAtEnd, numDigits);
                    return 2 + numberOfZeros + lengthDigitsIncludingMinus;
                }
            }
        }
    }

    public int putFloatAscii(final int offset, final DecimalFloat price)
    {
        return putFloatAscii(offset, price.value(), price.scale());
    }

    private boolean zero(final int offset, final long value)
    {
        if (value == 0)
        {
            putByte(offset, ZERO);
            return true;
        }
        return false;
    }

    private long calculateRemainderAndPutMinus(final int offset, final long value)
    {
        if (value < 0)
        {
            putChar(offset, '-');
            return value;
        }
        else
        {
            // Deal with negatives to avoid overflow for Integer.MAX_VALUE
            return -1L * value;
        }
    }

    @SuppressWarnings("FinalParameters")
    private int putLong(long remainder, final int end)
    {
        int index = end;
        while (remainder < 0)
        {
            final long digit = remainder % 10;
            remainder = remainder / 10;
            putByte(index, (byte)(ZERO + (-1L * digit)));
            index--;
        }

        return index;
    }
}
