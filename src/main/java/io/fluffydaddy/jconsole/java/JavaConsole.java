/*
 * Copyright © 2024 fluffydaddy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fluffydaddy.jconsole.java;

import io.fluffydaddy.jconsole.Console;
import io.fluffydaddy.jconsole.ConsoleEncoding;
import io.fluffydaddy.jconsole.ConsoleListener;
import io.fluffydaddy.jreactive.ErrorObserver;
import io.fluffydaddy.jreactive.impl.Subscriber;
import io.fluffydaddy.jreactive.livedata.runtime.ArchTaskExecutor;
import io.fluffydaddy.jutils.collection.Unit;
import io.fluffydaddy.jutils.queue.ByteQueue;
import io.fluffydaddy.jutils.queue.ByteQueueListener;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

public class JavaConsole extends Subscriber<ConsoleListener> implements Console {
    private boolean _isAlive;
    
    private InputStream _systemInputStream;
    private PrintStream _systemOutputStream;
    private PrintStream _systemErrorStream;
    
    private final ByteQueue _inputBuffer;
    
    private final ByteQueue _stdoutBuffer;
    private final ByteQueue _stderrBuffer;
    private final byte[] _receiveBuffer;
    
    private final ErrorObserver _errorListener;
    private final ConsoleEncoding _encoding;
    private final Charset _charset;
    private final boolean _modeWithSystem;
    
    private long _timeStarted;
    
    private final Unit<CharSequence> logNormalListener = log -> {
        long timeElapsed = calculateTimeElapsed();
        // logging
        forEach((Unit<ConsoleListener>) it -> it.onConsole(log, timeElapsed, false));
    };
    
    private final Unit<CharSequence> logErrorListener = err -> {
        // debugging
        long timeElapsed = calculateTimeElapsed();
        // logging
        forEach((Unit<ConsoleListener>) it -> it.onConsole(err, timeElapsed, true));
    };
    
    public JavaConsole(ErrorObserver errorListener,
                       ConsoleEncoding encoding,
                       Charset charset,
                       int receiveBuffer,
                       int sendBuffer,
                       boolean modeWithSystem) {
        _errorListener = errorListener;
        _encoding = encoding;
        _charset = charset;
        _receiveBuffer = new byte[receiveBuffer];
        _inputBuffer = new ByteQueue(receiveBuffer);
        _stdoutBuffer = new ByteQueue(sendBuffer);
        _stderrBuffer = new ByteQueue(sendBuffer);
        _modeWithSystem = modeWithSystem;
    }
    
    @Override
    public void start() {
        run();
        
        _isAlive = true;
    }
    
    @Override
    public void await() {
        _isAlive = false;
        
        System.setIn(_systemInputStream);
        System.setOut(_systemOutputStream);
        System.setErr(_systemErrorStream);
    }
    
    @Override
    public boolean isAlive() {
        return _isAlive;
    }
    
    @Override
    public void run() {
        InputStream consoleInputStream = new JavaInputStream(_inputBuffer);
        PrintStream consoleOutputStream = new PrintStream(
                new JavaOutputStream(
                        _stdoutBuffer,
                        newQueueListener(true)
                )
        );
        PrintStream consoleErrorStream = new PrintStream(
                new JavaOutputStream(
                        _stderrBuffer,
                        newQueueListener(false)
                )
        );
        
        _inputBuffer.reset();
        _stdoutBuffer.reset();
        _stderrBuffer.reset();
        
        _systemInputStream = System.in;
        _systemOutputStream = System.out;
        _systemErrorStream = System.err;
        
        System.setIn(consoleInputStream);
        System.setOut(consoleOutputStream);
        System.setErr(consoleErrorStream);
        
        _timeStarted = System.currentTimeMillis();
    }
    
    @Override
    public void notifyData(CharSequence text, int off, int len, boolean error) {
        try {
            final byte[] buf = _encoding.toByteArray(text, off, len, _charset);
            if (error) {
                _stderrBuffer.write(buf, 0, buf.length);
                if (_modeWithSystem) {
                    _systemErrorStream.write(buf, 0, buf.length);
                }
            } else {
                _stdoutBuffer.write(buf, 0, buf.length);
                if (_modeWithSystem) {
                    _systemOutputStream.write(buf, 0, buf.length);
                }
            }
        } catch (InterruptedException e) {
            if (_errorListener != null) {
                _errorListener.onError(e);
            }
        }
    }
    
    private long calculateTimeElapsed() {
        return calculateTimeNow() - _timeStarted;
    }
    
    private long calculateTimeNow() {
        return System.currentTimeMillis();
    }
    
    private ByteQueueListener newQueueListener(final boolean normal) {
        return () -> {
            if (!_isAlive) {
                return;
            }
            try {
                final ByteQueue queue = normal ? _stdoutBuffer : _stderrBuffer;
                final Unit<CharSequence> listener = normal ? logNormalListener : logErrorListener;
                
                final int bytesAvailable = queue.getAvailable();
                final int bytesToRead = Math.max(bytesAvailable, _receiveBuffer.length);
                
                final int bytesRead = queue.read(_receiveBuffer, 0, bytesToRead);
                final char[] buf = _encoding.decode(_receiveBuffer, 0, bytesRead, _charset);
                final CharSequence text = _encoding.encode(buf, 0, buf.length);
                
                ArchTaskExecutor.getMainThreadExecutor().execute(() -> listener.accept(text));
            } catch (InterruptedException e) {
                if (_errorListener != null) {
                    _errorListener.onError(e);
                }
            }
        };
    }
    
    @Override
    public void onInputUpdate() {
        forEach((Unit<ConsoleListener>) it -> it.onInput(_systemInputStream));
    }
}
