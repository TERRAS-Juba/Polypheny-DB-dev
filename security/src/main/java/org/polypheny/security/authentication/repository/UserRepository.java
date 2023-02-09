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

package org.polypheny.security.authentication.repository;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.polypheny.security.authentication.model.User;

public class UserRepository {
    final private StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure() // configures settings from hibernate.cfg.xml
            .build();
    final SessionFactory factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();

    Session session;

    public UserRepository() {

    }
    public User save(User user) {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        session.persist(user);
        transaction.commit();
        return user;
    }

    public User findByUsername(String username) throws NoResultException {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<User> criteria = builder.createQuery(User.class);
        Root<User> from = criteria.from(User.class);
        criteria.select(from);
        criteria.where(builder.equal(from.get("username"), username));
        TypedQuery<User> typed = session.createQuery(criteria);
        User user = typed.getSingleResult();
        transaction.commit();
        return user;
    }

    public User update(User user) {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        User updatedUser = session.merge(user);
        transaction.commit();
        return updatedUser;
    }

    public void deleteByUsername(Long id) {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        session.remove(session.find(User.class, id));
        transaction.commit();
    }

    public void open() {
        session = factory.openSession();
    }

    public void close() {
        session.close();
    }

}
