/*
 * Copyright 2019-2023 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.security.authentication.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncryptor {

    public static String generateHashedPassword(String password) {
        return BCrypt.hashpw(password,BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
}
