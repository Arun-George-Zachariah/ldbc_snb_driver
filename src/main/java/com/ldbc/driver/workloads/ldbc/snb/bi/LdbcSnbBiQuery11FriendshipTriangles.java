package com.ldbc.driver.workloads.ldbc.snb.bi;

import com.google.common.collect.ImmutableMap;
import com.ldbc.driver.Operation;
import com.ldbc.driver.SerializingMarshallingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LdbcSnbBiQuery11FriendshipTriangles extends Operation<LdbcSnbBiQuery11FriendshipTrianglesResult>
{
    public static final int TYPE = 11;
    public static final String COUNTRY = "country";

    private final String country;

    public LdbcSnbBiQuery11FriendshipTriangles( String country )
    {
        this.country = country;
    }

    public String country()
    {
        return country;
    }

    @Override
    public Map<String, Object> parameterMap() {
        return ImmutableMap.<String, Object>builder()
                .put(COUNTRY, country)
                .build();
    }

    @Override
    public String toString()
    {
        return "LdbcSnbBiQuery11FriendshipTriangles{" +
               "country='" + country + '\'' +
               '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        { return true; }
        if ( o == null || getClass() != o.getClass() )
        { return false; }

        LdbcSnbBiQuery11FriendshipTriangles that = (LdbcSnbBiQuery11FriendshipTriangles) o;

        return !(country != null ? !country.equals( that.country ) : that.country != null);

    }

    @Override
    public int hashCode()
    {
        return country != null ? country.hashCode() : 0;
    }

    @Override
    public LdbcSnbBiQuery11FriendshipTrianglesResult marshalResult( String serializedResults )
            throws SerializingMarshallingException
    {
        List<List<Object>> resultsAsList = SerializationUtil.marshalListOfLists( serializedResults );
        List<Object> row = resultsAsList.get( 0 );
        int count = ((Number) row.get( 0 )).intValue();
        return new LdbcSnbBiQuery11FriendshipTrianglesResult( count );
    }

    @Override
    public String serializeResult( Object resultsObject ) throws SerializingMarshallingException
    {
        LdbcSnbBiQuery11FriendshipTrianglesResult result = (LdbcSnbBiQuery11FriendshipTrianglesResult) resultsObject;
        List<List<Object>> resultsFields = new ArrayList<>();
        List<Object> resultFields = new ArrayList<>();
        resultFields.add( result.count() );
        resultsFields.add( resultFields );
        return SerializationUtil.toJson( resultsFields );
    }

    @Override
    public int type()
    {
        return TYPE;
    }
}
