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

package org.polypheny.security.admin;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;
import org.polypheny.security.admin.dto.AddRoleRequest;
import org.polypheny.security.admin.dto.AddUserRequest;
import org.polypheny.security.authentication.model.Role;
import org.polypheny.security.authentication.model.User;
import org.polypheny.security.authentication.service.RoleService;
import org.polypheny.security.authentication.service.UserService;

import java.util.HashSet;
import java.util.Set;

public class CrudManager {
    UserService userService = new UserService();
    RoleService roleService = new RoleService();

    public User addUser(AddUserRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user != null) {
            throw new ConflictResponse("the user already exist");
        }
        String[] roles = request.getRoles();
        Set<Role> rolesDb = new HashSet<>();
        for (String str : roles) {
            Role role = roleService.find(str);
            if (role == null) {
                throw new BadRequestResponse("Invalid roles");
            } else {
                rolesDb.add(role);
            }
        }
        user = new User(null, request.getUsername(), request.getPassword(), true, null);
        User addedUser = userService.save(user);
        addedUser.setRoles(rolesDb);
        userService.update(addedUser);
        return addedUser;
    }

    public void deleteUser(String username) {
        User user = userService.findByUsername(username);
        if (user != null) {
            userService.delete(user);
        } else {
            throw new NotFoundResponse();
        }

    }

    public Role addRole(AddRoleRequest request) {
        Role role = roleService.find(request.getName());
        if (role != null) {
            throw new ConflictResponse("the role already exist");
        } else {
            role = roleService.save(new Role(null, request.getName(), null));
        }
        return role;
    }

    public void deleteRole(String name) {
        Role role = roleService.find(name);
        if (role != null) {
            roleService.delete(role);
        } else {
            throw new NotFoundResponse();
        }
    }

}
