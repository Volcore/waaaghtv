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


public class WTVCodecFactory implements ProtocolCodecFactory
{
    static Logger   logger = LoggerFactory.getLogger( "WTVCodecFactory" );

    protected ProtocolEncoder wtvProtocolEncoder = new WTVProtocolEncoder( );
    protected ProtocolDecoder wtvProtocolDecoder = new WTVProtocolDecoder( );

    public ProtocolEncoder getEncoder( IoSession ioSession ) throws Exception
    {
        return wtvProtocolEncoder;
    }

    public ProtocolDecoder getDecoder( IoSession ioSession ) throws Exception
    {
        return wtvProtocolDecoder;
    }
}

