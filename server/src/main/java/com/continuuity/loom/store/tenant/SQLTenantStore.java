/*
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.continuuity.loom.store.tenant;

import com.continuuity.loom.admin.Tenant;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Implementation of {@link TenantStore} using a SQL database as the persistent store.
 */
public class SQLTenantStore extends AbstractIdleService implements TenantStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLTenantStore.class);
  private static final JsonSerde codec = new JsonSerde();

  private final DBConnectionPool dbConnectionPool;

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM tenants");
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Inject
  SQLTenantStore(DBConnectionPool dbConnectionPool) throws SQLException, ClassNotFoundException {
    this.dbConnectionPool = dbConnectionPool;
  }

  @Override
  protected void startUp() throws Exception {
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      DBQueryHelper.createDerbyTable(
        "CREATE TABLE tenants ( id VARCHAR(255), name VARCHAR(255), workers INT, tenant BLOB )", dbConnectionPool);
    }
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }

  @Override
  public Tenant getTenant(String id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT tenant FROM tenants WHERE id=?");
        statement.setString(1, id);
        try {
          return DBQueryHelper.getQueryItem(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting tenant {}", id, e);
      throw new IOException(e);
    }
  }

  @Override
  public List<Tenant> getAllTenants() throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("SELECT tenant FROM tenants");
        try {
          return DBQueryHelper.getQueryList(statement, Tenant.class);
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception getting all tenants", e);
      throw new IOException(e);
    }
  }

  @Override
  public void writeTenant(Tenant tenant) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      String tenantId = tenant.getId();
      try {
        PreparedStatement checkStatement = conn.prepareStatement("SELECT id FROM tenants WHERE id=?");
        checkStatement.setString(1, tenantId);
        PreparedStatement writeStatement;
        try {
          ResultSet rs = checkStatement.executeQuery();
          try {
            if (rs.next()) {
              // cluster exists already, perform an update.
              writeStatement = conn.prepareStatement(
                "UPDATE tenants SET tenant=?, workers=? WHERE id=?");
              writeStatement.setBlob(1, new ByteArrayInputStream(codec.serialize(tenant, Tenant.class)));
              writeStatement.setInt(2, tenant.getWorkers());
              writeStatement.setString(3, tenantId);
            } else {
              // cluster does not exist, perform an insert.
              writeStatement = conn.prepareStatement(
                "INSERT INTO tenants (id, workers, tenant) VALUES (?, ?, ?)");
              writeStatement.setString(1, tenantId);
              writeStatement.setInt(2, tenant.getWorkers());
              writeStatement.setBlob(3, new ByteArrayInputStream(codec.serialize(tenant, Tenant.class)));
            }
          } finally {
            rs.close();
          }
          // perform the update or insert
          try {
            writeStatement.executeUpdate();
          } finally {
            writeStatement.close();
          }
        } finally {
          checkStatement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception writing tenant {}", tenant);
      throw new IOException(e);
    }
  }

  @Override
  public void deleteTenant(String id) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = conn.prepareStatement("DELETE FROM tenants WHERE id=? ");
        statement.setString(1, id);
        try {
          statement.executeUpdate();
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception deleting tenant {}", id);
      throw new IOException(e);
    }
  }
}
