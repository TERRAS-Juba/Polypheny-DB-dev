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

import java.util.Optional;

public class UserService implements EntityService<User> {
    final UserRepository repository =new UserRepository();
    private static UserService service;

    public static UserService getInstance(){
        if (service==null)
        {
            synchronized (UserService.class){
                service=new UserService();
            }
        }
        return service;
    }
    @Override
    public User save(User entity) {
        repository.open();
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
        repository.deleteByUsername(entity.getUsername());
        repository.close();
    }

    public User find(String username) {
        repository.open();
        try{
            User user = repository.findByUsername(username);
            repository.close();
            return user;
        }catch (NoResultException e){
            repository.close();
            return null;
        }
    }
}
