/*
 * Copyright 2019-2021 The Polypheny Project
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
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.rex;


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.calcite.avatica.util.ByteString;
import org.junit.Test;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeFactory;
import org.polypheny.db.algebra.type.AlgDataTypeSystem;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.PolyTypeFactoryImpl;
import org.polypheny.db.util.Collation;
import org.polypheny.db.util.DateString;
import org.polypheny.db.util.NlsString;
import org.polypheny.db.util.TimeString;
import org.polypheny.db.util.TimestampString;
import org.polypheny.db.util.TimestampWithTimeZoneString;
import org.polypheny.db.util.Util;


/**
 * Test for {@link RexBuilder}.
 */
public class RexBuilderTest {

    /**
     * Test RexBuilder.ensureType()
     */
    @Test
    public void testEnsureTypeWithAny() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        RexBuilder builder = new RexBuilder( typeFactory );

        RexNode node = new RexLiteral( Boolean.TRUE, typeFactory.createPolyType( PolyType.BOOLEAN ), PolyType.BOOLEAN );
        RexNode ensuredNode = builder.ensureType( typeFactory.createPolyType( PolyType.ANY ), node, true );

        assertEquals( node, ensuredNode );
    }


    /**
     * Test RexBuilder.ensureType()
     */
    @Test
    public void testEnsureTypeWithItself() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        RexBuilder builder = new RexBuilder( typeFactory );

        RexNode node = new RexLiteral( Boolean.TRUE, typeFactory.createPolyType( PolyType.BOOLEAN ), PolyType.BOOLEAN );
        RexNode ensuredNode = builder.ensureType( typeFactory.createPolyType( PolyType.BOOLEAN ), node, true );

        assertEquals( node, ensuredNode );
    }


    private static final long MOON = -14159025000L;

    private static final int MOON_DAY = -164;

    private static final int MOON_TIME = 10575000;


    /**
     * Tests {@link RexBuilder#makeTimestampLiteral(TimestampString, int)}.
     */
    @Test
    public void testTimestampLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        final AlgDataType timestampType = typeFactory.createPolyType( PolyType.TIMESTAMP );
        final AlgDataType timestampType3 = typeFactory.createPolyType( PolyType.TIMESTAMP, 3 );
        final AlgDataType timestampType9 = typeFactory.createPolyType( PolyType.TIMESTAMP, 9 );
        final AlgDataType timestampType18 = typeFactory.createPolyType( PolyType.TIMESTAMP, 18 );
        final RexBuilder builder = new RexBuilder( typeFactory );

        // Old way: provide a Calendar
        final Calendar calendar = Util.calendar();
        calendar.set( 1969, Calendar.JULY, 21, 2, 56, 15 ); // one small step
        calendar.set( Calendar.MILLISECOND, 0 );
        checkTimestamp( builder.makeLiteral( calendar, timestampType, false ) );

        // Old way #2: Provide a Long
        checkTimestamp( builder.makeLiteral( MOON, timestampType, false ) );

        // The new way
        final TimestampString ts = new TimestampString( 1969, 7, 21, 2, 56, 15 );
        checkTimestamp( builder.makeLiteral( ts, timestampType, false ) );

        // Now with milliseconds
        final TimestampString ts2 = ts.withMillis( 56 );
        assertThat( ts2.toString(), is( "1969-07-21 02:56:15.056" ) );
        final RexNode literal2 = builder.makeLiteral( ts2, timestampType3, false );
        assertThat( ((RexLiteral) literal2).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15.056" ) );

        // Now with nanoseconds
        final TimestampString ts3 = ts.withNanos( 56 );
        final RexNode literal3 = builder.makeLiteral( ts3, timestampType9, false );
        assertThat( ((RexLiteral) literal3).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15" ) );
        final TimestampString ts3b = ts.withNanos( 2345678 );
        final RexNode literal3b = builder.makeLiteral( ts3b, timestampType9, false );
        assertThat( ((RexLiteral) literal3b).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15.002" ) );

        // Now with a very long fraction
        final TimestampString ts4 = ts.withFraction( "102030405060708090102" );
        final RexNode literal4 = builder.makeLiteral( ts4, timestampType18, false );
        assertThat( ((RexLiteral) literal4).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15.102" ) );

        // toString
        assertThat( ts2.round( 1 ).toString(), is( "1969-07-21 02:56:15" ) );
        assertThat( ts2.round( 2 ).toString(), is( "1969-07-21 02:56:15.05" ) );
        assertThat( ts2.round( 3 ).toString(), is( "1969-07-21 02:56:15.056" ) );
        assertThat( ts2.round( 4 ).toString(), is( "1969-07-21 02:56:15.056" ) );

        assertThat( ts2.toString( 6 ), is( "1969-07-21 02:56:15.056000" ) );
        assertThat( ts2.toString( 1 ), is( "1969-07-21 02:56:15.0" ) );
        assertThat( ts2.toString( 0 ), is( "1969-07-21 02:56:15" ) );

        assertThat( ts2.round( 0 ).toString(), is( "1969-07-21 02:56:15" ) );
        assertThat( ts2.round( 0 ).toString( 0 ), is( "1969-07-21 02:56:15" ) );
        assertThat( ts2.round( 0 ).toString( 1 ), is( "1969-07-21 02:56:15.0" ) );
        assertThat( ts2.round( 0 ).toString( 2 ), is( "1969-07-21 02:56:15.00" ) );

        assertThat( TimestampString.fromMillisSinceEpoch( 1456513560123L ).toString(), is( "2016-02-26 19:06:00.123" ) );
    }


    private void checkTimestamp( RexNode node ) {
        assertThat( node.toString(), is( "1969-07-21 02:56:15" ) );
        RexLiteral literal = (RexLiteral) node;
        assertThat( literal.getValue() instanceof Calendar, is( true ) );
        assertThat( literal.getValue2() instanceof Long, is( true ) );
        assertThat( literal.getValue3() instanceof Long, is( true ) );
        assertThat( (Long) literal.getValue2(), is( MOON ) );
        assertThat( literal.getValueAs( Calendar.class ), notNullValue() );
        assertThat( literal.getValueAs( TimestampString.class ), notNullValue() );
    }


    /**
     * Tests
     * {@link RexBuilder#makeTimestampWithLocalTimeZoneLiteral(TimestampString, int)}.
     */
    @Test
    public void testTimestampWithLocalTimeZoneLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        final AlgDataType timestampType = typeFactory.createPolyType( PolyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE );
        final AlgDataType timestampType3 = typeFactory.createPolyType( PolyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE, 3 );
        final AlgDataType timestampType9 = typeFactory.createPolyType( PolyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE, 9 );
        final AlgDataType timestampType18 = typeFactory.createPolyType( PolyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE, 18 );
        final RexBuilder builder = new RexBuilder( typeFactory );

        // The new way
        final TimestampWithTimeZoneString ts = new TimestampWithTimeZoneString( 1969, 7, 21, 2, 56, 15, TimeZone.getTimeZone( "PST" ).getID() );
        checkTimestampWithLocalTimeZone( builder.makeLiteral( ts.getLocalTimestampString(), timestampType, false ) );

        // Now with milliseconds
        final TimestampWithTimeZoneString ts2 = ts.withMillis( 56 );
        assertThat( ts2.toString(), is( "1969-07-21 02:56:15.056 PST" ) );
        final RexNode literal2 = builder.makeLiteral( ts2.getLocalTimestampString(), timestampType3, false );
        assertThat( ((RexLiteral) literal2).getValue().toString(), is( "1969-07-21 02:56:15.056" ) );

        // Now with nanoseconds
        final TimestampWithTimeZoneString ts3 = ts.withNanos( 56 );
        final RexNode literal3 = builder.makeLiteral( ts3.getLocalTimestampString(), timestampType9, false );
        assertThat( ((RexLiteral) literal3).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15" ) );
        final TimestampWithTimeZoneString ts3b = ts.withNanos( 2345678 );
        final RexNode literal3b = builder.makeLiteral( ts3b.getLocalTimestampString(), timestampType9, false );
        assertThat( ((RexLiteral) literal3b).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15.002" ) );

        // Now with a very long fraction
        final TimestampWithTimeZoneString ts4 = ts.withFraction( "102030405060708090102" );
        final RexNode literal4 = builder.makeLiteral( ts4.getLocalTimestampString(), timestampType18, false );
        assertThat( ((RexLiteral) literal4).getValueAs( TimestampString.class ).toString(), is( "1969-07-21 02:56:15.102" ) );

        // toString
        assertThat( ts2.round( 1 ).toString(), is( "1969-07-21 02:56:15 PST" ) );
        assertThat( ts2.round( 2 ).toString(), is( "1969-07-21 02:56:15.05 PST" ) );
        assertThat( ts2.round( 3 ).toString(), is( "1969-07-21 02:56:15.056 PST" ) );
        assertThat( ts2.round( 4 ).toString(), is( "1969-07-21 02:56:15.056 PST" ) );

        assertThat( ts2.toString( 6 ), is( "1969-07-21 02:56:15.056000 PST" ) );
        assertThat( ts2.toString( 1 ), is( "1969-07-21 02:56:15.0 PST" ) );
        assertThat( ts2.toString( 0 ), is( "1969-07-21 02:56:15 PST" ) );

        assertThat( ts2.round( 0 ).toString(), is( "1969-07-21 02:56:15 PST" ) );
        assertThat( ts2.round( 0 ).toString( 0 ), is( "1969-07-21 02:56:15 PST" ) );
        assertThat( ts2.round( 0 ).toString( 1 ), is( "1969-07-21 02:56:15.0 PST" ) );
        assertThat( ts2.round( 0 ).toString( 2 ), is( "1969-07-21 02:56:15.00 PST" ) );
    }


    private void checkTimestampWithLocalTimeZone( RexNode node ) {
        assertThat( node.toString(), is( "1969-07-21 02:56:15:TIMESTAMP_WITH_LOCAL_TIME_ZONE(0)" ) );
        RexLiteral literal = (RexLiteral) node;
        assertThat( literal.getValue() instanceof TimestampString, is( true ) );
        assertThat( literal.getValue2() instanceof Long, is( true ) );
        assertThat( literal.getValue3() instanceof Long, is( true ) );
    }


    /**
     * Tests {@link RexBuilder#makeTimeLiteral(TimeString, int)}.
     */
    @Test
    public void testTimeLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        AlgDataType timeType = typeFactory.createPolyType( PolyType.TIME );
        final AlgDataType timeType3 = typeFactory.createPolyType( PolyType.TIME, 3 );
        final AlgDataType timeType9 = typeFactory.createPolyType( PolyType.TIME, 9 );
        final AlgDataType timeType18 = typeFactory.createPolyType( PolyType.TIME, 18 );
        final RexBuilder builder = new RexBuilder( typeFactory );

        // Old way: provide a Calendar
        final Calendar calendar = Util.calendar();
        calendar.set( 1969, Calendar.JULY, 21, 2, 56, 15 ); // one small step
        calendar.set( Calendar.MILLISECOND, 0 );
        checkTime( builder.makeLiteral( calendar, timeType, false ) );

        // Old way #2: Provide a Long
        checkTime( builder.makeLiteral( MOON_TIME, timeType, false ) );

        // The new way
        final TimeString t = new TimeString( 2, 56, 15 );
        assertThat( t.getMillisOfDay(), is( 10575000 ) );
        checkTime( builder.makeLiteral( t, timeType, false ) );

        // Now with milliseconds
        final TimeString t2 = t.withMillis( 56 );
        assertThat( t2.getMillisOfDay(), is( 10575056 ) );
        assertThat( t2.toString(), is( "02:56:15.056" ) );
        final RexNode literal2 = builder.makeLiteral( t2, timeType3, false );
        assertThat( ((RexLiteral) literal2).getValueAs( TimeString.class ).toString(), is( "02:56:15.056" ) );

        // Now with nanoseconds
        final TimeString t3 = t.withNanos( 2345678 );
        assertThat( t3.getMillisOfDay(), is( 10575002 ) );
        final RexNode literal3 = builder.makeLiteral( t3, timeType9, false );
        assertThat( ((RexLiteral) literal3).getValueAs( TimeString.class ).toString(), is( "02:56:15.002" ) );

        // Now with a very long fraction
        final TimeString t4 = t.withFraction( "102030405060708090102" );
        assertThat( t4.getMillisOfDay(), is( 10575102 ) );
        final RexNode literal4 = builder.makeLiteral( t4, timeType18, false );
        assertThat( ((RexLiteral) literal4).getValueAs( TimeString.class ).toString(), is( "02:56:15.102" ) );

        // toString
        assertThat( t2.round( 1 ).toString(), is( "02:56:15" ) );
        assertThat( t2.round( 2 ).toString(), is( "02:56:15.05" ) );
        assertThat( t2.round( 3 ).toString(), is( "02:56:15.056" ) );
        assertThat( t2.round( 4 ).toString(), is( "02:56:15.056" ) );

        assertThat( t2.toString( 6 ), is( "02:56:15.056000" ) );
        assertThat( t2.toString( 1 ), is( "02:56:15.0" ) );
        assertThat( t2.toString( 0 ), is( "02:56:15" ) );

        assertThat( t2.round( 0 ).toString(), is( "02:56:15" ) );
        assertThat( t2.round( 0 ).toString( 0 ), is( "02:56:15" ) );
        assertThat( t2.round( 0 ).toString( 1 ), is( "02:56:15.0" ) );
        assertThat( t2.round( 0 ).toString( 2 ), is( "02:56:15.00" ) );

        assertThat( TimeString.fromMillisOfDay( 53560123 ).toString(), is( "14:52:40.123" ) );
    }


    private void checkTime( RexNode node ) {
        assertThat( node.toString(), is( "02:56:15" ) );
        RexLiteral literal = (RexLiteral) node;
        assertThat( literal.getValue() instanceof Calendar, is( true ) );
        assertThat( literal.getValue2() instanceof Integer, is( true ) );
        assertThat( literal.getValue3() instanceof Integer, is( true ) );
        assertThat( (Integer) literal.getValue2(), is( MOON_TIME ) );
        assertThat( literal.getValueAs( Calendar.class ), notNullValue() );
        assertThat( literal.getValueAs( TimeString.class ), notNullValue() );
    }


    /**
     * Tests {@link RexBuilder#makeDateLiteral(DateString)}.
     */
    @Test
    public void testDateLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        AlgDataType dateType = typeFactory.createPolyType( PolyType.DATE );
        final RexBuilder builder = new RexBuilder( typeFactory );

        // Old way: provide a Calendar
        final Calendar calendar = Util.calendar();
        calendar.set( 1969, Calendar.JULY, 21 ); // one small step
        calendar.set( Calendar.MILLISECOND, 0 );
        checkDate( builder.makeLiteral( calendar, dateType, false ) );

        // Old way #2: Provide in Integer
        checkDate( builder.makeLiteral( MOON_DAY, dateType, false ) );

        // The new way
        final DateString d = new DateString( 1969, 7, 21 );
        checkDate( builder.makeLiteral( d, dateType, false ) );
    }


    private void checkDate( RexNode node ) {
        assertThat( node.toString(), is( "1969-07-21" ) );
        RexLiteral literal = (RexLiteral) node;
        assertThat( literal.getValue() instanceof Calendar, is( true ) );
        assertThat( literal.getValue2() instanceof Integer, is( true ) );
        assertThat( literal.getValue3() instanceof Integer, is( true ) );
        assertThat( (Integer) literal.getValue2(), is( MOON_DAY ) );
        assertThat( literal.getValueAs( Calendar.class ), notNullValue() );
        assertThat( literal.getValueAs( DateString.class ), notNullValue() );
    }


    /**
     * Test case for "AssertionError in {@link RexLiteral#getValue3} with null literal of type DECIMAL".
     */
    @Test
    public void testDecimalLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        final AlgDataType type = typeFactory.createPolyType( PolyType.DECIMAL );
        final RexBuilder builder = new RexBuilder( typeFactory );
        final RexLiteral literal = builder.makeExactLiteral( null, type );
        assertThat( literal.getValue3(), nullValue() );
    }


    /**
     * Tests {@link DateString} year range.
     */
    @Test
    public void testDateStringYearError() {
        try {
            final DateString dateString = new DateString( 11969, 7, 21 );
            fail( "expected exception, got " + dateString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Year out of range: [11969]" ) );
        }
        try {
            final DateString dateString = new DateString( "12345-01-23" );
            fail( "expected exception, got " + dateString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Invalid date format: [12345-01-23]" ) );
        }
    }


    /**
     * Tests {@link DateString} month range.
     */
    @Test
    public void testDateStringMonthError() {
        try {
            final DateString dateString = new DateString( 1969, 27, 21 );
            fail( "expected exception, got " + dateString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Month out of range: [27]" ) );
        }
        try {
            final DateString dateString = new DateString( "1234-13-02" );
            fail( "expected exception, got " + dateString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Month out of range: [13]" ) );
        }
    }


    /**
     * Tests {@link DateString} day range.
     */
    @Test
    public void testDateStringDayError() {
        try {
            final DateString dateString = new DateString( 1969, 7, 41 );
            fail( "expected exception, got " + dateString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Day out of range: [41]" ) );
        }
        try {
            final DateString dateString = new DateString( "1234-01-32" );
            fail( "expected exception, got " + dateString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Day out of range: [32]" ) );
        }
        // We don't worry about the number of days in a month. 30 is in range.
        final DateString dateString = new DateString( "1234-02-30" );
        assertThat( dateString, notNullValue() );
    }


    /**
     * Tests {@link TimeString} hour range.
     */
    @Test
    public void testTimeStringHourError() {
        try {
            final TimeString timeString = new TimeString( 111, 34, 56 );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Hour out of range: [111]" ) );
        }
        try {
            final TimeString timeString = new TimeString( "24:00:00" );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Hour out of range: [24]" ) );
        }
        try {
            final TimeString timeString = new TimeString( "24:00" );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat(
                    e.getMessage(),
                    containsString( "Invalid time format: [24:00]" ) );
        }
    }


    /**
     * Tests {@link TimeString} minute range.
     */
    @Test
    public void testTimeStringMinuteError() {
        try {
            final TimeString timeString = new TimeString( 12, 334, 56 );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Minute out of range: [334]" ) );
        }
        try {
            final TimeString timeString = new TimeString( "12:60:23" );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Minute out of range: [60]" ) );
        }
    }


    /**
     * Tests {@link TimeString} second range.
     */
    @Test
    public void testTimeStringSecondError() {
        try {
            final TimeString timeString = new TimeString( 12, 34, 567 );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Second out of range: [567]" ) );
        }
        try {
            final TimeString timeString = new TimeString( 12, 34, -4 );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Second out of range: [-4]" ) );
        }
        try {
            final TimeString timeString = new TimeString( "12:34:60" );
            fail( "expected exception, got " + timeString );
        } catch ( IllegalArgumentException e ) {
            assertThat( e.getMessage(), containsString( "Second out of range: [60]" ) );
        }
    }


    /**
     * Test string literal encoding.
     */
    @Test
    public void testStringLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        final AlgDataType varchar = typeFactory.createPolyType( PolyType.VARCHAR );
        final RexBuilder builder = new RexBuilder( typeFactory );

        final NlsString latin1 = new NlsString( "foobar", "LATIN1", Collation.IMPLICIT );
        final NlsString utf8 = new NlsString( "foobar", "UTF8", Collation.IMPLICIT );

        RexNode literal = builder.makePreciseStringLiteral( "foobar" );
        assertEquals( "'foobar'", literal.toString() );
        literal = builder.makePreciseStringLiteral( new ByteString( new byte[]{ 'f', 'o', 'o', 'b', 'a', 'r' } ), "UTF8", Collation.IMPLICIT );
        assertEquals( "_UTF8'foobar'", literal.toString() );
        assertEquals( "_UTF8'foobar':CHAR(6) CHARACTER SET \"UTF-8\"", ((RexLiteral) literal).computeDigest( RexDigestIncludeType.ALWAYS ) );
        literal = builder.makePreciseStringLiteral(
                new ByteString( "\u82f1\u56fd".getBytes( StandardCharsets.UTF_8 ) ),
                "UTF8",
                Collation.IMPLICIT );
        assertEquals( "_UTF8'\u82f1\u56fd'", literal.toString() );
        // Test again to check decode cache.
        literal = builder.makePreciseStringLiteral(
                new ByteString( "\u82f1".getBytes( StandardCharsets.UTF_8 ) ),
                "UTF8",
                Collation.IMPLICIT );
        assertEquals( "_UTF8'\u82f1'", literal.toString() );
        try {
            literal = builder.makePreciseStringLiteral(
                    new ByteString( "\u82f1\u56fd".getBytes( StandardCharsets.UTF_8 ) ),
                    "GB2312",
                    Collation.IMPLICIT );
            fail( "expected exception, got " + literal );
        } catch ( RuntimeException e ) {
            assertThat( e.getMessage(), containsString( "Failed to encode" ) );
        }
        literal = builder.makeLiteral( latin1, varchar, false );
        assertEquals( "_LATIN1'foobar'", literal.toString() );
        literal = builder.makeLiteral( utf8, varchar, false );
        assertEquals( "_UTF8'foobar'", literal.toString() );
    }


    /**
     * Tests {@link RexBuilder#makeExactLiteral(java.math.BigDecimal)}.
     */
    @Test
    public void testBigDecimalLiteral() {
        final AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );
        final RexBuilder builder = new RexBuilder( typeFactory );
        checkBigDecimalLiteral( builder, "25" );
        checkBigDecimalLiteral( builder, "9.9" );
        checkBigDecimalLiteral( builder, "0" );
        checkBigDecimalLiteral( builder, "-75.5" );
        checkBigDecimalLiteral( builder, "10000000" );
        checkBigDecimalLiteral( builder, "100000.111111111111111111" );
        checkBigDecimalLiteral( builder, "-100000.111111111111111111" );
        checkBigDecimalLiteral( builder, "73786976294838206464" ); // 2^66
        checkBigDecimalLiteral( builder, "-73786976294838206464" );
    }


    private void checkBigDecimalLiteral( RexBuilder builder, String val ) {
        final RexLiteral literal = builder.makeExactLiteral( new BigDecimal( val ) );
        assertThat( "builder.makeExactLiteral(new BigDecimal(" + val + ")).getValueAs(BigDecimal.class).toString()", literal.getValueAs( BigDecimal.class ).toString(), is( val ) );
    }

}
