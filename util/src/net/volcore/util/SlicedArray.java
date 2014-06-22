/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.util;

// imports
    // java std
        import java.nio.*;

/*******************************************************************************
         Wrapper holding an sliced array, with functionality to further slice
        it. Also provides convenience conversions to bytebuffers, byte[].
 *******************************************************************************/
public class SlicedArray
{
    byte[]      data;
    int         offset;
    int         length;

    public SlicedArray( byte[] data, int offset, int length )
    {
        this.data = data;
        this.offset = offset;
        this.length = length;
    }

    public ByteBuffer getByteBuffer( ) 
    {
        return ByteBuffer.wrap( data, offset, length );
    }


    /** Getter for data */
    	public byte[] getData( ) { return data; }
    /** Setter for data */
    	public void setData( byte[] data ) { this.data = data; }
    /** Getter for offset */
    	public int getOffset( ) { return offset; }
    /** Setter for offset */
    	public void setOffset( int offset ) { this.offset = offset; }
    /** Getter for length */
    	public int getLength( ) { return length; }
    /** Setter for length */
    	public void setLength( int length ) { this.length = length; }

    public byte get( int index )
    {
        if( index > length )
            throw new ArrayIndexOutOfBoundsException( );

        return this.data[ offset+index ];
    }

    /** make a copy of the contents of the sliced array, putting them into a new array. */
    public byte[] extract( )
    {
        byte [] array = new byte[length];
        System.arraycopy( data, offset, array, 0, length );
        return array;
    }

    /** extract a sub-slice from this array */
    public SlicedArray slice( int from, int to )
    {
        if( offset+to > length 
        ||  to < from )
            throw new ArrayIndexOutOfBoundsException( );

        return new SlicedArray( data, offset+from, to-from );
    }

    /** increases the pointer of this array */
    public void advance( int dist )
    {
        if( dist > length )
            throw new ArrayIndexOutOfBoundsException( );

        offset += dist;
        length -= dist;
    }
}

