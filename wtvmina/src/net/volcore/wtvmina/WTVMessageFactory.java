/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
        import net.volcore.wtvmina.messages.*;
    // mina
        import org.apache.mina.core.buffer.*;


/*******************************************************************************
         This factory can be used to parse WTV messages.
 *******************************************************************************/
public class WTVMessageFactory
{
    /** Message codes. */
        public static final byte LOGON_VERSIONREQUEST       = 0x00;
        public static final byte LOGON_VERSIONREQUESTREPLY  = 0x00;
        public static final byte LOGON_AUTHORIZE            = 0x01;
        public static final byte LOGON_AUTHORIZEREPLY       = 0x01;

        public static final byte RECORDER_GETSERVERDETAILS  = 0x10;
        public static final byte RECORDER_SERVERDETAILS     = 0x10;
        public static final byte RECORDER_CREATEGAME        = 0x11;
        public static final byte RECORDER_GAMECREATED       = 0x11;
        public static final byte RECORDER_DETAILUPDATE      = 0x12;
        public static final byte RECORDER_GAMESTART         = 0x13;
        public static final byte RECORDER_UPDATEGAMEDATA    = 0x14;
        public static final byte RECORDER_STOPGAMEDATA      = 0x15;
        public static final byte RECORDER_RESUMEGAMEDATA    = 0x16;
        public static final byte RECORDER_RESUMINGGAMEDATA  = 0x16;
        public static final byte RECORDER_GAMEFINISH        = 0x17;
        public static final byte RECORDER_GAMEFINISHREPLY   = 0x17;

        public static final byte RELAY_GETGAMELIST          = 0x20;
        public static final byte RELAY_GAMELIST             = 0x20;
        public static final byte RELAY_GETGAMEINFO          = 0x21;
        public static final byte RELAY_GAMEINFO             = 0x21;
        public static final byte RELAY_SUBSCRIBEGAME        = 0x22;
        public static final byte RELAY_SUBSCRIBEGAMEREPLY   = 0x22;
        public static final byte RELAY_UNSUBSCRIBEGAME      = 0x23;
        public static final byte RELAY_UNSUBSCRIBEGAMEREPLY = 0x23;
        public static final byte RELAY_GAMEDETAILUPDATE     = 0x24;
        public static final byte RELAY_GAMESTART            = 0x25;
        public static final byte RELAY_UPDATEGAMEDATA       = 0x26;
        public static final byte RELAY_UPDATEGAMEDATAREPLY  = 0x26;
        public static final byte RELAY_GAMEFINISH           = 0x27;
        public static final byte RELAY_PING                 = 0x28;
        public static final byte RELAY_PONG                 = 0x28;

        public static final byte CACHE_HACK                 = (byte)0xee;
        public static final byte GENERIC_ERROR              = (byte)0xf2;

    static Logger   logger = LoggerFactory.getLogger( "WTVMessageFactory" );

    public static WTVMessage parse( int type, IoBuffer in, boolean client )
    {
        try {
            if( client )
                switch( type )
                {
                case LOGON_VERSIONREQUESTREPLY: return new LogonRequestReply( in );
                case LOGON_AUTHORIZEREPLY:      return new LogonAuthorizeReply( in );
                case RECORDER_SERVERDETAILS:    return new RecorderServerDetails( in );
                case RECORDER_GAMECREATED:      return new RecorderGameCreated( in );
                case RECORDER_RESUMINGGAMEDATA: return new RecorderResumingGameData( in );
                case RECORDER_GAMEFINISHREPLY:  return new RecorderGameFinishReply( in );
                case RELAY_GAMELIST:            return new RelayGamelist( in );
                case RELAY_GAMEINFO:            return new RelayGameInfo( in );
                case RELAY_PING:                return new RelayPing( in );
                case RELAY_SUBSCRIBEGAMEREPLY:  return new RelaySubscribeGameReply( in );
                case RELAY_GAMEDETAILUPDATE:    return new RelayGameDetailUpdate( in );
                case RELAY_GAMESTART:           return new RelayGameStart( in );
                case RELAY_UPDATEGAMEDATA:      return new RelayUpdateGameData( in );
                case RELAY_GAMEFINISH:          return new RelayGameFinish( in );
                case RELAY_UNSUBSCRIBEGAMEREPLY:return new RelayUnsubscribeGameReply( in );
                case GENERIC_ERROR:             return new GenericError( in );
                }
            else
                switch( type )
                {
                case LOGON_VERSIONREQUEST:      return new LogonRequest( in );
                case LOGON_AUTHORIZE:           return new LogonAuthorize( in );
                case RECORDER_GETSERVERDETAILS: return new RecorderGetServerDetails( in );
                case RECORDER_CREATEGAME:       return new RecorderCreateGame( in );
                case RECORDER_DETAILUPDATE:     return new RecorderDetailUpdate( in );
                case RECORDER_GAMESTART:        return new RecorderGameStart( in );
                case RECORDER_UPDATEGAMEDATA:   return new RecorderUpdateGameData( in );
                case RECORDER_STOPGAMEDATA:     return new RecorderStopGameData( in );
                case RECORDER_RESUMEGAMEDATA:   return new RecorderResumeGameData( in );
                case RECORDER_GAMEFINISH:       return new RecorderGameFinish( in );
                case RELAY_GETGAMELIST:         return new RelayGetGamelist( in );
                case RELAY_GETGAMEINFO:         return new RelayGetGameInfo( in );
                case RELAY_PONG:                return new RelayPong( in );
                case RELAY_SUBSCRIBEGAME:       return new RelaySubscribeGame( in );
                case RELAY_UPDATEGAMEDATAREPLY: return new RelayUpdateGameDataReply( in );
                case RELAY_UNSUBSCRIBEGAME:     return new RelayUnsubscribeGame( in );
                case GENERIC_ERROR:             return new GenericError( in );
                case CACHE_HACK:                return new CacheHack( in );
                }

            throw new ParseException( "Unknown packet type: "+type );
        }
        catch( ParseException e )
        {
            logger.error( "Failed to parse wtv message: "+e );
            e.printStackTrace( );
            return null;
        }
    }
}

