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

package com.dlink.flink.catalog;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

import com.dlink.flink.catalog.factory.DlinkMysqlCatalogFactoryOptions;

import org.apache.flink.table.api.Schema;
import org.apache.flink.table.catalog.AbstractCatalog;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogDatabase;
import org.apache.flink.table.catalog.CatalogDatabaseImpl;
import org.apache.flink.table.catalog.CatalogFunction;
import org.apache.flink.table.catalog.CatalogFunctionImpl;
import org.apache.flink.table.catalog.CatalogPartition;
import org.apache.flink.table.catalog.CatalogPartitionSpec;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.CatalogView;
import org.apache.flink.table.catalog.FunctionLanguage;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.ResolvedCatalogBaseTable;
import org.apache.flink.table.catalog.ResolvedCatalogTable;
import org.apache.flink.table.catalog.ResolvedCatalogView;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotEmptyException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotExistException;
import org.apache.flink.table.catalog.exceptions.FunctionAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.FunctionNotExistException;
import org.apache.flink.table.catalog.exceptions.PartitionAlreadyExistsException;
import org.apache.flink.table.catalog.exceptions.PartitionNotExistException;
import org.apache.flink.table.catalog.exceptions.PartitionSpecInvalidException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.exceptions.TableNotPartitionedException;
import org.apache.flink.table.catalog.exceptions.TablePartitionedException;
import org.apache.flink.table.catalog.stats.CatalogColumnStatistics;
import org.apache.flink.table.catalog.stats.CatalogTableStatistics;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.types.DataType;
import org.apache.flink.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ????????? catalog
 * ??????connection done.
 * ??????db?????????????????????????????????????????????????????????????????? default_database
 * ?????????????????????????????????????????????????????????????????????sql????????????????????????
 */
public class DlinkMysqlCatalog extends AbstractCatalog {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    public static final String DEFAULT_DATABASE = "default_database";

    static {
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new CatalogException("????????? mysql ?????????", e);
        }
    }

    private static final String COMMENT = "comment";
    /**
     * ?????????????????????SQL?????????????????????????????????conn??????????????????????????????
     */
    private boolean sqlExceptionHappened = false;

    /**
     * ????????????????????? ?????????????????????
     */
    protected static class ObjectType {

        /**
         * ?????????
         */
        public static final String DATABASE = "database";

        /**
         * ?????????
         */
        public static final String TABLE = "TABLE";

        /**
         * ??????
         */
        public static final String VIEW = "VIEW";
    }

    /**
     * ????????????????????? ?????????????????????
     */
    protected static class ColumnType {

        /**
         * ????????????
         */
        public static final String PHYSICAL = "physical";

        /**
         * ????????????
         */
        public static final String COMPUTED = "computed";

        /**
         * ???????????????
         */
        public static final String METADATA = "metadata";

        /**
         * ??????
         */
        public static final String WATERMARK = "watermark";
    }

    /**
     * ??????????????????
     */
    private final String user;
    /**
     * ???????????????
     */
    private final String pwd;
    /**
     * ???????????????
     */
    private final String url;

    /**
     * ??????database
     */
    private static final String defaultDatabase = "default_database";

    /**
     * ??????????????????
     *
     * @return ??????????????????
     */
    public String getUser() {
        return user;
    }

    /**
     * ???????????????
     *
     * @return ???????????????
     */
    public String getPwd() {
        return pwd;
    }

    /**
     * ??????????????????
     *
     * @return ??????????????????
     */
    public String getUrl() {
        return url;
    }

    public DlinkMysqlCatalog(String name,
                             String url,
                             String user,
                             String pwd) {
        super(name, defaultDatabase);
        this.url = url;
        this.user = user;
        this.pwd = pwd;
    }

    public DlinkMysqlCatalog(String name) {
        super(name, defaultDatabase);
        this.url = DlinkMysqlCatalogFactoryOptions.URL.defaultValue();
        this.user = DlinkMysqlCatalogFactoryOptions.USERNAME.defaultValue();
        this.pwd = DlinkMysqlCatalogFactoryOptions.PASSWORD.defaultValue();
    }

    @Override
    public void open() throws CatalogException {
        // ????????????????????????
        // ????????????db??????????????????
        Integer defaultDbId = getDatabaseId(defaultDatabase);
        if (defaultDbId == null) {
            try {
                createDatabase(defaultDatabase, new CatalogDatabaseImpl(new HashMap<>(), ""), true);
            } catch (DatabaseAlreadyExistException a) {
                logger.info("?????????????????????");
            }
        }
    }

    @Override
    public void close() throws CatalogException {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                sqlExceptionHappened = true;
                throw new CatalogException("Fail to close connection.", e);
            }
        }
    }

    private Connection connection;

    protected Connection getConnection() throws CatalogException {
        try {
            // todo: ???????????????????????????????????????????????????????????????????????????????????????
            // Class.forName(MYSQL_DRIVER);
            if (connection == null) {
                connection = DriverManager.getConnection(url, user, pwd);
            }
            if (sqlExceptionHappened) {
                sqlExceptionHappened = false;
                if (!connection.isValid(10)) {
                    connection.close();
                }
                if (connection.isClosed()) {
                    connection = null;
                    return getConnection();
                }
                connection = null;
                return getConnection();
            }

            return connection;
        } catch (Exception e) {
            throw new CatalogException("Fail to get connection.", e);
        }
    }

    @Override
    public List<String> listDatabases() throws CatalogException {
        List<String> myDatabases = new ArrayList<>();
        String querySql = "SELECT database_name FROM metadata_database";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dbName = rs.getString(1);
                myDatabases.add(dbName);
            }

            return myDatabases;
        } catch (Exception e) {
            throw new CatalogException(
                    String.format("Failed listing database in catalog %s", getName()), e);
        }
    }

    @Override
    public CatalogDatabase getDatabase(String databaseName) throws DatabaseNotExistException, CatalogException {
        String querySql = "SELECT id, database_name,description "
                + " FROM metadata_database where database_name=?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setString(1, databaseName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String description = rs.getString("description");

                Map<String, String> map = new HashMap<>();

                String sql = "select `key`,`value` "
                        + "from metadata_database_property "
                        + "where database_id=? ";
                try (PreparedStatement pStat = conn.prepareStatement(sql)) {
                    pStat.setInt(1, id);
                    ResultSet prs = pStat.executeQuery();
                    while (prs.next()) {
                        map.put(rs.getString("key"), rs.getString("value"));
                    }
                } catch (SQLException e) {
                    sqlExceptionHappened = true;
                    throw new CatalogException(
                            String.format("Failed get database properties in catalog %s", getName()), e);
                }

                return new CatalogDatabaseImpl(map, description);
            } else {
                throw new DatabaseNotExistException(getName(), databaseName);
            }
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException(
                    String.format("Failed get database in catalog %s", getName()), e);
        }
    }

    @Override
    public boolean databaseExists(String databaseName) throws CatalogException {
        return getDatabaseId(databaseName) != null;
    }

    private Integer getDatabaseId(String databaseName) throws CatalogException {
        String querySql = "select id from metadata_database where database_name=?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setString(1, databaseName);
            ResultSet rs = ps.executeQuery();
            boolean multiDB = false;
            Integer id = null;
            while (rs.next()) {
                if (!multiDB) {
                    id = rs.getInt(1);
                    multiDB = true;
                } else {
                    throw new CatalogException("??????????????????database: " + databaseName);
                }
            }
            return id;
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException(String.format("?????? database ???????????????%s.%s", getName(), databaseName), e);
        }
    }

    @Override
    public void createDatabase(String databaseName, CatalogDatabase db,
                               boolean ignoreIfExists) throws DatabaseAlreadyExistException, CatalogException {

        checkArgument(!StringUtils.isNullOrWhitespaceOnly(databaseName));
        checkNotNull(db);
        if (databaseExists(databaseName)) {
            if (!ignoreIfExists) {
                throw new DatabaseAlreadyExistException(getName(), databaseName);
            }
        } else {
            // ?????????????????????????????????
            Connection conn = getConnection();
            // ????????????
            String insertSql = "insert into metadata_database(database_name, description) values(?, ?)";

            try (PreparedStatement stat = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                conn.setAutoCommit(false);
                stat.setString(1, databaseName);
                stat.setString(2, db.getComment());
                stat.executeUpdate();
                ResultSet idRs = stat.getGeneratedKeys();
                if (idRs.next() && db.getProperties() != null && db.getProperties().size() > 0) {
                    int id = idRs.getInt(1);
                    String propInsertSql = "insert into metadata_database_property(database_id, "
                            + "`key`,`value`) values (?,?,?)";
                    PreparedStatement pstat = conn.prepareStatement(propInsertSql);
                    for (Map.Entry<String, String> entry : db.getProperties().entrySet()) {
                        pstat.setInt(1, id);
                        pstat.setString(2, entry.getKey());
                        pstat.setString(3, entry.getValue());
                        pstat.addBatch();
                    }
                    pstat.executeBatch();
                    pstat.close();
                }
                conn.commit();
            } catch (SQLException e) {
                sqlExceptionHappened = true;
                logger.error("?????? database ???????????????", e);
            }
        }
    }

    @Override
    public void dropDatabase(String name, boolean ignoreIfNotExists,
                             boolean cascade) throws DatabaseNotExistException, DatabaseNotEmptyException, CatalogException {
        if (name.equals(defaultDatabase)) {
            throw new CatalogException("?????? database ???????????????");
        }
        // 1?????????db id???
        Integer id = getDatabaseId(name);
        if (id == null) {
            if (!ignoreIfNotExists) {
                throw new DatabaseNotExistException(getName(), name);
            }
            return;
        }
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            // ??????????????????
            List<String> tables = listTables(name);
            if (tables.size() > 0) {
                if (!cascade) {
                    // ??????????????????????????????
                    throw new DatabaseNotEmptyException(getName(), name);
                }
                // ???????????????
                for (String table : tables) {
                    try {
                        dropTable(new ObjectPath(name, table), true);
                    } catch (TableNotExistException t) {
                        logger.warn("???{}?????????", name + "." + table);
                    }
                }
            }
            // todo: ????????????????????????????????????????????????????????????
            String deletePropSql = "delete from metadata_database_property where database_id=?";
            PreparedStatement dStat = conn.prepareStatement(deletePropSql);
            dStat.setInt(1, id);
            dStat.executeUpdate();
            dStat.close();
            String deleteDbSql = "delete from metadata_database where database_id=?";
            dStat = conn.prepareStatement(deleteDbSql);
            dStat.setInt(1, id);
            dStat.executeUpdate();
            dStat.close();
            conn.commit();
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? database ???????????????", e);
        }
    }

    @Override
    public void alterDatabase(String name, CatalogDatabase newDb,
                              boolean ignoreIfNotExists) throws DatabaseNotExistException, CatalogException {
        if (name.equals(defaultDatabase)) {
            throw new CatalogException("?????? database ???????????????");
        }
        // 1?????????db id???
        Integer id = getDatabaseId(name);
        if (id == null) {
            if (!ignoreIfNotExists) {
                throw new DatabaseNotExistException(getName(), name);
            }
            return;
        }
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            // 1??????????????????????????????????????????????????????
            String updateCommentSql = "update metadata_database set description=? where  database_id=?";
            PreparedStatement uState = conn.prepareStatement(updateCommentSql);
            uState.setString(1, newDb.getComment());
            uState.setInt(2, id);
            uState.executeUpdate();
            uState.close();
            if (newDb.getProperties() != null && newDb.getProperties().size() > 0) {
                String upsertSql = "insert  into metadata_database_property (database_id, `key`,`value`) \n"
                        + "values (?,?,?)\n"
                        + "on duplicate key update `value` =?, update_time = sysdate()\n";
                PreparedStatement pstat = conn.prepareStatement(upsertSql);
                for (Map.Entry<String, String> entry : newDb.getProperties().entrySet()) {
                    pstat.setInt(1, id);
                    pstat.setString(2, entry.getKey());
                    pstat.setString(3, entry.getValue());
                    pstat.setString(4, entry.getValue());
                    pstat.addBatch();
                }

                pstat.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? database ???????????????", e);
        }
    }

    @Override
    public List<String> listTables(String databaseName) throws DatabaseNotExistException, CatalogException {
        return listTablesViews(databaseName, ObjectType.TABLE);
    }

    @Override
    public List<String> listViews(String databaseName) throws DatabaseNotExistException, CatalogException {
        return listTablesViews(databaseName, ObjectType.VIEW);
    }

    protected List<String> listTablesViews(String databaseName,
                                           String tableType) throws DatabaseNotExistException, CatalogException {
        Integer databaseId = getDatabaseId(databaseName);
        if (null == databaseId) {
            throw new DatabaseNotExistException(getName(), databaseName);
        }

        // get all schemas
        // ?????????table ??? view
        String querySql = "SELECT table_name FROM metadata_table where table_type=? and database_id = ?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setString(1, tableType);
            ps.setInt(2, databaseId);
            ResultSet rs = ps.executeQuery();

            List<String> tables = new ArrayList<>();

            while (rs.next()) {
                String table = rs.getString(1);
                tables.add(table);
            }
            return tables;
        } catch (Exception e) {
            throw new CatalogException(
                    String.format("Failed listing %s in catalog %s", tableType, getName()), e);
        }
    }

    @Override
    public CatalogBaseTable getTable(ObjectPath tablePath) throws TableNotExistException, CatalogException {
        // ??????????????????
        // 1??????????????? ????????????view????????????table
        // 2????????????
        // 3???????????????
        Integer id = getTableId(tablePath);

        if (id == null) {
            throw new TableNotExistException(getName(), tablePath);
        }

        Connection conn = getConnection();
        try {
            String queryTable = "SELECT table_name "
                    + " ,description, table_type "
                    + " FROM metadata_table "
                    + " where  id=?";
            PreparedStatement ps = conn.prepareStatement(queryTable);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            String description;
            String tableType;
            if (rs.next()) {
                description = rs.getString("description");
                tableType = rs.getString("table_type");
                ps.close();
            } else {
                ps.close();
                throw new TableNotExistException(getName(), tablePath);
            }
            if (tableType.equals(ObjectType.TABLE)) {
                // ????????? table
                String propSql = "SELECT `key`, `value` from metadata_table_property "
                        + "WHERE table_id=?";
                PreparedStatement pState = conn.prepareStatement(propSql);
                pState.setInt(1, id);
                ResultSet prs = pState.executeQuery();
                Map<String, String> props = new HashMap<>();
                while (prs.next()) {
                    String key = prs.getString("key");
                    String value = prs.getString("value");
                    props.put(key, value);
                }
                pState.close();
                props.put(COMMENT, description);
                return CatalogTable.fromProperties(props);
            } else if (tableType.equals(ObjectType.VIEW)) {
                // 1??????????????????table???????????????????????????
                // 2??????????????????
                String colSql = "SELECT column_name, column_type, data_type, description "
                        + " FROM metadata_column WHERE "
                        + " table_id=?";
                PreparedStatement cStat = conn.prepareStatement(colSql);
                cStat.setInt(1, id);
                ResultSet crs = cStat.executeQuery();

                Schema.Builder builder = Schema.newBuilder();
                while (crs.next()) {
                    String colName = crs.getString("column_name");
                    String dataType = crs.getString("data_type");

                    builder.column(colName, dataType);
                    String cDesc = crs.getString("description");
                    if (null != cDesc && cDesc.length() > 0) {
                        builder.withComment(cDesc);
                    }
                }
                cStat.close();
                // 3?????????query
                String qSql = "SELECT `key`, value FROM metadata_table_property"
                        + " WHERE table_id=? ";
                PreparedStatement qStat = conn.prepareStatement(qSql);
                qStat.setInt(1, id);
                ResultSet qrs = qStat.executeQuery();
                String originalQuery = "";
                String expandedQuery = "";
                Map<String, String> options = new HashMap<>();
                while (qrs.next()) {
                    String key = qrs.getString("key");
                    String value = qrs.getString("value");
                    if ("OriginalQuery".equals(key)) {
                        originalQuery = value;
                    } else if ("ExpandedQuery".equals(key)) {
                        expandedQuery = value;
                    } else {
                        options.put(key, value);
                    }
                }
                // ??????view
                return CatalogView.of(builder.build(), description, originalQuery, expandedQuery, options);
            } else {
                throw new CatalogException("???????????????????????????" + tableType);
            }
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? ??????????????????", e);
        }

    }

    @Override
    public boolean tableExists(ObjectPath tablePath) throws CatalogException {
        Integer id = getTableId(tablePath);
        return id != null;
    }

    private Integer getTableId(ObjectPath tablePath) {
        Integer dbId = getDatabaseId(tablePath.getDatabaseName());
        if (dbId == null) {
            return null;
        }
        // ??????id
        String getIdSql = "select id from metadata_table "
                + " where table_name=? and database_id=?";
        Connection conn = getConnection();
        try (PreparedStatement gStat = conn.prepareStatement(getIdSql)) {
            gStat.setString(1, tablePath.getObjectName());
            gStat.setInt(2, dbId);
            ResultSet rs = gStat.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            logger.error("get table fail", e);
            throw new CatalogException("get table fail.", e);
        }
        return null;
    }

    @Override
    public void dropTable(ObjectPath tablePath,
                          boolean ignoreIfNotExists) throws TableNotExistException, CatalogException {
        Integer id = getTableId(tablePath);

        if (id == null) {
            throw new TableNotExistException(getName(), tablePath);
        }
        Connection conn = getConnection();
        try {
            // todo: ????????????????????????????????????????????????????????????
            conn.setAutoCommit(false);
            String deletePropSql = "delete from metadata_table_property "
                    + " where table_id=?";
            PreparedStatement dStat = conn.prepareStatement(deletePropSql);
            dStat.setInt(1, id);
            dStat.executeUpdate();
            dStat.close();
            String deleteColSql = "delete from metadata_column "
                    + " where table_id=?";
            dStat = conn.prepareStatement(deleteColSql);
            dStat.setInt(1, id);
            dStat.executeUpdate();
            dStat.close();
            String deleteDbSql = "delete from metadata_table "
                    + " where id=?";
            dStat = conn.prepareStatement(deleteDbSql);
            dStat.setInt(1, id);
            dStat.executeUpdate();
            dStat.close();
            conn.commit();
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            logger.error("drop table fail", e);
            throw new CatalogException("drop table fail.", e);
        }
    }

    @Override
    public void renameTable(ObjectPath tablePath, String newTableName,
                            boolean ignoreIfNotExists) throws TableNotExistException, TableAlreadyExistException, CatalogException {
        Integer id = getTableId(tablePath);

        if (id == null) {
            throw new TableNotExistException(getName(), tablePath);
        }
        ObjectPath newPath = new ObjectPath(tablePath.getDatabaseName(), newTableName);
        if (tableExists(newPath)) {
            throw new TableAlreadyExistException(getName(), newPath);
        }
        String updateSql = "UPDATE metadata_table SET table_name=? WHERE id=?";
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newTableName);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            sqlExceptionHappened = true;
            throw new CatalogException("??????????????????", ex);
        }
    }

    @Override
    public void createTable(ObjectPath tablePath, CatalogBaseTable table,
                            boolean ignoreIfExists) throws TableAlreadyExistException, DatabaseNotExistException, CatalogException {
        Integer dbId = getDatabaseId(tablePath.getDatabaseName());
        if (null == dbId) {
            throw new DatabaseNotExistException(getName(), tablePath.getDatabaseName());
        }
        if (tableExists(tablePath)) {
            if (!ignoreIfExists) {
                throw new TableAlreadyExistException(getName(), tablePath);
            }
            return;
        }
        // ?????????
        // ?????????table???????????????????????????table????????????view
        // ???????????????table??????????????????????????? resolved table??????????????????properties????????????????????????????????????
        // ???????????????view???????????????????????????????????????
        if (!(table instanceof ResolvedCatalogBaseTable)) {
            throw new UnsupportedOperationException("???????????????????????? ResolvedCatalogBaseTable ????????????");
        }
        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            // ?????????????????????
            CatalogBaseTable.TableKind kind = table.getTableKind();

            String insertSql = "insert into metadata_table(\n"
                    + " table_name,"
                    + " table_type,"
                    + " database_id,"
                    + " description)"
                    + " values(?,?,?,?)";
            PreparedStatement iStat = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            iStat.setString(1, tablePath.getObjectName());
            iStat.setString(2, kind.toString());
            iStat.setInt(3, dbId);
            iStat.setString(4, table.getComment());
            iStat.executeUpdate();
            ResultSet idRs = iStat.getGeneratedKeys();
            if (!idRs.next()) {
                iStat.close();
                throw new CatalogException("??????????????????????????????");
            }
            int id = idRs.getInt(1);
            iStat.close();
            // ??????????????????
            if (table instanceof ResolvedCatalogTable) {
                // table ??????????????????properties??????
                Map<String, String> props = ((ResolvedCatalogTable) table).toProperties();
                String propInsertSql = "insert into metadata_table_property(table_id,"
                        + "`key`,`value`) values (?,?,?)";
                PreparedStatement pStat = conn.prepareStatement(propInsertSql);
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    pStat.setInt(1, id);
                    pStat.setString(2, entry.getKey());
                    pStat.setString(3, entry.getValue());
                    pStat.addBatch();
                }
                pStat.executeBatch();
                pStat.close();
            } else {
                // view????????????????????????????????????
                // view ??????????????????query???expanded query
                // ??????????????????
                ResolvedCatalogView view = (ResolvedCatalogView) table;
                List<Schema.UnresolvedColumn> cols = view.getUnresolvedSchema().getColumns();
                if (cols.size() > 0) {
                    String colInsertSql = "insert into metadata_column("
                            + " column_name, column_type, data_type"
                            + " , `expr`"
                            + " , description"
                            + " , table_id"
                            + " , `primary`) "
                            + " values(?,?,?,?,?,?,?)";
                    PreparedStatement colIStat = conn.prepareStatement(colInsertSql);
                    for (Schema.UnresolvedColumn col : cols) {
                        if (col instanceof Schema.UnresolvedPhysicalColumn) {
                            Schema.UnresolvedPhysicalColumn pCol = (Schema.UnresolvedPhysicalColumn) col;
                            if (!(pCol.getDataType() instanceof DataType)) {
                                throw new UnsupportedOperationException(String.format(
                                        "????????????????????????????????????????????????%s.%s.%s : %s", tablePath.getDatabaseName(),
                                        tablePath.getObjectName(), pCol.getName(),
                                        pCol.getDataType()));
                            }
                            DataType dataType = (DataType) pCol.getDataType();

                            colIStat.setString(1, pCol.getName());
                            colIStat.setString(2, ColumnType.PHYSICAL);
                            colIStat.setString(3,
                                    dataType.getLogicalType().asSerializableString());
                            colIStat.setObject(4, null);
                            colIStat.setString(5, pCol.getComment().orElse(""));
                            colIStat.setInt(6, id);
                            colIStat.setObject(7, null); // view????????????
                            colIStat.addBatch();
                        } else {
                            throw new UnsupportedOperationException("????????????view ???????????? ???????????????");
                        }
                    }
                    colIStat.executeBatch();
                    colIStat.close();

                    // ??? query?????????????????????
                    Map<String, String> option = view.getOptions();
                    if (option == null) {
                        option = new HashMap<>();
                    }
                    option.put("OriginalQuery", view.getOriginalQuery());
                    option.put("ExpandedQuery", view.getExpandedQuery());
                    String propInsertSql = "insert into metadata_table_property(table_id,"
                            + "`key`,`value`) values (?,?,?)";
                    PreparedStatement pStat = conn.prepareStatement(propInsertSql);
                    for (Map.Entry<String, String> entry : option.entrySet()) {
                        pStat.setInt(1, id);
                        pStat.setString(2, entry.getKey());
                        pStat.setString(3, entry.getValue());
                        pStat.addBatch();
                    }
                    pStat.executeBatch();
                    pStat.close();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            sqlExceptionHappened = true;
            logger.error("?????????????????????", ex);
            throw new CatalogException("?????????????????????", ex);
        }
    }

    @Override
    public void alterTable(ObjectPath tablePath, CatalogBaseTable newTable,
                           boolean ignoreIfNotExists) throws TableNotExistException, CatalogException {
        Integer id = getTableId(tablePath);

        if (id == null) {
            throw new TableNotExistException(getName(), tablePath);
        }

        Map<String, String> opts = newTable.getOptions();
        if (opts != null && opts.size() > 0) {
            String updateSql = "INSERT INTO metadata_table_property(table_id,"
                    + "`key`,`value`) values (?,?,?) "
                    + "on duplicate key update `value` =?, update_time = sysdate()";
            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                for (Map.Entry<String, String> entry : opts.entrySet()) {
                    ps.setInt(1, id);
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue());
                    ps.setString(4, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException ex) {
                sqlExceptionHappened = true;
                throw new CatalogException("??????????????????", ex);
            }
        }
    }

    /************************ partition *************************/
    @Override
    public List<CatalogPartitionSpec> listPartitions(ObjectPath tablePath) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public List<CatalogPartitionSpec> listPartitions(ObjectPath tablePath,
                                                     CatalogPartitionSpec partitionSpec) throws TableNotExistException, TableNotPartitionedException, PartitionSpecInvalidException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public List<CatalogPartitionSpec> listPartitionsByFilter(ObjectPath tablePath,
                                                             List<Expression> filters) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public CatalogPartition getPartition(ObjectPath tablePath,
                                         CatalogPartitionSpec partitionSpec) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public boolean partitionExists(ObjectPath tablePath, CatalogPartitionSpec partitionSpec) throws CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void createPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec, CatalogPartition partition,
                                boolean ignoreIfExists) throws TableNotExistException, TableNotPartitionedException, PartitionSpecInvalidException, PartitionAlreadyExistsException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void dropPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec,
                              boolean ignoreIfNotExists) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void alterPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec, CatalogPartition newPartition,
                               boolean ignoreIfNotExists) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    /***********************Functions**********************/

    @Override
    public List<String> listFunctions(String dbName) throws DatabaseNotExistException, CatalogException {
        Integer dbId = getDatabaseId(dbName);
        if (null == dbId) {
            throw new DatabaseNotExistException(getName(), dbName);
        }
        String querySql = "SELECT function_name from metadata_function "
                + " WHERE database_id=?";

        Connection conn = getConnection();
        try (PreparedStatement gStat = conn.prepareStatement(querySql)) {
            gStat.setInt(1, dbId);
            ResultSet rs = gStat.executeQuery();
            List<String> functions = new ArrayList<>();
            while (rs.next()) {
                String n = rs.getString("function_name");
                functions.add(n);
            }
            return functions;
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? UDF ????????????");
        }
    }

    @Override
    public CatalogFunction getFunction(ObjectPath functionPath) throws FunctionNotExistException, CatalogException {
        Integer id = getFunctionId(functionPath);
        if (null == id) {
            throw new FunctionNotExistException(getName(), functionPath);
        }

        String querySql = "SELECT class_name,function_language from metadata_function "
                + " WHERE id=?";
        Connection conn = getConnection();
        try (PreparedStatement gStat = conn.prepareStatement(querySql)) {
            gStat.setInt(1, id);
            ResultSet rs = gStat.executeQuery();
            if (rs.next()) {
                String className = rs.getString("class_name");
                String language = rs.getString("function_language");
                CatalogFunctionImpl func = new CatalogFunctionImpl(className, FunctionLanguage.valueOf(language));
                return func;
            } else {
                throw new FunctionNotExistException(getName(), functionPath);
            }
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? UDF ?????????"
                    + functionPath.getDatabaseName() + "."
                    + functionPath.getObjectName());
        }
    }

    @Override
    public boolean functionExists(ObjectPath functionPath) throws CatalogException {
        Integer id = getFunctionId(functionPath);
        return id != null;
    }

    private Integer getFunctionId(ObjectPath functionPath) {
        Integer dbId = getDatabaseId(functionPath.getDatabaseName());
        if (dbId == null) {
            return null;
        }
        // ??????id
        String getIdSql = "select id from metadata_function "
                + " where function_name=? and database_id=?";
        Connection conn = getConnection();
        try (PreparedStatement gStat = conn.prepareStatement(getIdSql)) {
            gStat.setString(1, functionPath.getObjectName());
            gStat.setInt(2, dbId);
            ResultSet rs = gStat.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                return id;
            }
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            logger.error("get function fail", e);
            throw new CatalogException("get function fail.", e);
        }
        return null;
    }

    @Override
    public void createFunction(ObjectPath functionPath, CatalogFunction function,
                               boolean ignoreIfExists) throws FunctionAlreadyExistException, DatabaseNotExistException, CatalogException {
        Integer dbId = getDatabaseId(functionPath.getDatabaseName());
        if (null == dbId) {
            throw new DatabaseNotExistException(getName(), functionPath.getDatabaseName());
        }
        if (functionExists(functionPath)) {
            if (!ignoreIfExists) {
                throw new FunctionAlreadyExistException(getName(), functionPath);
            }
        }

        Connection conn = getConnection();
        String insertSql = "Insert into metadata_function "
                + "(function_name,class_name,database_id,function_language) "
                + " values (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, functionPath.getObjectName());
            ps.setString(2, function.getClassName());
            ps.setInt(3, dbId);
            ps.setString(4, function.getFunctionLanguage().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? ?????? ??????", e);
        }
    }

    @Override
    public void alterFunction(ObjectPath functionPath, CatalogFunction newFunction,
                              boolean ignoreIfNotExists) throws FunctionNotExistException, CatalogException {
        Integer id = getFunctionId(functionPath);
        if (null == id) {
            if (!ignoreIfNotExists) {
                throw new FunctionNotExistException(getName(), functionPath);
            }
            return;
        }

        Connection conn = getConnection();
        String insertSql = "update metadata_function "
                + "set (class_name =?, function_language=?) "
                + " where id=?";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, newFunction.getClassName());
            ps.setString(2, newFunction.getFunctionLanguage().toString());
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? ?????? ??????", e);
        }
    }

    @Override
    public void dropFunction(ObjectPath functionPath,
                             boolean ignoreIfNotExists) throws FunctionNotExistException, CatalogException {
        Integer id = getFunctionId(functionPath);
        if (null == id) {
            if (!ignoreIfNotExists) {
                throw new FunctionNotExistException(getName(), functionPath);
            }
            return;
        }

        Connection conn = getConnection();
        String insertSql = "delete from metadata_function "
                + " where id=?";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            sqlExceptionHappened = true;
            throw new CatalogException("?????? ?????? ??????", e);
        }
    }

    @Override
    public CatalogTableStatistics getTableStatistics(ObjectPath tablePath) throws TableNotExistException, CatalogException {
        // todo: ????????????????????????
        checkNotNull(tablePath);

        if (!tableExists(tablePath)) {
            throw new TableNotExistException(getName(), tablePath);
        }
        /*
         * if (!isPartitionedTable(tablePath)) { CatalogTableStatistics result = tableStats.get(tablePath); return
         * result != null ? result.copy() : CatalogTableStatistics.UNKNOWN; } else { return
         * CatalogTableStatistics.UNKNOWN; }
         */
        return CatalogTableStatistics.UNKNOWN;
    }

    @Override
    public CatalogColumnStatistics getTableColumnStatistics(ObjectPath tablePath) throws TableNotExistException, CatalogException {
        // todo: ????????????????????????
        checkNotNull(tablePath);

        if (!tableExists(tablePath)) {
            throw new TableNotExistException(getName(), tablePath);
        }

        // CatalogColumnStatistics result = tableColumnStats.get(tablePath);
        // return result != null ? result.copy() : CatalogColumnStatistics.UNKNOWN;
        return CatalogColumnStatistics.UNKNOWN;
    }

    @Override
    public CatalogTableStatistics getPartitionStatistics(ObjectPath tablePath,
                                                         CatalogPartitionSpec partitionSpec) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public CatalogColumnStatistics getPartitionColumnStatistics(ObjectPath tablePath,
                                                                CatalogPartitionSpec partitionSpec) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void alterTableStatistics(ObjectPath tablePath, CatalogTableStatistics tableStatistics,
                                     boolean ignoreIfNotExists) throws TableNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void alterTableColumnStatistics(ObjectPath tablePath, CatalogColumnStatistics columnStatistics,
                                           boolean ignoreIfNotExists) throws TableNotExistException, CatalogException, TablePartitionedException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void alterPartitionStatistics(ObjectPath tablePath, CatalogPartitionSpec partitionSpec,
                                         CatalogTableStatistics partitionStatistics,
                                         boolean ignoreIfNotExists) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }

    @Override
    public void alterPartitionColumnStatistics(ObjectPath tablePath, CatalogPartitionSpec partitionSpec,
                                               CatalogColumnStatistics columnStatistics,
                                               boolean ignoreIfNotExists) throws PartitionNotExistException, CatalogException {
        // todo: ????????????????????????
        throw new UnsupportedOperationException("?????????????????????");
    }
}
