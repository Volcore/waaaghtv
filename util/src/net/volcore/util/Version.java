/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // java
        import java.nio.*;
        import java.io.*;

public class Version
{
	public int   major;
	public int   minor;
	public int   release;
	final static int majorBits = 4;
	final static int minorBits = 6;
	final static int releaseBits = 6;
           
    public Version(int _major,int _minor,int _release)
    {      
		major =  _major;
		minor =  _minor;
		release =  _release;
    }
	public Version(ByteBuffer stream)
	{
		release = (int)stream.get();
		minor = (int)stream.getShort();
		major = (int)stream.get();
	}
	

    public int toOrdinal( )
    {
        return release + (minor<<(releaseBits)) + (major<<(releaseBits+minorBits));
    }

	// bitstream functions
	public Version(InputBitStream ibs)
	{
		major = ibs.readBits(majorBits);
		minor = ibs.readBits(minorBits);
		release = ibs.readBits(releaseBits);
	}
	public void read(InputBitStream ibs)  throws IOException
	{	
		major = ibs.readBits(majorBits);
		minor = ibs.readBits(minorBits);
		release = ibs.readBits(releaseBits);
	}
	public void write(OutputBitStream obs) throws IOException 
	{
		obs.writeBits(major,majorBits);
		obs.writeBits(minor,minorBits);
		obs.writeBits(release,releaseBits);
	}

	public void write(ByteBuffer stream)
	{
		stream.put((byte)release);
		stream.putShort((short)minor);
		stream.put((byte)major);
	}

	public void read(ByteBuffer stream)  throws IOException
	{
		release = (int)stream.get();
		minor = (int)stream.getShort();
		major = (int)stream.get();
	}
	public String toString()
	{
		StringBuffer str = new StringBuffer(10);
		str.append(Integer.toString(major));
		str.append('.');
		str.append(Integer.toString(minor));
		str.append('.');
		str.append(Integer.toString(release));
		return str.toString();
	}
} 
