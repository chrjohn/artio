/*
 * Copyright 2015-2020 Real Logic Limited.
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
package uk.co.real_logic.artio.sbe_util;

import org.agrona.DirectBuffer;
import uk.co.real_logic.sbe.json.JsonPrinter;

import java.nio.ByteBuffer;

public class MessageDumper
{
    public static String print(
        final JsonPrinter dumper, final DirectBuffer buffer, final int offset, final int length)
    {
        final ByteBuffer byteBuffer = buffer.byteBuffer();
        final int originalPosition = byteBuffer.position();
        final int originalLimit = byteBuffer.limit();
        byteBuffer.limit(length + offset).position(offset);
        final ByteBuffer slice = byteBuffer.slice();
        byteBuffer.limit(originalLimit).position(originalPosition);

        return dumper.print(slice);
    }
}
