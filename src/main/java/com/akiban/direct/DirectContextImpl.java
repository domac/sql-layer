/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.akiban.direct;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.akiban.sql.embedded.JDBCConnection;

public class DirectContextImpl implements DirectContext {
    public static final String CONNECTION_URL = "jdbc:default:connection";

    private final String space;
    
    private class ConnectionHolder {
        private Connection connection;
        private ClassLoader contextClassLoader;
        private DirectObject extent;

        private Connection getConnection() {
            if (connection == null) {
                try {
                    connection = DriverManager.getConnection(CONNECTION_URL, space, "");
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            return connection;
        }

        private DirectObject getExtent() {
            if (extent == null) {
                Class<?> cl = classLoader.getExtentClass();
                if (cl != null) {
                    try {
                        extent = (DirectObject) cl.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
            return extent;
        }

        private void enter() {
            contextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        private void leave() {
            try {
                if (connection != null) {
                    try {
                        connection.close();
                        connection = null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    private final DirectClassLoader classLoader;

    private final ThreadLocal<ConnectionHolder> connectionThreadLocal = new ThreadLocal<ConnectionHolder>() {

        @Override
        protected ConnectionHolder initialValue() {
            return new ConnectionHolder();
        }
    };

    public DirectContextImpl(final String space, final DirectClassLoader dcl) {
        this.space = space;
        this.classLoader = dcl;
    }

    @Override
    public Connection getConnection() {
        return connectionThreadLocal.get().getConnection();
    }

    @Override
    public DirectObject getExtent() {
        classLoader.ensureGenerated();
        return connectionThreadLocal.get().getExtent();
    }

    public Statement createStatement() throws SQLException {
        return getConnection().createStatement();
    }

    public void enter() {
        connectionThreadLocal.get().enter();
    }

    public void leave() {
        connectionThreadLocal.get().leave();
    }

    public DirectClassLoader getClassLoader() {
        return classLoader;
    }
}