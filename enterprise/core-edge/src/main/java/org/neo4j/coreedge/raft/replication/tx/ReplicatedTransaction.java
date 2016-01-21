/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.raft.replication.tx;

import java.util.Objects;

import org.neo4j.coreedge.raft.replication.session.GlobalSession;
import org.neo4j.coreedge.raft.replication.session.LocalOperationId;
import org.neo4j.coreedge.raft.replication.ReplicatedContent;

import static java.lang.String.format;

public class ReplicatedTransaction<MEMBER> implements ReplicatedContent
{
    private final byte[] txBytes;
    private final GlobalSession globalSession;
    private final LocalOperationId localOperationId;

    public ReplicatedTransaction( byte[] txBytes, GlobalSession globalSession, LocalOperationId localOperationId )
    {
        this.txBytes = txBytes;
        this.globalSession = globalSession;
        this.localOperationId = localOperationId;
    }

    public byte[] getTxBytes()
    {
        return txBytes;
    }

    public GlobalSession<MEMBER> globalSession()
    {
        return globalSession;
    }

    public LocalOperationId localOperationId()
    {
        return localOperationId;
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        ReplicatedTransaction that = (ReplicatedTransaction) o;
        return Objects.equals( globalSession, that.globalSession ) &&
                Objects.equals( localOperationId, that.localOperationId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( globalSession, localOperationId );
    }

    @Override
    public String toString()
    {
        return format( "ReplicatedTransaction{globalSession=%s, localOperationId=%s}", globalSession, localOperationId );
    }
}
