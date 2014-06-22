/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util;

//imports
    // slf4j
        import org.slf4j.*;
    // java
        import java.io.*;

public class OutputBitStream
{
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	DataOutputStream      dos = new DataOutputStream(bos);

	byte                  curByte = 0;
	byte                  writeBit = 0;  // current bit position in curByte

	public int getNumBytes()
	{
		return bos.size();
	}
	public int getNumBits()
	{
		return writeBit+getNumBytes()*8;
	}
	public int getCurrentBitPos()
	{
		return writeBit;
	}

	public void flush() throws IOException
	{
		if(writeBit>0)
			writeBits(0,8 - writeBit);
	}

	public byte[] getData() throws IOException 
	{
		flush();
		return bos.toByteArray();
	}

	public void writeBits(int value, int numbits) throws IOException
	{
		int fraction = 0;
		int put = 0;

		while(numbits>0)
		{
			put = 8 - writeBit;

			if(put>numbits)
				put = numbits;

			fraction = value & ( ( 1<<put) -1 );
			curByte |= fraction << writeBit;
			numbits -= put;
			value >>= put;
			writeBit= (byte)((writeBit + put) & 7);

			if(writeBit==0)
			{
				dos.writeByte(curByte);
				curByte = 0;
			}
		}
	}
	public void writeLongBits(long value, int numbits) throws IOException
	{
		long fraction = 0;
		int put = 0;

		while(numbits>0)
		{
			put = 8 - writeBit;

			if(put>numbits)
				put = numbits;

			fraction = value & ( ( 1<<put) -1 );
			curByte |= fraction << writeBit;
			numbits -= put;
			value >>= put;
			writeBit= (byte)((writeBit + put) & 7);

			if(writeBit==0)
			{
				dos.writeByte(curByte);
				curByte = 0;
			}
		}
	}

	public void writeUTF8String(String str) throws IOException
	{
		byte[] data = str.getBytes("UTF-8");

		writeData(data);
		writeBits(0,8); // terminate
	}

	public void writeString(String str) throws IOException
	{
		byte[] data = str.getBytes();

		writeData(data);
		writeBits(0,8); // terminate
	}
	public void writeStringHuffman(String str) throws IOException
	{
		// todo: implement huffman tree/table here
		byte[] data = str.getBytes();

		writeData(data); 
		writeBits(0,8); // terminate
	}

	public void writeData(byte[] data) throws IOException
	{
		for(int i=0;i<data.length;++i)
			writeBits(data[i],8);
	}
	public void writeData(byte[] data,int off,int len) throws IOException
	{
		for(int i=off;i<off+len;++i)
			writeBits(data[i],8);
	}

	void writeDelta(int oldv, int newv,int numbits) throws IOException
	{
		if(oldv==newv)
			writeBits(0,1);
		else
		{
			writeBits(1,1);
			writeBits(newv,numbits);
		}
	}
	void writeDeltaLongCounter(int oldv, int newv) throws IOException
	{
		int x,i;
		x = oldv^newv;
		for(i=31;i>0;i--)
			if(0!=(x&(1<<i)))
			{
				i++;
				break;
			}
		writeBits(i,5);
		if(i!=0)
			writeBits(newv,i);
	}
}
