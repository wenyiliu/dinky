/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.dlink.explainer.sqllineage;

import com.dlink.assertion.Asserts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLCaseExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;

public class LineageUtils {
    protected static final Logger logger = LoggerFactory.getLogger(LineageUtils.class);

    public static void columnLineageAnalyzer(String sql, String type, TreeNode<LineageColumn> node) {
        if (Asserts.isNullString(sql)) {
            return;
        }

        AtomicReference<Boolean> isContinue = new AtomicReference<>(false);
        List<SQLStatement> statements = new ArrayList<>();

        // ??????
        try {
            statements = SQLUtils.parseStatements(sql, type);
        } catch (Exception e) {
            logger.info("can't parser by druid {}",type,e);
        }

        // ?????????????????????
        SQLStatement statement = statements.get(0);
        // ?????????????????????
        SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) statement;
        SQLSelectQuery sqlSelectQuery = sqlSelectStatement.getSelect().getQuery();

        // ???union???????????????
        if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectQuery;
            // ??????????????????
            List<SQLSelectItem> selectItems = sqlSelectQueryBlock.getSelectList();
            selectItems.forEach(x -> {
                // ??????---------------------
                String column = Asserts.isNullString(x.getAlias()) ? x.toString() : x.getAlias();

                if (column.contains(".")) {
                    column = column.substring(column.indexOf(".") + 1);
                }
                column = column.replace("`", "").replace("\"", "");

                String expr = x.getExpr().toString();
                LineageColumn myColumn = new LineageColumn();
                myColumn.setTargetColumnName(column);
                myColumn.setExpression(expr);

                TreeNode<LineageColumn> itemNode = new TreeNode<>(myColumn);
                SQLExpr expr1 = x.getExpr();
                // ?????????????????????????????????????????????
                handlerExpr(expr1, itemNode);

                if (node.getLevel() == 0 || node.getData().getTargetColumnName().equals(column)) {
                    node.addChild(itemNode);
                    isContinue.set(true);
                }

            });

            if (isContinue.get()) {
                // ?????????
                SQLTableSource table = sqlSelectQueryBlock.getFrom();

                // ????????????
                if (table instanceof SQLExprTableSource) {
                    // ???????????????---------------------
                    handlerSQLExprTableSource(node, (SQLExprTableSource) table);
                } else if (table instanceof SQLJoinTableSource) {
                    // ??????join
                    handlerSQLJoinTableSource(node, (SQLJoinTableSource) table, type);
                } else if (table instanceof SQLSubqueryTableSource) {
                    // ?????? subquery ---------------------
                    handlerSQLSubqueryTableSource(node, table, type);
                } else if (table instanceof SQLUnionQueryTableSource) {
                    // ?????? union ---------------------
                    handlerSQLUnionQueryTableSource(node, (SQLUnionQueryTableSource) table, type);
                }
            }

            // ??????---------------------
            // union???????????????
        } else if (sqlSelectQuery instanceof SQLUnionQuery) {
            // ??????---------------------
            columnLineageAnalyzer(((SQLUnionQuery) sqlSelectQuery).getLeft().toString(), type, node);
            columnLineageAnalyzer(((SQLUnionQuery) sqlSelectQuery).getRight().toString(), type, node);
        }
    }

    /**
     * ??????UNION??????
     *
     * @param node
     * @param table
     */
    private static void handlerSQLUnionQueryTableSource(TreeNode<LineageColumn> node, SQLUnionQueryTableSource table, String type) {
        node.getAllLeafs().stream().filter(e -> !e.getData().getIsEnd()).forEach(e -> {
            columnLineageAnalyzer(table.getUnion().toString(), type, e);
        });
    }

    /**
     * ??????sub??????
     *
     * @param node
     * @param table
     */
    private static void handlerSQLSubqueryTableSource(TreeNode<LineageColumn> node, SQLTableSource table, String type) {
        node.getAllLeafs().stream().filter(e -> !e.getData().getIsEnd()).forEach(e -> {
            if (Asserts.isNotNullString(e.getData().getSourceTableName())) {
                if (e.getData().getSourceTableName().equals(table.getAlias())) {
                    columnLineageAnalyzer(((SQLSubqueryTableSource) table).getSelect().toString(), type, e);
                }
            } else {
                columnLineageAnalyzer(((SQLSubqueryTableSource) table).getSelect().toString(), type, e);
            }
        });
    }

    /**
     * ??????JOIN
     *
     * @param node
     * @param table
     */
    private static void handlerSQLJoinTableSource(TreeNode<LineageColumn> node,
                                                  SQLJoinTableSource table, String type) {
        // ??????---------------------
        // ??????????????????
        node.getAllLeafs().stream().filter(e -> !e.getData().getIsEnd()).forEach(e -> {
            if (table.getLeft() instanceof SQLJoinTableSource) {
                handlerSQLJoinTableSource(node, (SQLJoinTableSource) table.getLeft(), type);
            } else if (table.getLeft() instanceof SQLExprTableSource) {
                handlerSQLExprTableSource(node, (SQLExprTableSource) table.getLeft());
            } else if (table.getLeft() instanceof SQLSubqueryTableSource) {
                // ??????---------------------
                handlerSQLSubqueryTableSource(node, table.getLeft(), type);
            } else if (table.getLeft() instanceof SQLUnionQueryTableSource) {
                // ??????---------------------
                handlerSQLUnionQueryTableSource(node, (SQLUnionQueryTableSource) table.getLeft(), type);
            }
        });
        node.getAllLeafs().stream().filter(e -> !e.getData().getIsEnd()).forEach(e -> {
            if (table.getRight() instanceof SQLJoinTableSource) {
                handlerSQLJoinTableSource(node, (SQLJoinTableSource) table.getRight(), type);
            } else if (table.getRight() instanceof SQLExprTableSource) {
                handlerSQLExprTableSource(node, (SQLExprTableSource) table.getRight());
            } else if (table.getRight() instanceof SQLSubqueryTableSource) {
                // ??????---------------------
                handlerSQLSubqueryTableSource(node, table.getRight(), type);
            } else if (table.getRight() instanceof SQLUnionQueryTableSource) {
                // ??????---------------------
                handlerSQLUnionQueryTableSource(node, (SQLUnionQueryTableSource) table.getRight(), type);
            }
        });
    }

    /**
     * ???????????????
     *
     * @param node
     * @param table
     */
    private static void handlerSQLExprTableSource(TreeNode<LineageColumn> node,
                                                  SQLExprTableSource table) {
        SQLExprTableSource tableSource = table;
        String tableName = tableSource.getExpr() instanceof SQLPropertyExpr ? ((
                SQLPropertyExpr) tableSource.getExpr()).getName().replace("`", "").replace("\"", "") : "";
        String alias = Asserts.isNotNullString(tableSource.getAlias()) ? tableSource.getAlias().replace("`", "").replace("\"", "") : "";
        node.getChildren().forEach(e -> {
            e.getChildren().forEach(f -> {
                if (!f.getData().getIsEnd() && (f.getData().getSourceTableName() == null || f.getData().getSourceTableName().equals(tableName) || f
                        .getData().getSourceTableName().equals(alias))) {
                    f.getData().setSourceTableName(tableSource.toString());
                    f.getData().setIsEnd(true);
                    f.getData().setExpression(e.getData().getExpression());
                }
            });

        });
    }

    /**
     * ???????????????
     *
     * @param sqlExpr
     * @param itemNode
     */
    private static void handlerExpr(SQLExpr sqlExpr, TreeNode<LineageColumn> itemNode) {
        // ??????
        if (sqlExpr instanceof SQLAggregateExpr) {
            visitSQLAggregateExpr((SQLAggregateExpr) sqlExpr, itemNode);
        }
        // ??????
        else if (sqlExpr instanceof SQLMethodInvokeExpr) {
            visitSQLMethodInvoke((SQLMethodInvokeExpr) sqlExpr, itemNode);
        }
        // case
        else if (sqlExpr instanceof SQLCaseExpr) {
            visitSQLCaseExpr((SQLCaseExpr) sqlExpr, itemNode);
        }
        // ??????
        else if (sqlExpr instanceof SQLBinaryOpExpr) {
            visitSQLBinaryOpExpr((SQLBinaryOpExpr) sqlExpr, itemNode);
        }
        // ?????????
        else if (sqlExpr instanceof SQLPropertyExpr) {
            visitSQLPropertyExpr((SQLPropertyExpr) sqlExpr, itemNode);
        }
        // ???
        else if (sqlExpr instanceof SQLIdentifierExpr) {
            visitSQLIdentifierExpr((SQLIdentifierExpr) sqlExpr, itemNode);
        }
        // ???????????????
        else if (sqlExpr instanceof SQLIntegerExpr) {
            visitSQLIntegerExpr((SQLIntegerExpr) sqlExpr, itemNode);
        }
        // ??????
        else if (sqlExpr instanceof SQLNumberExpr) {
            visitSQLNumberExpr((SQLNumberExpr) sqlExpr, itemNode);
        }
        // ??????
        else if (sqlExpr instanceof SQLCharExpr) {
            visitSQLCharExpr((SQLCharExpr) sqlExpr, itemNode);
        }
    }

    /**
     * ??????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLMethodInvoke(SQLMethodInvokeExpr expr, TreeNode<LineageColumn> node) {
        if (expr.getArguments().size() == 0) {
            // ????????????????????????????????????????????????
            if (node.getData().getExpression().equals(expr.toString())) {
                node.getData().setIsEnd(true);
            }
        } else {
            expr.getArguments().forEach(expr1 -> {
                handlerExpr(expr1, node);
            });
        }
    }

    /**
     * ??????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLAggregateExpr(SQLAggregateExpr expr, TreeNode<LineageColumn> node) {
        expr.getArguments().forEach(expr1 -> {
            handlerExpr(expr1, node);
        });
    }

    /**
     * ??????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLCaseExpr(SQLCaseExpr expr, TreeNode<LineageColumn> node) {
        handlerExpr(expr.getValueExpr(), node);
        expr.getItems().forEach(expr1 -> {
            handlerExpr(expr1.getValueExpr(), node);
        });
        handlerExpr(expr.getElseExpr(), node);
    }

    /**
     * ??????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLBinaryOpExpr(SQLBinaryOpExpr expr, TreeNode<LineageColumn> node) {
        handlerExpr(expr.getLeft(), node);
        handlerExpr(expr.getRight(), node);
    }

    /**
     * ????????????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLPropertyExpr(SQLPropertyExpr expr, TreeNode<LineageColumn> node) {
        LineageColumn project = new LineageColumn();
        String columnName = expr.getName().replace("`", "").replace("\"", "");
        project.setTargetColumnName(columnName);
        project.setSourceTableName(expr.getOwner().toString());
        TreeNode<LineageColumn> search = node.findChildNode(project);

        if (Asserts.isNull(search)) {
            node.addChild(project);
        }
    }

    /**
     * ???
     *
     * @param expr
     * @param node
     */
    public static void visitSQLIdentifierExpr(SQLIdentifierExpr expr, TreeNode<LineageColumn> node) {
        LineageColumn project = new LineageColumn();
        project.setTargetColumnName(expr.getName().replace("`", "").replace("\"", ""));
        TreeNode<LineageColumn> search = node.findChildNode(project);

        if (Asserts.isNull(search)) {
            node.addChild(project);
        }
    }

    /**
     * ????????????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLIntegerExpr(SQLIntegerExpr expr, TreeNode<LineageColumn> node) {
        LineageColumn project = new LineageColumn();
        project.setTargetColumnName(expr.getNumber().toString());
        // ????????????????????????
        project.setSourceTableName("");
        project.setIsEnd(true);
        TreeNode<LineageColumn> search = node.findChildNode(project);

        if (Asserts.isNull(search)) {
            node.addChild(project);
        }
    }

    /**
     * ??????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLNumberExpr(SQLNumberExpr expr, TreeNode<LineageColumn> node) {
        LineageColumn project = new LineageColumn();
        project.setTargetColumnName(expr.getNumber().toString());
        // ????????????????????????
        project.setSourceTableName("");
        project.setIsEnd(true);
        TreeNode<LineageColumn> search = node.findChildNode(project);

        if (Asserts.isNull(search)) {
            node.addChild(project);
        }
    }

    /**
     * ??????
     *
     * @param expr
     * @param node
     */
    public static void visitSQLCharExpr(SQLCharExpr expr, TreeNode<LineageColumn> node) {
        LineageColumn project = new LineageColumn();
        project.setTargetColumnName(expr.toString());
        // ????????????????????????
        project.setSourceTableName("");
        project.setIsEnd(true);
        TreeNode<LineageColumn> search = node.findChildNode(project);

        if (Asserts.isNull(search)) {
            node.addChild(project);
        }
    }
}
