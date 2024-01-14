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

import io.fluffydaddy.jutils.queue.ByteQueueListener;
import io.fluffydaddy.jreactive.DataSubscriber;

public interface Console extends DataSubscriber<ConsoleListener>, ByteQueueListener, Runnable {
    void start();
    
    void await();
    
    boolean isAlive();
    
    void notifyData(CharSequence text, int off, int len, boolean error);
}
