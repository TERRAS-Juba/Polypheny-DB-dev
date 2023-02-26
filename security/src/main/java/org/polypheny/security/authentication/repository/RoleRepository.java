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
import org.polypheny.security.authentication.model.Role;

public class RoleRepository {
    final private StandardServiceRegistry registry;
    final private SessionFactory factory;

    private Session session;


    public RoleRepository() {
        registry = new StandardServiceRegistryBuilder().configure() // configures settings from hibernate-postgresql.cfg.xml
                .build();
        factory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
    }

    public Role save(Role role) {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        session.persist(role);
        transaction.commit();
        return role;
    }

    public Role findByName(String name) throws NoResultException {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<Role> criteria = builder.createQuery(Role.class);
        Root<Role> from = criteria.from(Role.class);
        criteria.select(from);
        criteria.where(builder.equal(from.get("name"), name));
        TypedQuery<Role> typed = session.createQuery(criteria);
        Role role = typed.getSingleResult();
        transaction.commit();
        return role;
    }

    public Role update(Role role) {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        Role updatedRole = session.merge(role);
        transaction.commit();
        return updatedRole;
    }

    public void delete(Long id) {
        Transaction transaction = session.getTransaction();
        session.beginTransaction();
        session.remove(session.find(Role.class, id));
        transaction.commit();
    }

    public void open() {
        session = factory.openSession();
    }

    public void close() {
        session.close();
    }
}
