/*
 * Copyright 2019-2021 The Polypheny Project
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

package org.polypheny.db.algebra.constant;


/**
 * Enumerates the types of condition in a join expression.
 */
public enum JoinConditionType {
    /**
     * Join clause has no condition, for example "FROM EMP, DEPT"
     */
    NONE,

    /**
     * Join clause has an ON condition, for example "FROM EMP JOIN DEPT ON EMP.DEPTNO = DEPT.DEPTNO"
     */
    ON,

    /**
     * Join clause has a USING condition, for example "FROM EMP JOIN DEPT USING (DEPTNO)"
     */
    USING;
}

