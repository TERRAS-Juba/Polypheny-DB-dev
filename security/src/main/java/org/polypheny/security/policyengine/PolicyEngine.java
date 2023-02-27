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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.JSQLParserException;
import org.polypheny.security.policyengine.dto.SelectPolicyDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolicyEngine {
    private static PolicyEngine instance;
    private List<SelectPolicyDto> selectPoliciesList;

    private Preprocessing preprocessing;

    private PolicyEngine() {
        selectPoliciesList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> data = null;
        try {
            Path path = Path.of("policyfiles/DML_policies.json");
            data = mapper.readValue(new File("policyfiles/DML_policies.json"), List.class);
            for (Map<String, Object> item : data) {
                if (item.get("rule_type").equals("select")) {
                    selectPoliciesList.add(new SelectPolicyDto((ArrayList<String>) item.get("rule_granularity_user"), (ArrayList<String>) item.get("forbidden_select"), (ArrayList<String>) item.get("rule_granularity_role")));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PolicyEngine getInstance() {
        synchronized (PolicyEngine.class) {
            if (instance == null) {
                return new PolicyEngine();
            } else {
                return instance;
            }
        }
    }

    public Boolean validatePolySqlQuery(String query, String id, String roles) throws RuntimeException{
        preprocessing = new Preprocessing();
        try {
            return preprocessing.validatePolySqlQuery(query, id, roles, this);
        } catch (JSQLParserException e) {
            throw new RuntimeException("The query is either not supported or incorrect");
        }
    }

    public List<SelectPolicyDto> getSelectPoliciesList() {
        return selectPoliciesList;
    }
}
