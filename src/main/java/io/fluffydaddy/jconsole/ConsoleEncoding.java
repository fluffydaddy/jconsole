package io.fluffydaddy.jconsole;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public interface ConsoleEncoding {
    CharSequence encode(char[] buf, int off, int len);
    char[] decode(byte[] text, int off, int len, Charset charset);
    byte[] toByteArray(CharSequence text, int off, int len, Charset charset);
}
