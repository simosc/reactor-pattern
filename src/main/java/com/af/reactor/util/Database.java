package com.af.reactor.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

    public static final String DB_URL = "jdbc:mysql://localhost/reactor";
    public static final String DB_CLASS = "com.mysql.jdbc.Driver";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "used2befun";

    public static final Logger log = Logger.getLogger(Database.class.getName());

    public Database() throws ClassNotFoundException {
        Class.forName(DB_CLASS);
    }

    public int getNext() {

        int next = -1;
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER,
                DB_PASSWORD)) {
            con.setAutoCommit(false);
            Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_UPDATABLE);
            ResultSet rs = stmt
                    .executeQuery("SELECT id, counter from reactor FOR UPDATE");
            if (rs.next()) {
                next = rs.getInt(2) + 1;
                rs.updateInt(2, next);
                rs.updateRow();
            } else {
                log.warning("ResultSet is not an updatable result set.");
            }
            con.commit();
            rs.close();
        } catch (SQLException e) {
            log.log(Level.SEVERE,
                    "Failed to execute SQL statement: " + e.getMessage(), e);
        }
        return next;
    }

    public void resetCounter() {
        try (Connection connection = DriverManager.getConnection(DB_URL,
                DB_USER, DB_PASSWORD)) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("UPDATE reactor SET counter=0");
        } catch (SQLException e) {
            log.log(Level.SEVERE,
                    "Failed to execute SQL statement: " + e.getMessage(), e);
        }
    }
}
