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

import io.fluffydaddy.jutils.queue.ByteQueue;

import java.io.IOException;
import java.io.InputStream;

public class JavaInputStream extends InputStream {
    final Object lock;
    final ByteQueue queue;
    
    public JavaInputStream(ByteQueue queue) {
        this(queue, new Object());
    }
    
    public JavaInputStream(ByteQueue queue, Object lock) {
        this.queue = queue;
        this.lock = lock;
    }
    
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            return read(new byte[0], 0, 0);
        }
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            try {
                return queue.read(b, off, len);
            } catch (InterruptedException e) {
                throw new IOException(e.getCause());
            }
        }
    }
}
