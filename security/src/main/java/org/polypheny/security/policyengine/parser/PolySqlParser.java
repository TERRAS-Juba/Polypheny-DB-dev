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

package org.polypheny.security.policyengine.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PolySqlParser {
    private final Map<String, String> tables;
    private final List<List<String>> attributes;
    private final List<String> joinConditions;
    private final List<List<String>> whereConditions;
    private final List<String> groupByColumn;
    private final List<String> orderByColumns;
    private final List<String> havingExpression;
    private final List<String> limitExpression;
    private final List<String> offsetExpression;

    public PolySqlParser(Map<String, String> tables, List<List<String>> attributes, List<String> joinConditions, List<List<String>> whereConditions,List<String> groupByColumn, List<String> orderByColumns, List<String> havingExpression, List<String> limitExpression, List<String> offsetExpression) {
        this.tables = tables;
        this.attributes = attributes;
        this.joinConditions = joinConditions;
        this.whereConditions = whereConditions;
        this.groupByColumn = groupByColumn;
        this.orderByColumns = orderByColumns;
        this.havingExpression = havingExpression;
        this.limitExpression = limitExpression;
        this.offsetExpression = offsetExpression;
    }

    public void preprocess(String query) throws JSQLParserException {
        Statement statement;
        statement = CCJSqlParserUtil.parse(query);
        Select select = (Select) statement;
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        // Get the columns in the SELECT clause
        extractTables(plainSelect);
        extractJoinTables(plainSelect);
        extractAttributes(plainSelect);
        extractWhereConditions(plainSelect);
        extractGroupByCondition(plainSelect);
        extractOrderByColumns(plainSelect);
        extractLimitOffsetHavingExpressions(plainSelect);
    }

    //******************************************************************************
    // Parser functions
    //******************************************************************************
    private void extractTables(PlainSelect selectBody) {
        // Get the FROM clause and its tables
        FromItem fromItem = selectBody.getFromItem();
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            Alias alias = table.getAlias();
            if (alias != null) {
                tables.put(table.getName(), alias.getName());
            } else {
                tables.put(table.getName(), "");
            }
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            SelectBody subSelectBody = subSelect.getSelectBody();
            PlainSelect subSelectPlain = (PlainSelect) subSelectBody;
            // Recursively parse subselect
            extractTables(subSelectPlain);
        }
    }

    private void extractJoinTables(PlainSelect selectBody) {
        // Get the JOIN clauses and their tables
        List<Join> joins = selectBody.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem joinItem = join.getRightItem();
                if (joinItem instanceof Table) {
                    Table joinTable = (Table) joinItem;
                    Alias joinAlias = joinTable.getAlias();
                    if (joinAlias != null) {
                        tables.put(joinTable.getName(), joinAlias.getName());
                    } else {
                        tables.put(joinTable.getName(), "");
                    }
                } else if (joinItem instanceof SubSelect) {
                    SubSelect subSelect = (SubSelect) joinItem;
                    SelectBody subSelectBody = subSelect.getSelectBody();
                    PlainSelect subSelectPlain = (PlainSelect) subSelectBody;
                    // Recursively parse subselect
                    extractJoinTables(subSelectPlain);
                }
                // Get the ON clause expression
                var onExpressions = join.getOnExpressions();
                if (onExpressions != null) {
                    for (var item : onExpressions) {
                        joinConditions.add(item.toString());
                    }
                }
            }
        }
    }

    private void extractAttributes(PlainSelect selectBody) {
        // Get the columns in the SELECT clause
        List<SelectItem> selectItems = selectBody.getSelectItems();
        for (SelectItem selectItem : selectItems) {
            var attribute = selectItem.toString();
            var liste = new ArrayList<String>();
            if (attribute.equals("*")) {
                liste.add("*");
                attributes.add(liste);
            } else {
                if (attribute.contains(".")) {
                    String[] parts = attribute.split("\\.");
                    liste.addAll(Arrays.asList(parts));
                    attributes.add(liste);
                } else {
                    liste.add(attribute);
                    attributes.add(liste);
                }
            }
        }
    }

    private void extractWhereConditions(PlainSelect selectBody) {
        // Get the WHERE clause expression
        Expression whereExpression = selectBody.getWhere();
        if (whereExpression != null) {
            String[] conditions = whereExpression.toString().toLowerCase().split("and|or");
            for (String condition : conditions) {
                String[] parts = condition.trim().split(" ");
                var liste = new ArrayList<String>();
                for (String part : parts) {
                    liste.add(part.replace("'", ""));
                }
                whereConditions.add(liste);
            }
        }
    }

    private void extractGroupByCondition(PlainSelect selectBody) {
        // Get the GROUP BY columns
        GroupByElement groupByColumns = selectBody.getGroupBy();
        if (groupByColumns != null) {
            groupByColumn.add(groupByColumns.toString().trim());
        }
    }

    private void extractOrderByColumns(PlainSelect selectBody) {
        // Get the ORDER BY columns
        List<OrderByElement> orderByElements = selectBody.getOrderByElements();
        if (orderByElements != null) {
            for (OrderByElement orderByElement : orderByElements) {
                orderByColumns.add(orderByElement.toString().trim());
            }
        }
    }

    private void extractLimitOffsetHavingExpressions(PlainSelect selectBody) {
        // Get the HAVING clause expression
        Expression havingExpression = selectBody.getHaving();
        if (havingExpression != null) {
            this.havingExpression.add(havingExpression.toString().trim());
        }
        // Get the LIMIT and OFFSET expressions
        Limit limitExpression = selectBody.getLimit();
        if (limitExpression != null) {
            this.limitExpression.add(limitExpression.toString().trim());
        }
        Offset offsetExpression = selectBody.getOffset();
        if (offsetExpression != null) {
            this.offsetExpression.add(offsetExpression.toString().trim());
        }
    }
}