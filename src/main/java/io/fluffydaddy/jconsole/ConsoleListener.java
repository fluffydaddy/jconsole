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

import java.io.InputStream;

public interface ConsoleListener {
	// text - что пришло; elapsedTime Сколько прошло времени после начала чтения из консоли.
	void onConsole(CharSequence text, long elapsedTime, boolean error);
	// вызывается когда консоль получила, что требует действия от пользователя.
	void onInput(InputStream systemInput);
}
