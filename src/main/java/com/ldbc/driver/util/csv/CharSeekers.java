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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import static com.ldbc.driver.util.csv.BufferedCharSeeker.DEFAULT_BUFFER_SIZE;
import static com.ldbc.driver.util.csv.ThreadAheadReadable.threadAhead;

/**
* Factory for common {@link CharSeeker} implementations.
*/
public class CharSeekers {
    /**
     * Instantiates a {@link BufferedCharSeeker} with optional {@link ThreadAheadReadable read-ahead} capability.
     *
     * @param reader    the {@link Readable} which is the source of data, f.ex. a {@link FileReader}.
     * @param readAhead whether or not to start a {@link ThreadAheadReadable read-ahead thread}
     *                  which strives towards always keeping one buffer worth of data read and available from I/O when it's
     *                  time for the {@link BufferedCharSeeker} to read more data.
     * @return a {@link CharSeeker} with optional {@link ThreadAheadReadable read-ahead} capability.
     */
    public static CharSeeker charSeeker(Readable reader, boolean readAhead) throws FileNotFoundException {
        if (readAhead) {   // Thread that always has one buffer read ahead
            reader = threadAhead(reader, DEFAULT_BUFFER_SIZE);
        }

        // Give the reader to the char seeker
        return new BufferedCharSeeker(reader, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Instantiates a default {@link CharSeeker} capable of reading data in the specified {@code file}.
     *
     * @param file {@link File} to read data from.
     * @return {@link CharSeeker} reading and parsing data from {@code file}.
     * @throws FileNotFoundException if the specified {@code file} doesn't exist.
     */
    public static CharSeeker charSeeker(File file) throws FileNotFoundException {
        return charSeeker(new FileReader(file), true);
    }
}