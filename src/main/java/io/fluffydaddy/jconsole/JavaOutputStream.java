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

import io.fluffydaddy.jutils.queue.ByteQueue;
import io.fluffydaddy.jutils.queue.ByteQueueListener;

import java.io.IOException;
import java.io.OutputStream;

public final class JavaOutputStream extends OutputStream {
    final ByteQueue queue;
    final ByteQueueListener listener;
    
    public JavaOutputStream(ByteQueue queue, ByteQueueListener listener) {
        this.queue = queue;
        this.listener = listener;
    }
    
    @Override
    public void write(int p) throws IOException {
        write(new byte[]{(byte) p}, 0, 1);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            queue.write(b, off, len);
            listener.onInputUpdate();
        } catch (InterruptedException e) {
            throw new IOException(e.getCause());
        }
    }
}
