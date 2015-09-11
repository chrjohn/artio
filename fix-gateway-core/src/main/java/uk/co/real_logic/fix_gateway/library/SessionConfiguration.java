/*
 * Copyright 2015 Real Logic Ltd.
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
package uk.co.real_logic.fix_gateway.library;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable Configuration class.
 */
public final class SessionConfiguration
{
    public static final int DEFAULT_SESSION_BUFFER_SIZE = 8 * 1024;

    private final List<String> hosts;
    private final List<Integer> ports;
    private final String username;
    private final String password;
    private final String senderCompId;
    private final String targetCompId;
    private final String senderSubId;
    private final String senderLocationId;
    private final int bufferSize;
    private final boolean sequenceNumbersPersistent;
    private final int initialSequenceNumber;

    public static Builder builder()
    {
        return new Builder();
    }

    private SessionConfiguration(
        final List<String> hosts,
        final List<Integer> ports,
        final String username,
        final String password,
        final String senderCompId,
        final String senderSubId,
        final String senderLocationId,
        final String targetCompId,
        final int bufferSize,
        final boolean sequenceNumbersPersistent,
        final int initialSequenceNumber)
    {
        Objects.requireNonNull(hosts);
        Objects.requireNonNull(ports);
        Objects.requireNonNull(senderCompId);
        Objects.requireNonNull(senderSubId);
        Objects.requireNonNull(senderLocationId);
        Objects.requireNonNull(targetCompId);

        requireNonEmpty(hosts, "hosts");
        requireNonEmpty(ports, "ports");

        this.senderCompId = senderCompId;
        this.senderSubId = senderSubId;
        this.senderLocationId = senderLocationId;
        this.targetCompId = targetCompId;
        this.hosts = hosts;
        this.ports = ports;
        this.username = username;
        this.password = password;
        this.bufferSize = bufferSize;
        this.sequenceNumbersPersistent = sequenceNumbersPersistent;
        this.initialSequenceNumber = initialSequenceNumber;
    }

    private void requireNonEmpty(final List<?> values, final String name)
    {
        if (values.isEmpty())
        {
            throw new IllegalArgumentException(name + " is empty");
        }
    }

    public List<String> hosts()
    {
        return hosts;
    }

    public List<Integer> ports()
    {
        return ports;
    }

    public String username()
    {
        return username;
    }

    public String password()
    {
        return password;
    }

    public String senderCompId()
    {
        return senderCompId;
    }

    public String senderSubId()
    {
        return senderSubId;
    }

    public String senderLocationId()
    {
        return senderLocationId;
    }

    public String targetCompId()
    {
        return targetCompId;
    }

    public int bufferSize()
    {
        return bufferSize;
    }

    public boolean sequenceNumbersPersistent()
    {
        return sequenceNumbersPersistent;
    }

    public int initialSequenceNumber()
    {
        return initialSequenceNumber;
    }

    public static final class Builder
    {
        private String username;
        private String password;
        private List<String> hosts = new ArrayList<>();
        private List<Integer> ports = new ArrayList<>();
        private String senderCompId;
        private String targetCompId;
        private String senderSubId = "";
        private String senderLocationId = "";
        private int bufferSize = DEFAULT_SESSION_BUFFER_SIZE;
        private boolean sequenceNumbersPersistent = false;
        private int initialSequenceNumber = 1;

        private Builder()
        {
        }

        public Builder credentials(final String username, final String password)
        {
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder address(final String host, final int port)
        {
            hosts.add(host);
            ports.add(Integer.valueOf(port));
            return this;
        }

        public Builder senderCompId(final String senderCompId)
        {
            this.senderCompId = senderCompId;
            return this;
        }

        public Builder senderSubId(final String senderSubId)
        {
            this.senderSubId = senderSubId;
            return this;
        }

        public Builder senderLocationId(final String senderLocationId)
        {
            this.senderLocationId = senderLocationId;
            return this;
        }

        public Builder targetCompId(final String targetCompId)
        {
            this.targetCompId = targetCompId;
            return this;
        }

        public Builder bufferSize(final int bufferSize)
        {
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Set this flag if you want sequence numbers to persistent when you reconnect
         * to the acceptor.
         *
         * @param sequenceNumbersPersistent true to make sequence numbers persistent
         * @return this builder
         *
         * @see this#initialSequenceNumber(int)
         * @see LibraryConfiguration#acceptorSequenceNumbersResetUponReconnect(boolean)
         */
        public Builder sequenceNumbersPersistent(final boolean sequenceNumbersPersistent)
        {
            this.sequenceNumbersPersistent = sequenceNumbersPersistent;
            return this;
        }

        /**
         * Sets the initial sequence number that you use when connecting to an acceptor.
         *
         * @param initialSequenceNumber the msg sequence number to use when you send your logon message.
         * @return this builder
         *
         * @see this#sequenceNumbersPersistent(boolean)
         * @see LibraryConfiguration#acceptorSequenceNumbersResetUponReconnect(boolean)
         */
        public Builder initialSequenceNumber(final int initialSequenceNumber)
        {
            this.initialSequenceNumber = initialSequenceNumber;
            return this;
        }

        public SessionConfiguration build()
        {
            return new SessionConfiguration(hosts, ports, username, password, senderCompId, senderSubId,
                senderLocationId, targetCompId, bufferSize, sequenceNumbersPersistent, initialSequenceNumber);
        }
    }

}
