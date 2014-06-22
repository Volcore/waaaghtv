/** Copyright (C) 2008 Volker Schönefeld. See the copyright notice in the LICENSE file. */
package net.volcore.wtvmina;

//imports
    // slf4j
        import org.slf4j.*;
    // volcore libs
        import net.volcore.util.*;
    // mina
        import org.apache.mina.core.buffer.*;
        import org.apache.mina.core.session.*;
        import org.apache.mina.filter.codec.*;


public class WC3CodecFactory implements ProtocolCodecFactory
{
    static Logger   logger = LoggerFactory.getLogger( "WC3CodecFactory" );

    protected ProtocolEncoder wc3ProtocolEncoder = new WC3ProtocolEncoder( );
    protected ProtocolDecoder wc3ProtocolDecoder = new WC3ProtocolDecoder( );

    public ProtocolEncoder getEncoder( IoSession ioSession ) throws Exception
    {
        return wc3ProtocolEncoder;
    }

    public ProtocolDecoder getDecoder( IoSession ioSession ) throws Exception
    {
        return wc3ProtocolDecoder;
    }
}
