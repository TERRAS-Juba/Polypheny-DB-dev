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

package org.polypheny.security.policyengine.dto;

import java.util.ArrayList;

public class SelectPolicyDto {
    ArrayList<String> rule_granularity_user;
    ArrayList<String> forbidden_select;
    ArrayList<String> rule_granularity_role;

    public SelectPolicyDto(ArrayList<String> rule_granularity_user, ArrayList<String> forbidden_select, ArrayList<String> rule_granularity_role) {
        this.rule_granularity_user = rule_granularity_user;
        this.forbidden_select = forbidden_select;
        this.rule_granularity_role = rule_granularity_role;
    }

    public ArrayList<String> getRule_granularity_user() {
        return rule_granularity_user;
    }

    public ArrayList<String> getForbidden_select() {
        return forbidden_select;
    }

    public ArrayList<String> getRule_granularity_role() {
        return rule_granularity_role;
    }

    public void setRule_granularity_user(ArrayList<String> rule_granularity_user) {
        this.rule_granularity_user = rule_granularity_user;
    }

    public void setForbidden_select(ArrayList<String> forbidden_select) {
        this.forbidden_select = forbidden_select;
    }

    public void setRule_granularity_role(ArrayList<String> rule_granularity_role) {
        this.rule_granularity_role = rule_granularity_role;
    }
}
