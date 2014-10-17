package com.ldbc.driver.util.csv;

/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Seeks for specific characters in a stream of characters, e.g. a {@link Reader}. Uses a {@link Mark}
 * as keeper of position. Once a {@link #seek(Mark, int[])} has succeeded the characters specified by
 * the mark can be {@link #extract(Mark, Extractor) extracted} into a value of an arbitrary type.
 * <p/>
 * Typical usage is:
 * <p/>
 * <pre>
 * CharSeeker seeker = ...
 * Mark mark = new Mark();
 * int[] delimiters = new int[] {'\t',','};
 *
 * while ( seeker.seek( mark, delimiters ) )
 * {
 *     String value = seeker.extract( mark, Extractors.STRING );
 *     // ... somehow manage the value
 *     if ( mark.isEndOfLine() )
 *     {
 *         // ... end of line, put some logic to handle that here
 *     }
 * }
 * </pre>
 * <p/>
 * The {@link Reader} that gets passed in will be closed in {@link #close()}.
 *
 * @author Mattias Persson
 */
public interface CharSeeker extends Closeable {
    boolean seek(Mark mark, int[] untilOneOfChars) throws IOException;

    <T> T extract(Mark mark, Extractor<T> extractor);
}