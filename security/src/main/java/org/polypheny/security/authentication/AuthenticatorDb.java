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

package org.polypheny.security.authentication;

import org.polypheny.db.catalog.entity.CatalogUser;
import org.polypheny.db.iface.AuthenticationException;
import org.polypheny.db.iface.Authenticator;
import org.polypheny.security.authentication.model.User;
import org.polypheny.security.authentication.service.UserService;

public class AuthenticatorDb implements Authenticator {
        private final UserService userService=UserService.getInstance();
        @Override
        public CatalogUser authenticate(String username, String password) throws AuthenticationException {
                User user=userService.find(username,password);
                if(user!=null){
                        return new CatalogUser(Math.toIntExact(user.getId()),user.getUsername(),user.getPassword());
                }else {
                     throw new AuthenticationException("Invalid Credentials");
                }
        }
}
