/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.wtvmaster.game;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java persistance
        import javax.persistence.*;
    // java io
        import java.io.*;
        import java.nio.*;
        import java.nio.channels.*;
    // wtvmaster
        import net.volcore.wtvmaster.user.*;

/*******************************************************************************
         In-memory representation of a game.
 *******************************************************************************/
public class Game
{
    static Logger                   logger = LoggerFactory.getLogger( "Game" );

    /** Persistent data. */
        /** Persistent, the Unique ID of the game. */
            private int     id;
        /** Data required by gamelist */
            /** Persistent, the current status of the game. */
                private byte    status = STATUS_NONE;
            /** Persistent, the date the game has been created. */
                private int     date;
            /** Persistent, the name of the game. */
                private String  name;
            /** Persistent, the name of the organisation. */
                private String  organisation;
            /** Persistent, is a certified game. */
                private boolean certified;

        /** Data required for gameinfo */
            /** Persistent, additional comments for the game, like players and matchup. */
                private String  comment;
            /** Persistent, name of the account that streamed the game. */
                private String  streamer;
            /** Persistent, the length of the game. */
                private int     gameLength;
            /** Persistent, the number of packets of the game. */
                private int     numPackets;

        /** Data required for game start */
            /** Persistent, the delay used for the game. */
                private int     delay;
            /** Persistent, the final random seed for the game. */
                private int     lastSeed;

        /** Repository information */
            /** Persistent, the location in the repository of the game. */
                private String  path;
            /** Persistent, the md5 checksum of the game info, used for finding duplicates. */ 
                private String  checksum;

    /** Public access to members. */
        /** Accessor. */
            /** Getter for id */
            	public int getId( ) { return id; }
            /** Setter for id */
            	public void setId( int id ) { this.id = id; }
            /** Getter for status */
            	public byte getStatus( ) { return status; }
            /** Setter for status */
            	public void setStatus( byte status ) { this.status = status; }
            /** Getter for date */
            	public int getDate( ) { return date; }
            /** Setter for date */
            	public void setDate( int date ) { this.date = date; }
            /** Getter for name */
            	public String getName( ) { return name; }
            /** Setter for name */
            	public void setName( String name ) { this.name = name; }
            /** Getter for comment */
            	public String getComment( ) { return comment; }
            /** Setter for comment */
            	public void setComment( String comment ) { this.comment = comment; }
            /** Getter for streamer */
            	public String getStreamer( ) { return streamer; }
            /** Setter for streamer */
            	public void setStreamer( String streamer ) { this.streamer = streamer; }
            /** Getter for gameLength */
            	public int getGameLength( ) { return gameLength; }
            /** Setter for gameLength */
            	public void setGameLength( int gameLength ) { this.gameLength = gameLength; }
            /** Getter for numPackets */
            	public int getNumPackets( ) { return numPackets; }
            /** Setter for numPackets */
            	public void setNumPackets( int numPackets ) { this.numPackets = numPackets; }
            /** Getter for delay */
            	public int getDelay( ) { return delay; }
            /** Setter for delay */
            	public void setDelay( int delay ) { this.delay = delay; }
            /** Getter for lastSeed */
            	public int getLastSeed( ) { return lastSeed; }
            /** Setter for lastSeed */
            	public void setLastSeed( int lastSeed ) { this.lastSeed = lastSeed; }
            /** Getter for path */
            	public String getPath( ) { return path; }
            /** Setter for path */
            	public void setPath( String path ) { this.path = path; }
            /** Getter for checksum */
            	public String getChecksum( ) { return checksum; }
            /** Setter for checksum */
            	public void setChecksum( String checksum ) { this.checksum = checksum; }
            /** Getter for organisation */
            	public String getOrganisation( ) { return organisation; }
            /** Setter for organisation */
            	public void setOrganisation( String organisation ) { this.organisation = organisation; }
            /** Getter for certified */
            	public boolean getCertified( ) { return certified; }
            /** Setter for certified */
            	public void setCertified( boolean certified ) { this.certified = certified; }


    /** Status codes. */
        /** The game has just been created, but not yet initialized. */
            public final static byte STATUS_NONE         = 0;
        /** The game has been created and initialized, though it has not started yet. */
            public final static byte STATUS_NEW          = 1;
        /** The game has been created, initialized and it is going to start soon. */
            public final static byte STATUS_DELAY        = 2;
        /** The game has been created, initialized and it has started. */
            public final static byte STATUS_STARTED      = 3;
        /** The game has been created, initialized and it has been finished. */
            public final static byte STATUS_COMPLETED    = 4;
        /** The game did not reach the finish state before the uploader disconnected or an error occured. */
            public final static byte STATUS_BROKEN       = 5;

        /** Return the HP related status. */
            public int getHpStatus( )
            {
                if( status < STATUS_COMPLETED ) return 0;
                if( status == STATUS_COMPLETED ) return 1;
                return 2;
            }

    /** Uploading information. */
        /** UserID of the current uploader. This field is used to keep track of who is currently uploading this game. */
            public int                      uploaderId = UserDatabase.INVALID_USERID;
        /** Current write position of the uploader. */
            public int                      uploadPosition = 0;

            /** Getter for uploaderId */
            	public int getUploaderId( ) { return uploaderId; }
            /** Setter for uploaderId */
            	public void setUploaderId( int uploaderId ) { this.uploaderId = uploaderId; }

    /** Cache and access statistics. */
        /** Last access. */
            protected long          lastAccessTime = System.currentTimeMillis( );

            /** Getter for lastAccessTime */
            	public long getLastAccessTime( ) { return lastAccessTime; }
            /** Setter for lastAccessTime */
            	public void setLastAccessTime( ) { this.lastAccessTime = System.currentTimeMillis( ); }

    /** File storage functionality. */
        /** Random access file tied to this game. */
            RandomAccessFile file = null;
        /** Truncates the file and fills it with info, details and an empty gamedata block */
            synchronized public boolean createOnDisk( byte[] gameInfo )
            {
                setLastAccessTime( );
                if( gameInfo.length > Repository.gameInfoLength )
                {
                    logger.error( "Oh my god, the game infos are larger than 1020 bytes. this must not happen!?" );
                    return false;
                }

                try {
                    // overwrite if there is an old file
                    file.setLength( 0 );
                    file.writeInt( Endian.swapInt( gameInfo.length ) );
                    file.write( gameInfo );
                    file.write( Repository.zeroPad, 0, Repository.gameInfoLength-gameInfo.length-4 );
                    // write game details stub
                    file.write( Repository.zeroPad, 0, Repository.gameDetailsLength );
                    // last packet size stub
                    file.writeInt( 0 );
                    return true;
                }
                catch( Exception e )
                {
                    logger.error( "Failed to create initialize game "+id+": "+e );
                    return false;
                }
            }

        /** Opens the file descriptor. */
            synchronized public void openFile( )
            {
                if( this.file != null ) 
                    return;

                setLastAccessTime( );

                try {
                    String filename = Repository.basePath + path;
                    new File( filename ).getParentFile( ).mkdirs( );
                    this.file = new RandomAccessFile( filename, "rw" );
                } catch( Exception e)
                {
                    logger.error( "Failed to create file for game "+id+": "+e );
                    e.printStackTrace( );
                    return;
                }
            }

        /** Closes the file descriptor. */
            synchronized public void closeFile( )
            {
                if( this.file == null ) 
                    return;

                setLastAccessTime( );

                try {
                    this.file.close( );
                    this.file = null;
                } catch( Exception e)
                {
                    logger.error( "Failed to close file for game "+id+": "+e );
                    e.printStackTrace( );
                    return;
                }
            }

        /** Fetches the last packet size from disk.
         *  @return -1 if an error occured, the size of the last packet otherwise. */
            synchronized public int getLastPacketSize( )
            {
                setLastAccessTime( );
                try {
                    file.seek( file.length( ) - 4 );
                    int value = Endian.swapInt( file.readInt( ) );
                    //logger.trace( "last packet size: "+value );
                    return value + 8;
                } catch( Exception e )
                {
                    logger.error( "Failed to fetch last packet size: "+e );
                    e.printStackTrace( );
                    return -1;
                }
            }

        /** Fetches the game data size.
         *  @return -1 if an error occured, the game data size otherwise */
            synchronized public int getGameDataSize( )
            {
                setLastAccessTime( );
                try {
                    int size = (int)file.length( ) - Repository.gameDataOffset - 4;
                    //logger.trace( "gamedata size: "+size );
                    return size;
                } catch( Exception e )
                {
                    logger.error( "Failed to fetch last packet size: "+e );
                    return -1;
                }
            }

        /** Fetch the game info from file */
            synchronized public byte[] getGameInfo( )
            {
                setLastAccessTime( );
                try {
                    file.seek( Repository.gameInfoOffset );
                    int size = Endian.swapInt( file.readInt( ) );
                    byte[] tmp = new byte[ size ];
                    file.readFully( tmp );
                    // VS: fix file info for ggc 1.24c
                    if(size>8)
                    {
                        ByteBuffer buf = ByteBuffer.wrap(tmp);
                        buf.order( ByteOrder.LITTLE_ENDIAN );
                        int gameTag = buf.getInt( );
                        int gameVersion = buf.getInt( );
                        if(gameVersion==3993)
                        {
                            logger.info("Fixing GGC 1.24c issue...");
                            buf.position(4);
                            buf.putInt(24);
                        }
                    }
                    return tmp;
                } catch( Exception e )
                {
                    logger.error( "Failed to get game info from file: "+e );
                    e.printStackTrace( );
                    return null;
                }
            }

        /** Fetch and parse the game info from file */
            public GameInfo getParsedGameInfo( )
            {
                byte[] data = getGameInfo( );

                if( data == null ) 
                    return null;

                return new GameInfo( data );
            }

        /** Fetch the game details from file */
            synchronized public byte[] getGameDetails( )
            {
                setLastAccessTime( );
                try {
                    file.seek( Repository.gameDetailsOffset );
                    int size = Endian.swapInt( file.readInt( ) );
                    byte[] tmp = new byte[ size ];
                    file.readFully( tmp );
                    return tmp;
                } catch( Exception e )
                {
                    logger.error( "Failed to get game details from file: "+e );
                    e.printStackTrace( );
                    return null;
                }
            }

        /** Fetch the game details from file */
            synchronized public byte[] getDataBlock( int offset )
            {
                setLastAccessTime( );
                try {
                    int realOffset = offset + Repository.gameDataOffset;
                    int length = 0;

                    file.seek( realOffset );

                    while( length < 30*1024 
                    && (file.length( ) - file.getFilePointer( ) - 4 ) >= 8 )
                    {
                        int time = Endian.swapInt( file.readInt( ) );
                        int size = Endian.swapInt( file.readInt( ) );

                        if( size < 0 )
                        {
                            logger.error( "Negative packet size in file! Critical!" );
                            logger.error( time+" "+size+" "+file.length( )+" "+file.getFilePointer( )+" "+length+" "+realOffset );
                            return null;
                        }

                        if( (file.length( ) - file.getFilePointer( ) - 4 ) < size )
                            break;

                        length += size+8;
                        file.skipBytes( size );
                    }
    
                    if( length == 0 )
                        return null;

                    file.seek( realOffset );
                    byte[] data = new byte[ length ];
                    file.readFully( data );
                    return data;
                } catch( Exception e )
                {
                    logger.error( "Failed to get game data from file: "+e );
                    e.printStackTrace( );
                    return null;
                }
            }

        /** Write the game details to disk. */
            synchronized public void writeGameDetails( byte[] details )
            {
                setLastAccessTime( );
                if( details.length > Repository.gameDetailsLength )
                {
                    logger.error( "Oh my god, the game details are larger than 2044 bytes. this must not happen!?" );
                    return;
                }

                try {
                    file.seek( Repository.gameDetailsOffset );
                    file.writeInt( Endian.swapInt( details.length ) );
                    file.write( details );
                } catch( Exception e )
                {
                    logger.error( "Failed to write game details: "+e );
                    e.printStackTrace( );
                }
            }

        /** Add data to the game. */
            synchronized public void addPacket( int time, int size, byte[] data )
            {
                setLastAccessTime( );
                try {
                    file.seek( file.length( ) - 4 );
                    file.writeInt( Endian.swapInt( time ) );
                    file.writeInt( Endian.swapInt( size ) );
                    file.write( data );
                    file.writeInt( Endian.swapInt( size ) );
                    numPackets++;
                    gameLength = time;
                } catch( Exception e )
                {
                    logger.error( "Failed to write game data: "+e );
                    e.printStackTrace( );
                }
            }

        /** Truncate the game to a specific position after a resume. */
            synchronized public void truncate( int offset, int lastsize )
            {
                setLastAccessTime( );
                try {
                    int realOffset = offset + Repository.gameDataOffset;

                    if( file.length( ) == realOffset + 4 )
                    {
                        logger.trace( "Nothing to truncate, file has the correct length." );
                        return;
                    }

                    logger.trace( "Truncating file of game "+this );
                    file.setLength( realOffset + 4 );
                    file.seek( realOffset );
                    file.writeInt( Endian.swapInt( lastsize ) );

                    // recompute the number of packets (this is expensive! -_- )
                    numPackets = computeNumberOfPackets( );
                } catch( Exception e )
                {
                    logger.error( "Failed to write game data: "+e );
                    e.printStackTrace( );
                }
            }

        /** Validate the provided resume position */
            synchronized public boolean verifyResumeInfo( int offset, int lasttime, int lastsize, byte[] data )
            {
                setLastAccessTime( );
                try {
                    int realOffset = offset + Repository.gameDataOffset;
                    file.seek( realOffset-lastsize-8 );
                    int time = Endian.swapInt( file.readInt( ) );
                    int size = Endian.swapInt( file.readInt( ) );

                    logger.trace( "Resume info:  "+time+"/"+size+" vs "+lasttime+"/"+lastsize );

                    if( time != lasttime 
                    ||  size != lastsize )
                    {
                        logger.error( "Verifying resume position: time/size doesn't match!" );
                        return false;
                    }
                    
                    byte[] data2 = new byte[ data.length ];

                    file.readFully( data2 );

                    for( int i=0; i<data.length; ++i )
                        if( data[i] != data2[i] )
                        {
                            logger.error( "Verifying resume position: data doesn't match!" );
                            return false;
                        }

                    return true;
                } catch( Exception e )
                {
                    logger.error( "Failed to verify game data: "+e );
                    e.printStackTrace( );
                    return false;
                }
            }

        /** Compares the provided game info with the game info on disk, to see if its a match. */
            synchronized public boolean isSameGame( byte[] gameInfo )
            {
                setLastAccessTime( );
                byte[] tmp = null;

                try {
                    file.seek( Repository.gameInfoOffset );
                    int size = Endian.swapInt( file.readInt( ) );
                    tmp = new byte[ size ];
                    file.readFully( tmp );
                } catch( Exception e )
                {
                    logger.error( "Failed to compare game info: "+e );
                    return false;
                }

                if( tmp.length != gameInfo.length )
                    return false;

                for( int i=0; i<tmp.length; ++i )
                    if( tmp[i] != gameInfo[i] )
                        return false;

                return true;
            }

    /** Auxiliary functions. */
        /** recomputes the number of packets of the game from disk. this is expensive, but required sometimes. */
            synchronized public int computeNumberOfPackets( )
            {
                int numPackets = 0;
                try {
                    int realOffset = Repository.gameDataOffset;
                    file.seek( realOffset );

                    while( (file.length( ) - file.getFilePointer( ) - 4 ) >= 8 )
                    {
                        int time = Endian.swapInt( file.readInt( ) );
                        int size = Endian.swapInt( file.readInt( ) );

                        if( size < 0 )
                        {
                            logger.error( "Negative packet size in file! Critical!" );
                            logger.error( time+" "+size+" "+file.length( )+" "+file.getFilePointer( )+" "+realOffset );
                            return -1;
                        }

                        if( (file.length( ) - file.getFilePointer( ) - 4 ) < size )
                            break;

                        file.skipBytes( size );
                        numPackets++;
                    }

                    return numPackets;
                } catch( Exception e )
                {
                    logger.error( "Failed to get game data from file: "+e );
                    e.printStackTrace( );
                    return -1;
                }
            }

        /** Is the game finished. 
          * @return true if the game is either completed or broken, false else */
            public boolean isFinished(  )
            {
                if( status == STATUS_COMPLETED
                ||  status == STATUS_BROKEN )
                    return true;
                return false;
            }

        /** toString function, used for debugging purposes */
            public String toString( )
            {
                return "(Game: "+id+" "+status+" "+date+" "+name+" "+comment+" "+streamer+" "+gameLength+" "+numPackets+" "+delay+" "+lastSeed+")";
            }

        /** Returns the age of the game. */
            public int getAge( )
            {
                return (int)(System.currentTimeMillis( )/1000)-date;
            }

    /** Public static functionality. */
        /** Compute the checksum of a gameinfo block. */
            static public String computeGameInfoChecksum( byte[] gameInfo )
            {
                return Checksum.getMD5String( gameInfo );
            }
}
