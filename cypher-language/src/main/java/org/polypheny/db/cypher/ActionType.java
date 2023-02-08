/*
 * Copyright 2019-2022 The Polypheny Project
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

package org.polypheny.db.cypher;

public enum ActionType {
    GRAPH_ALL, GRAPH_TRAVERSE, GRAPH_READ, GRAPH_MATCH, GRAPH_DELETE, GRAPH_MERGE, USER_IMPERSONATE, ROLE_ALL, CONSTRAINT_CREATE, INDEX_CREATE, DBMS_ALL, CREATE_LABEL, CREATE_RELTYPE, CREATE_PROPERTYKEY, DATABASE_CREATE, ROLE_CREATE, USER_CREATE, GRAPH_CREATE, INDEX_DROP, CONSTRAINT_DROP, DATABASE_DROP, ROLE_DROP, USER_DROP, INDEX_SHOW, CONSTRAINT_SHOW, TRANSACTION_SHOW, PRIVILEGE_SHOW, ROLE_SHOW, USER_SHOW, USER_PASSWORD, USER_STATUS, USER_HOME, SET_DATABASE_ACCESS, GRAPH_PROPERTY_SET, GRAPH_LABEL_SET, PRIVILEGE_REMOVE, ROLE_REMOVE, GRAPH_LABEL_REMOVE, GRAPH_WRITE, ACCESS, DATABASE_START, DATABASE_STOP, INDEX_ALL, CONSTRAINT_ALL, TRANSACTION_ALL, CREATE_TOKEN, USER_ALTER, DATABASE_ALTER, PRIVILEGE_ASSIGN, ROLE_ASSIGN, DATABASE_MANAGEMENT, EXECUTE_ADMIN_PROCEDURE, EXECUTE_BOOSTED_PROCEDURE, EXECUTE_BOOSTED_FUNCTION, EXECUTE_PROCEDURE, EXECUTE_FUNCTION, PRIVILEGE_ALL, ROLE_RENAME, USER_RENAME, USER_ALL, TRANSACTION_TERMINATE, DATABASE_ALL
}
