package com.pman.distributedurlshortener.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.pman.distributedurlshortener.Application;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConnectionPool {

    private HikariDataSource dataSource;

    public ConnectionPool() {
        HikariConfig config = getPostgresCon();

        dataSource = new HikariDataSource(config);
        System.out.println("Connection pool created");
    }

    private HikariConfig getPostgresCon() {
        Properties dbConfig = Application.getPostgres();
        HikariConfig config = new HikariConfig();

        config.setDataSourceClassName(dbConfig.getProperty("sql.datasource.classname"));
        config.addDataSourceProperty("serverName", dbConfig.getProperty("sql.datasource.servername"));
        config.addDataSourceProperty("portNumber", dbConfig.getProperty("sql.datasource.portnumber"));
        config.addDataSourceProperty("databaseName", dbConfig.getProperty("sql.datasource.databasename"));
        config.setUsername(dbConfig.getProperty("sql.datasource.user"));
        config.setPassword(dbConfig.getProperty("sql.datasource.password"));

        config.setMinimumIdle(Integer.parseInt(dbConfig.getProperty("sql.datasource.minidle")));
        config.setMaximumPoolSize(Integer.parseInt(dbConfig.getProperty("sql.datasource.maxpoolsize")));

        config.setPoolName("DUS Pool");

        System.out.println("datasource config created");
        System.out.println(config.getDataSourceProperties().toString());

        return config;
    }

    /**
     * get a connection from the pool
     * 
     * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean save(String hash, String url) throws SQLException {
        String query = "INSERT INTO dus_schema.url_mapping(hash, url) VALUES (?, ?)";
        PreparedStatement stmt = this.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, hash);
        stmt.setString(2, url);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        return rs.next();
    }

    public String getUrl(String hash) throws SQLException {
        String query = "SELECT url FROM dus_schema.url_mapping WHERE hash = ?";
        PreparedStatement stmt = this.getConnection().prepareStatement(query);
        stmt.setString(1, hash);
        ResultSet resultSet = stmt.executeQuery();
        if (resultSet.next()) {
            return resultSet.getString("url");
        }
        return null;
    }

    public String getHash(String url) throws SQLException {
        String query = "SELECT hash FROM dus_schema.url_mapping WHERE url = ?";
        PreparedStatement stmt = this.getConnection().prepareStatement(query);
        stmt.setString(1, url);
        ResultSet resultSet = stmt.executeQuery();
        if (resultSet.next()) {
            return resultSet.getString("hash");
        }
        return null;
    }
}
