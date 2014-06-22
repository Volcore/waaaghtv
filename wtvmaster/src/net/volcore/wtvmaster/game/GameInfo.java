package net.volcore.wtvmaster.game;

import java.nio.*;
import java.nio.channels.*;

public class GameInfo
{
    public int gameTag;
    public int gameVersion;
    public int mapCheck;
    public String mapName;
    public GameInfo( byte[] data )
    {
        ByteBuffer buf = ByteBuffer.wrap( data );
        buf.order( ByteOrder.LITTLE_ENDIAN );
        gameTag = buf.getInt( );
        gameVersion = buf.getInt( );

        buf.getInt( );
        buf.getInt( );
        buf.getInt( );
        buf.getInt( );
        buf.getInt( );

        mapCheck = buf.getInt( );

        // read the map name
        StringBuffer sb = new StringBuffer( );
        char c = (char)buf.get( );
        while( c != 0 && buf.hasRemaining( ) )
        {
            sb.append( c );
            c = (char)buf.get( );
        }
        mapName = sb.toString( );
    }
}
