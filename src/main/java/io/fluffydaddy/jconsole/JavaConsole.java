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

package io.fluffydaddy.jconsole;

import androidx.arch.core.executor.ArchTaskExecutor;
import io.fluffydaddy.jutils.Unit;
import io.fluffydaddy.jutils.queue.ByteQueue;
import io.fluffydaddy.jutils.queue.ByteQueueListener;
import io.fluffydaddy.reactive.ErrorObserver;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class JavaConsole implements Console {
    private boolean isAlive;
    
    private InputStream systemInputStream;
    private PrintStream systemOutputStream;
    private PrintStream systemErrorStream;
    
    private final ByteQueue inputBuffer = new ByteQueue(ByteQueue.QUEUE_SIZE);
    
    private final ByteQueue stdoutBuffer = new ByteQueue(ByteQueue.QUEUE_SIZE);
    private final ByteQueue stderrBuffer = new ByteQueue(ByteQueue.QUEUE_SIZE);
    private final byte[] receiveBuffer = new byte[ByteQueue.QUEUE_SIZE];
    
    private final ConsoleListener inputListener;
    private final ErrorObserver errorListener;
    
    private final Unit<String> logNormalListener = new Unit<>() {
        @Override
        public void accept(String log) {
            // logging
            if (inputListener != null) {
                inputListener.onInput(log, false);
            }
        }
    };
    
    private final Unit<String> logErrorListener = new Unit<>() {
        @Override
        public void accept(String err) {
            // debugging
            if (inputListener != null) {
                inputListener.onInput(err, true);
            }
        }
    };
    
    public JavaConsole(ConsoleListener inputListener, ErrorObserver errorListener) {
        this.inputListener = inputListener;
        this.errorListener = errorListener;
    }
    
    @Override
    public void start() {
        run();
        
        isAlive = true;
    }
    
    @Override
    public void await() {
        isAlive = false;
        
        System.setIn(systemInputStream);
        System.setOut(systemOutputStream);
        System.setErr(systemErrorStream);
    }
    
    @Override
    public boolean isAlive() {
        return isAlive;
    }
    
    @Override
    public void run() {
        InputStream consoleInputStream = new JavaInputStream(inputBuffer);
        PrintStream consoleOutputStream = new PrintStream(
                new JavaOutputStream(
                        stdoutBuffer,
                        newQueueListener(true)
                )
        );
        PrintStream consoleErrorStream = new PrintStream(
                new JavaOutputStream(
                        stderrBuffer,
                        newQueueListener(false)
                )
        );
        
        inputBuffer.reset();
        stdoutBuffer.reset();
        stderrBuffer.reset();
        
        systemInputStream = System.in;
        systemOutputStream = System.out;
        systemErrorStream = System.err;
        
        System.setIn(consoleInputStream);
        System.setOut(consoleOutputStream);
        System.setErr(consoleErrorStream);
    }
    
    private ByteQueueListener newQueueListener(final boolean normal) {
        return () -> {
            if (!isAlive) {
                return;
            }
            try {
                final ByteQueue queue = normal ? stdoutBuffer : stderrBuffer;
                final Unit<String> listener = normal ? logNormalListener : logErrorListener;
                
                final int bytesAvailable = queue.getAvailable();
                final int bytesToRead = Math.max(bytesAvailable, receiveBuffer.length);
                
                final int bytesRead = queue.read(receiveBuffer, 0, bytesToRead);
                final String str = new String(receiveBuffer, 0, bytesRead, StandardCharsets.UTF_8);
                
                ArchTaskExecutor.getMainThreadExecutor().execute(() -> listener.accept(str));
            } catch (InterruptedException e) {
                if (errorListener != null) {
                    errorListener.onError(e);
                }
            }
        };
    }
}
