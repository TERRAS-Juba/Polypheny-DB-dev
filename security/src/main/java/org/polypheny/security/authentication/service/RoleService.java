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
import org.polypheny.security.authentication.model.Role;
import org.polypheny.security.authentication.repository.RoleRepository;

public class RoleService implements EntityService<Role> {
    final RoleRepository repository = new RoleRepository();

    @Override
    public Role save(Role entity) {
        repository.open();
        Role role = repository.save(entity);
        repository.close();
        return role;
    }

    @Override
    public Role update(Role entity) {
        repository.open();
        Role role = repository.update(entity);
        repository.close();
        return role;
    }

    @Override
    public void delete(Role entity) {
        repository.open();
        repository.delete(entity.getId());
        repository.close();
    }

    public Role find(String name) {
        repository.open();
        try {
            Role role = repository.findByName(name);
            repository.close();
            return role;
        } catch (NoResultException e) {
            repository.close();
            return null;
        }

    }
}
