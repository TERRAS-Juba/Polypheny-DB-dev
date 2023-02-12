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

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polypheny.security.authentication.model.Role;
import org.polypheny.security.authentication.model.User;
import org.polypheny.security.authentication.repository.UserRepository;
import org.polypheny.security.authentication.utils.PasswordEncryptor;

import java.util.HashSet;
import java.util.Set;

public class TestUserRepository {
    private static StandardServiceRegistry registry;
    private static SessionFactory factory;
    private static UserRepository repository;

    @BeforeEach
    public void init() {
        registry = new StandardServiceRegistryBuilder().configure("hibernate-test.cfg.xml").build();
        factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        repository = new UserRepository(registry, factory);
    }
    @Test
    public void testOpenCloseSession() {
        repository.open();
        repository.close();
    }

    @Test
    public void TestSaveUser() {
        repository.open();
        Set<Role> roles = new HashSet<>();
        roles.add(new Role(null, "ADMIN", null));
        User user = new User(null, "user", PasswordEncryptor.generateHashedPassword("password"), true, roles);
        repository.save(user);
        repository.open();
    }

    @Test
    public void TestFindUserByUsername() {
        // Add user
        repository.open();
        // Create some roles
        Set<Role> roles = new HashSet<>();
        Role role = new Role(null, "ADMIN", null);
        roles.add(role);
        // Create and persist a user
        User user = new User(null, "user", PasswordEncryptor.generateHashedPassword("password"), true, roles);
        repository.save(user);
        // Retrieve user
        User userDb = repository.findByUsername("user");
        repository.close();
        Assertions.assertEquals(userDb.getUsername(), "user");
        Assertions.assertTrue(PasswordEncryptor.verifyPassword("password", userDb.getPassword()));
        Assertions.assertEquals(userDb.getEnabled(), true);
        Assertions.assertEquals(userDb.getRoles().size(), 1);
    }

    @Test
    public void TestUpdateUser() {
        // Add user
        repository.open();
        // Create some roles
        Set<Role> roles = new HashSet<>();
        Role role = new Role(null, "ADMIN", null);
        roles.add(role);
        // Create and persist a user
        User user = new User(null, "user", PasswordEncryptor.generateHashedPassword("password"), true, roles);
        repository.save(user);
        // Retrieve user
        User userDb = repository.findByUsername("user");
        // Update User
        Role role2 = new Role(null, "DOCTOR", null);
        userDb.getRoles().add(role2);
        userDb.setEnabled(false);
        repository.update(userDb);
        // Retrieve user again to see if changes were persisted to database
        User updatedUser = repository.findByUsername("user");
        repository.close();
        Assertions.assertEquals(updatedUser.getUsername(), "user");
        Assertions.assertTrue(PasswordEncryptor.verifyPassword("password", updatedUser.getPassword()));
        Assertions.assertEquals(updatedUser.getEnabled(), false);
        Assertions.assertEquals(updatedUser.getRoles().size(), 2);
    }
    @Test
    public void TestDeleteByUsername() {
        // Add user
        repository.open();
        // Create some roles
        Set<Role> roles = new HashSet<>();
        Role role = new Role(null, "ADMIN", null);
        roles.add(role);
        // Create and persist a user
        User user = new User(null, "user", PasswordEncryptor.generateHashedPassword("password"), true, roles);
        repository.save(user);
        // Retrieve user
        User userDb = repository.findByUsername("user");
        Assertions.assertEquals(userDb.getUsername(), "user");
        Assertions.assertTrue(PasswordEncryptor.verifyPassword("password", userDb.getPassword()));
        Assertions.assertEquals(userDb.getEnabled(), true);
        Assertions.assertEquals(userDb.getRoles().size(), 1);
        // Delete the user
        repository.delete(userDb.getId());
        // Retrieve user again to see if changes were persisted to database
        Assertions.assertThrows(jakarta.persistence.NoResultException.class,()->repository.findByUsername("user"));
        repository.close();
    }

}
