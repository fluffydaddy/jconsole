package io.fluffydaddy.jconsole.java;

import io.fluffydaddy.jconsole.ConsoleEncoding;
import io.fluffydaddy.jconsole.ConsoleListener;
import io.fluffydaddy.jutils.queue.ByteQueue;
import io.fluffydaddy.jreactive.ErrorObserver;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JavaConsoleTest implements ErrorObserver {
    public static void main(String[] args) throws Exception {
        new JavaConsoleTest().test();
    }
    
    public void test() throws Exception {
        ConsoleEncoding encoding = new ConsoleEncoding() {
            @Override
            public CharSequence encode(char[] buf, int off, int len) {
                return new String(buf, off, len);
            }
            
            @Override
            public char[] decode(byte[] text, int off, int len, Charset charset) {
                try {
                    return charset.newDecoder().decode(ByteBuffer.wrap(text, off, len)).array();
                } catch (CharacterCodingException e) {
                    onError(e);
                    return null;
                }
            }
            
            @Override
            public byte[] toByteArray(CharSequence text, int off, int len, Charset charset) {
                try {
                    return charset.newEncoder().encode(CharBuffer.wrap(text, off, len)).array();
                } catch (CharacterCodingException e) {
                    onError(e);
                    return null;
                }
            }
        };
        JavaConsole console = new JavaConsole(this,
                encoding,
                StandardCharsets.UTF_8,
                ByteQueue.QUEUE_SIZE,
                ByteQueue.QUEUE_SIZE,
                true);
        console.start();
        
        console.subscribe(new ConsoleListener() {
            @Override
            public void onConsole(CharSequence text, long elapsedTime, boolean error) {
                System.out.println("onConsole()!");
            }
            
            @Override
            public void onInput(InputStream inputStream) {
            
            }
        });
        
        Thread.sleep(1250);
        
        String text = "Hello, World!\r\n";
        console.notifyData(text, 0, text.length(), false);
    }
    
    @Override
    public void onError(Throwable cause) {
        cause.printStackTrace(System.err);
    }
}
