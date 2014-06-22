/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvcache.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java 
        import java.nio.charset.*;
    // mina
        import org.apache.mina.core.buffer.*;

public class GameListEntry
{
    static Logger   logger = LoggerFactory.getLogger( "GameListEntry" );

    public int          gameId;
    public byte         status;
    public int          date;
    public String       name;

    /** Charset en- and decoder. */
        protected final static CharsetDecoder charsetDecoder = Charset.forName( "UTF-8" ).newDecoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );
        protected final static CharsetEncoder charsetEncoder = Charset.forName( "UTF-8" ).newEncoder( ).onMalformedInput( CodingErrorAction.REPLACE ).onUnmappableCharacter( CodingErrorAction.REPLACE );

    public GameListEntry( IoBuffer rbuf )
    {
        gameId = rbuf.getInt( );
        status = rbuf.get( );
        date = rbuf.getInt( );
        try {
            name = rbuf.getString( charsetDecoder );
        } catch( CharacterCodingException e )
        {
            logger.error( "Failed to read game list: "+e );
            e.printStackTrace( );
        }

        //logger.trace( "New game: "+gameId+" "+status+" "+date+" "+name );
    }

    public void write( IoBuffer rbuf )
    {
        rbuf.putInt( gameId );
        rbuf.put( status );
        rbuf.putInt( date );
        try {
            rbuf.putString( name, charsetEncoder );
        } catch( CharacterCodingException e )
        {
            logger.error( "Failed to write game list: "+e );
            e.printStackTrace( );
        }
        rbuf.put( (byte) 0 );
    }
}

