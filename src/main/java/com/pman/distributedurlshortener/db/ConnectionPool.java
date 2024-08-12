package com.pman.distributedurlshortener.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConnectionPool {

    private HikariDataSource dataSource;

    public ConnectionPool() {
        HikariConfig config = getPostgresCon();

        dataSource = new HikariDataSource(config);
        System.out.println("Connection pool created");
    }

    @SuppressWarnings("unused")
    private HikariConfig getH2Config() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://localhost:3306/empdb");
        config.setUsername("root");
        config.setPassword("root");
        config.addDataSourceProperty("minimumIdle", "5");
        config.addDataSourceProperty("maximumPoolSize", "25");

        return config;
    }

    private HikariConfig getPostgresCon() {
        HikariConfig config = new HikariConfig();

        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        config.addDataSourceProperty("serverName", "localhost");
        config.addDataSourceProperty("portNumber", "5432");
        config.addDataSourceProperty("databaseName", "dus");
        config.addDataSourceProperty("user", "dus");
        config.addDataSourceProperty("password", "dus123");

        System.out.println("postgres config created");
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
