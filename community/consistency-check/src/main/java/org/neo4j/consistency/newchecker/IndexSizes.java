/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.consistency.newchecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.neo4j.common.EntityType;
import org.neo4j.consistency.checking.index.IndexAccessors;
import org.neo4j.consistency.newchecker.ParallelExecution.ThrowingRunnable;
import org.neo4j.internal.helpers.collection.BoundedIterable;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.kernel.api.index.IndexAccessor;

/**
 * Calculates index sizes in parallel and caches the sizes
 */
class IndexSizes
{
    private static final double SMALL_INDEX_FACTOR_THRESHOLD = 0.05;

    private final ParallelExecution execution;
    private final IndexAccessors indexAccessors;
    private final ConcurrentMap<IndexDescriptor,Long> nodeIndexSizes = new ConcurrentHashMap<>();
    private final ConcurrentMap<IndexDescriptor,Long> relationshipIndexSizes = new ConcurrentHashMap<>();
    private final long highNodeId;

    IndexSizes( ParallelExecution execution, IndexAccessors indexAccessors, long highNodeId )
    {
        this.execution = execution;
        this.indexAccessors = indexAccessors;
        this.highNodeId = highNodeId;
    }

    void initialize() throws Exception
    {
        calculateSizes( execution, indexAccessors, EntityType.NODE, nodeIndexSizes );
        calculateSizes( execution, indexAccessors, EntityType.RELATIONSHIP, relationshipIndexSizes );
    }

    private void calculateSizes( ParallelExecution execution, IndexAccessors indexAccessors, EntityType entityType,
            ConcurrentMap<IndexDescriptor,Long> indexSizes ) throws Exception
    {
        List<IndexDescriptor> indexes = indexAccessors.onlineRules( entityType );
        execution.run( "Estimate index sizes", indexes.stream().map( index -> (ThrowingRunnable) () ->
        {
            IndexAccessor accessor = indexAccessors.accessorFor( index );
            try ( BoundedIterable<Long> reader = accessor.newAllEntriesReader() )
            {
                indexSizes.put( index, reader.maxCount() );
            }
        } ).toArray( ThrowingRunnable[]::new ) );
    }

    private List<IndexDescriptor> getAllIndexes( EntityType entityType )
    {
        return new ArrayList<>( indexAccessors.onlineRules( entityType ) );
    }

    List<IndexDescriptor> smallIndexes( EntityType entityType )
    {
        List<IndexDescriptor> smallIndexes = getAllIndexes( entityType );
        smallIndexes.removeAll( largeIndexes( entityType ) );
        return smallIndexes;
    }

    List<IndexDescriptor> largeIndexes( EntityType entityType )
    {
        List<IndexDescriptor> indexes = getAllIndexes( entityType );
        indexes.sort( Comparator.comparingLong( this::getEstimatedIndexSize ).reversed() );
        int threshold = 0;
        for ( IndexDescriptor index : indexes )
        {
            if ( getSizeFactor( index ) > SMALL_INDEX_FACTOR_THRESHOLD || threshold % IndexChecker.NUM_INDEXES_IN_CACHE != 0 )
            {
                threshold++;
            }
        }
        return indexes.subList( 0, threshold );
    }

    private double getSizeFactor( IndexDescriptor index )
    {
        return (double) getEstimatedIndexSize( index ) / highNodeId;
    }

    long getEstimatedIndexSize( IndexDescriptor index )
    {
        EntityType entityType = index.schema().entityType();
        ConcurrentMap<IndexDescriptor,Long> map = entityType == EntityType.NODE ? nodeIndexSizes : relationshipIndexSizes;
        return map.get( index );
    }
}