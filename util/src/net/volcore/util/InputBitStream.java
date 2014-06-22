/** Copyright (C) 2008 Volker Schönefeld. All rights reserved. See the LICENSE file for more information. */
package net.volcore.util;

//imports
    // slf4j
        import org.slf4j.*;
    // java
        import java.nio.*;
        import java.io.*;

public class InputBitStream
{
	ByteBuffer            buf = null;
	int                   curByte = 0;
	int                   readBit = 0;  // current bit position in curByte
	int                   numRead = 0;
	boolean               nextbyte = false;

	public InputBitStream(ByteBuffer input) { init(input); }
	public void init(ByteBuffer input) { buf = input; numRead = 0; readBit = 0; curByte = 0; nextbyte = true; }

	public int getNumBytesRead()
	{
		return numRead;
	}
	public int getNumBitsRead()
	{
		return readBit+getNumBytesRead()*8;
	}
	public int getCurrentBitPos()
	{
		return readBit;
	}
	public int getRemainingNumBits()
	{
		if(nextbyte==false)
			return buf.remaining()*8+8-readBit;
		else
			return buf.remaining()*8;
	}
	public int getRemainingNumBytes()
	{
		//if(nextbyte==false)
			return buf.remaining();
		//else
		//	return buf.remaining();
	}
	public boolean hasRemainingBits()
	{
		if(nextbyte==false)
			return buf.hasRemaining() || 0<(8-readBit);
		else
			return buf.hasRemaining();
			
	}

	public int readBits(int numbits) throws BufferUnderflowException
	{
		int fraction = 0;
		int get = 0;
		int value = 0;
		int valuebits = 0;

		while(numbits>valuebits)
		{
			if(nextbyte)
			{
				curByte = (int)buf.get();
				numRead++;
				nextbyte = false;
			}

			get = 8 - readBit;

			if(get>numbits-valuebits)
				get = numbits-valuebits;

			fraction = curByte >> readBit;
			fraction &=  (1<<get)-1;
			value |= fraction << valuebits;
			valuebits += get;
			readBit = (readBit + get) & 7;

			if(readBit==0)
				nextbyte = true;
		}

		return value;
	}

	public long readLongBits(int numbits) throws BufferUnderflowException
	{
		long fraction = 0;
		int get = 0;
		long value = 0;
		int valuebits = 0;

		while(numbits>valuebits)
		{
			if(nextbyte)
			{
				curByte = (int)buf.get();
				numRead++;
				nextbyte = false;
			}

			get = 8 - readBit;

			if(get>numbits-valuebits)
				get = numbits-valuebits;

			fraction = curByte >> readBit;
			fraction &=  (1<<get)-1;
			value |= fraction << valuebits;
			valuebits += get;
			readBit = (readBit + get) & 7;

			if(readBit==0)
				nextbyte = true;
		}

		return value;
	}

	public int readDeltaLongCounter(int oldv)
	{
		int i,newv;

		i = readBits(5);
		if(i==0) return oldv;
		newv = readBits(i);

		return (oldv&~((1<<i)-1)|newv);
	}

	public int readDelta(int oldv,int numbits)
	{
		if(readBits(1)!=0)
			return readBits( numbits );
		else
			return oldv;
	}

	public void readData(byte[] data, int bytes)
	{
		for(int i=0;i<bytes;++i)
			data[i] = (byte)readBits(8);
	}

	public void readData(byte[] data, int bytes, int offset)
	{
		for(int i=0;i<bytes;++i)
			data[offset+i] = (byte)readBits(8);
	}


	public void readByteAlign()
	{
		if(readBit>0)
		{
			readBit=0;
			nextbyte = true;
		}
	}
	
	public String readStringNoAlign()
	{
		int n = 0;
		StringBuffer s = new StringBuffer();
		while(true)
		{
			int c = readBits(8);
			if(c<=0 || c>=255)
				break;
			s.append((char)c);
			++n;
		}

		if(n==0)
			return "";
		else 
			return s.toString();
	}
	public String readString()
	{
		readByteAlign();
		return readStringNoAlign();
	}
	public String readUTF8String()
	{
		readByteAlign();
    
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	DataOutputStream      dos = new DataOutputStream(bos);

        String s="<unsupported encoding or read error>";
        try{
        // read until \0
		int n = 0;
		while(true)
		{
			int c = readBits(8);
			if(c<=0 || c>=255)
				break;
            dos.writeByte((byte)c);
			++n;
		}

        s = new String(bos.toByteArray(),"UTF-8");
        } catch(Exception e){}

		return s;
	}


	public void readDict()
	{
		String key = "",value = "";
		
		System.out.println("changed keys:");
		while((key = readString()).equals("")==false)
		{
			value = readString();
			System.out.println(key+" -> "+value);
		}

		System.out.println("deleted keys:");
		while((key = readString()).equals("")==false)
			System.out.println(key);
		

	}
}
