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

package org.polypheny.security.policyengine;

import net.sf.jsqlparser.JSQLParserException;
import org.polypheny.security.policyengine.dto.SelectPolicyDto;
import org.polypheny.security.policyengine.parser.PolySqlParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Preprocessing {
    private final Map<String, String> tables = new HashMap<>();
    private final List<List<String>> attributes = new ArrayList<>();
    private final List<String> joinConditions = new ArrayList<>();
    private final List<List<String>> whereConditions = new ArrayList<>();
    private final List<String> groupByColumn = new ArrayList<>();
    private final List<String> orderByColumns = new ArrayList<>();
    private final List<String> havingExpression = new ArrayList<>();
    private final List<String> limitExpression = new ArrayList<>();
    private final List<String> offsetExpression = new ArrayList<>();
    private final PolySqlParser parser = new PolySqlParser(tables, attributes, joinConditions, whereConditions, groupByColumn, orderByColumns, havingExpression, limitExpression, offsetExpression);

    public boolean validatePolySqlQuery(String sql, String id, String roles, PolicyEngine engine) throws JSQLParserException {
        parser.preprocess(sql);
        return validateSelectRules(sql, id, roles, engine);
    }

    public boolean validateSelectRules(String sql, String id, String roles, PolicyEngine engine) {
        var liste = engine.getSelectPoliciesList();
        for (int i = 0; i < liste.size(); i++) {
            SelectPolicyDto policy = liste.get(i);
            // check if the user is concerned by this policy
            // Check by role
            boolean role_is_concerned = false;
            if (roles.contains("*")) {
                role_is_concerned = true;
            } else {
                var granularity_role = policy.getRule_granularity_role();
                if (granularity_role != null) {
                    for (String item : granularity_role) {
                        if (roles.contains(item)) {
                            role_is_concerned = true;
                            break;
                        }
                    }
                }
            }
            // Check by user id
            boolean user_is_concerned = false;
            var granularity_user = policy.getRule_granularity_user();
            if (granularity_user != null) {
                user_is_concerned = policy.getRule_granularity_user().contains(id) || policy.getRule_granularity_user().contains("*");
            }
            // check for forbidden table selection
            if (user_is_concerned || role_is_concerned) {
                for (String item : tables.keySet()) {
                    if (liste.get(i).getForbidden_select().contains(item)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
