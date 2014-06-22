/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.util;

// imports
    // java
        import java.nio.*;
        import java.util.*;

public class HexDump
{
    /** Converts a bytebuffer to a hex string */
	    public final static String bytebufferToHexString( ByteBuffer array )
	    {
	    	int size = array.remaining( );
            StringBuffer s = new StringBuffer();
	    	int p = 0;
            s.append( "ByteBuffer with " ).append( " length " ).append( array.remaining( ) ).append( ". Relevant data dump:\n" );

	    	while(p<size)
	    	{
	    		s.append( printHex(p)+"  " );

	    		for(int i=0;i<8;++i)
	    			if(p+i>=size) s.append( "   " );
	    			else        s.append( printHex(array.get(p+i))+" " );

	    		if(p+8<size) s.append("- ");
	    		else s.append("  ");

	    		for(int i=8;i<16;++i)
	    			if(p+i>=size) s.append("   ");
	    			else        s.append( printHex(array.get(p+i))+" ");

	    		s.append(" ");

	    		for(int i=0;i<16;++i)
	    			if(p+i>=size) break;
	    			else        
                        if( Text.isPrintable( (char) array.get(p+i) ) )
	    					s.append((char)array.get(p+i));
	    				else
	    					s.append(".");

	    		s.append("\n");
	    		p+= 16;
	    	}

	    	return s.toString();
	    }

    /** Convert an array to a hex string */
    	public static String arrayToHexString(byte[] array)
    	{
    		int size = array.length;
            StringBuffer s = new StringBuffer();
    		int p = 0;
    
    		while(p<size)
    		{
    			s.append(printHex(p)+"  ");
    
    			for(int i=0;i<8;++i)
    				if(p+i>=size) s.append("   ");
    				else        s.append(printHex(array[p+i])+" ");
    
    			if(p+8<size) s.append("- ");
    			else s.append("  ");
    
    			for(int i=8;i<16;++i)
    				if(p+i>=size) s.append("   ");
    				else        s.append(printHex(array[p+i])+" ");
    
    			s.append(" ");
    
    			for(int i=0;i<16;++i)
    				if(p+i>=size) break;
    				else        
                        if( Text.isPrintable( (char) array[p+i] ) )
    						s.append((char)array[p+i]);
    					else
    						s.append(".");
    
    			s.append("\n");
    			p+= 16;
    		}
    
    		return s.toString();
    	}

    /** Generate a sequence of random hex strings. */
        public static String generateRandomHexString( int chars )
        {
            StringBuffer s = new StringBuffer( );

            Random rng = new Random( );

            for( int i=0; i<chars; ++i )
                s.append( printHex( (byte) rng.nextInt( ) ) );

            return s.toString( );
        }

    /** Convert a byte to hex string. */
        public static String printHex( byte input )
        {
    	    return Integer.toHexString((input&0xf0)>>4)
    			+  Integer.toHexString((input&0x0f)>>0);
        }

    /** Convert an integer to hex string. */
        public static String printHex(int s)
        {
        	byte  d = (byte)(s&0xff);
        	byte  c = (byte)((s>>8)&0xff);
        	byte  b = (byte)((s>>16)&0xff);
        	byte  a = (byte)((s>>24)&0xff);
    
        	return  printHex(a)+printHex(b)+printHex(c)+printHex(d); 
        }

    /** Converts a sliced array to an hex string. */
        public final static String slicedArrayToHexString( SlicedArray array )
        {
        	int size = array.length;
            StringBuffer s = new StringBuffer();
        	int p = 0;
            s.append( "SlicedArray with offset " ).append( array.offset ).append( " length " ).append( array.length ).append( ". Relevant data dump:\n" );
    
        	while(p<size)
        	{
        		s.append( printHex(p)+"  " );
    
        		for(int i=0;i<8;++i)
        			if(p+i>=size) s.append( "   " );
        			else        s.append( printHex(array.get(p+i))+" " );
    
        		if(p+8<size) s.append("- ");
        		else s.append("  ");
    
        		for(int i=8;i<16;++i)
        			if(p+i>=size) s.append("   ");
        			else        s.append(printHex(array.get(p+i))+" ");
    
        		s.append(" ");
    
        		for(int i=0;i<16;++i)
        			if(p+i>=size) break;
        			else        
                        if( Text.isPrintable( (char) array.get(p+i) ) )
        					s.append((char)array.get(p+i));
        				else
        					s.append(".");
    
        		s.append("\n");
        		p+= 16;
        	}
    
        	return s.toString();
        }
}
