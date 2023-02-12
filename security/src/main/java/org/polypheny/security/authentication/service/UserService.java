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

package org.polypheny.security.authentication.service;

import jakarta.persistence.NoResultException;
import org.polypheny.security.authentication.model.User;
import org.polypheny.security.authentication.repository.UserRepository;
import org.polypheny.security.authentication.utils.PasswordEncryptor;

public class UserService implements EntityService<User> {
    final UserRepository repository = new UserRepository();

    @Override
    public User save(User entity) {
        repository.open();
        entity.setPassword(PasswordEncryptor.generateHashedPassword(entity.getPassword()));
        User user = repository.save(entity);
        repository.close();
        return user;
    }

    @Override
    public User update(User entity) {
        repository.open();
        User user = repository.update(entity);
        repository.close();
        return user;
    }

    @Override
    public void delete(User entity) {
        repository.open();
        repository.delete(entity.getId());
        repository.close();
    }

    public User find(String username, String password) {
        repository.open();
        try {
            User user = repository.findByUsername(username);
            if (PasswordEncryptor.verifyPassword(password, user.getPassword())) {
                repository.close();
                return user;
            } else {
                repository.close();
                return null;
            }

        } catch (NoResultException e) {
            repository.close();
            return null;
        }
    }

    public User findByUsername(String username) {
        repository.open();
        try {
            User user = repository.findByUsername(username);
            repository.close();
            return user;

        } catch (NoResultException e) {
            repository.close();
            return null;
        }
    }
}
