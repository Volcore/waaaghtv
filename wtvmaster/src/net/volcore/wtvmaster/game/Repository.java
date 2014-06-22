/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;

/*******************************************************************************
         The repository is the on disk storage for games.

        The on-disk representation looks like this:<p>

            4 bytes             (int)       game info length<p>
            1020 bytes          (byte[])    game infos and zero pad<p>
            4 bytes             (int)       game details length<p>
            2044 bytes          (byte[])    game details and zero pad<p>
            n-4 bytes           (byte[])    game data<p>
                4 bytes         (int)       game data block time<p>
                4 bytes         (int)       game data block size<p>
                ? bytes         (byte[])    game data block of size [game data block size]<p>
            4 bytes             (int)       last packet size<p>
 *******************************************************************************/
public class Repository
{
    /** Static file storage information. */
        /** File storage prime, hashing with which determines the storage location. */
            public final static int         PRIME1 = 23;
            public final static int         PRIME2 = 29;
            public final static int         PRIME3 = 31;
            public final static int         PRIME4 = 37;
        /** Base path of the repository. */
            public static String            basePath = "repository/";
        /** Total static length of the game info block. */
            public static final int         gameInfoLength = 1024;
        /** Offset of the game info block. */
            public static final int         gameInfoOffset = 0;
        /** Total static length of the game details block. */
            public static final int         gameDetailsLength = 2048;
        /** Offset of the game details block. */
            public static final int         gameDetailsOffset = gameInfoOffset+gameInfoLength;
        /** Offset of the game data block. */
            public static final int         gameDataOffset = gameDetailsOffset+gameDetailsLength;
        /** Auxiliary array used to fill up unused space with zeros. */
            public static final byte[]      zeroPad = new byte[ gameDetailsLength ];

        /** Compute the location of the file. */
            public static String computePath( int id )
            {
                int hash1 = id%PRIME1;
                int hash2 = id%PRIME2;
                int hash3 = id%PRIME3;
                int hash4 = id%PRIME4;

                StringBuilder pathBuilder = new StringBuilder( );
                pathBuilder.append( hash1 );
                pathBuilder.append( "/" );
                pathBuilder.append( hash2 );
                pathBuilder.append( "/" );
                pathBuilder.append( hash3 );
                pathBuilder.append( "/" );
                pathBuilder.append( hash4 );
                pathBuilder.append( "/" );
                pathBuilder.append( id );
                return pathBuilder.toString( );
            }
}
