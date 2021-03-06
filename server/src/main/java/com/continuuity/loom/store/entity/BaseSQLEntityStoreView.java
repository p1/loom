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
package com.continuuity.loom.store.entity;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.store.DBConnectionPool;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Base implementation of {@link BaseEntityStoreView} using a sql database as the persistent store.
 */
public abstract class BaseSQLEntityStoreView extends BaseEntityStoreView {
  protected final Account account;
  protected final DBConnectionPool dbConnectionPool;
  protected final String accountErrorSnippet;

  BaseSQLEntityStoreView(Account account, DBConnectionPool dbConnectionPool) {
    this.account = account;
    this.dbConnectionPool = dbConnectionPool;
    this.accountErrorSnippet = " from tenant " + account.getTenantId();
  }

  @Override
  protected byte[] getEntity(EntityType entityType, String entityName) throws IOException {
    try {
      byte[] entityBytes = null;
      Connection conn = dbConnectionPool.getConnection();
      try {
        PreparedStatement statement = getSelectStatement(conn, entityType, entityName);
        try {
          ResultSet rs = statement.executeQuery();
          try {
            if (rs.next()) {
              entityBytes = rs.getBytes(1);
            }
          } finally {
            rs.close();
          }
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
      return entityBytes;
    } catch (SQLException e) {
      throw new IOException("Exception getting entity of type " + entityType.name().toLowerCase()
                              + " of name " + entityName + accountErrorSnippet);
    }
  }

  @Override
  protected <T> Collection<T> getAllEntities(EntityType entityType, Function<byte[], T> transform) throws IOException {
    try {
      Connection conn = dbConnectionPool.getConnection();
      List<T> entities = Lists.newLinkedList();
      try {
        PreparedStatement statement = getSelectAllStatement(conn, entityType);
        try {
          ResultSet rs = statement.executeQuery();
          try {
            while (rs.next()) {
              entities.add(transform.apply(rs.getBytes(1)));
            }
          } finally {
            rs.close();
          }
        } finally {
          statement.close();
        }
      } finally {
        conn.close();
      }
      return entities;
    } catch (SQLException e) {
      throw new IOException("Exception getting all entities of type "
                              + entityType.name().toLowerCase() + accountErrorSnippet);
    }
  }

  protected PreparedStatement getSelectStatement(Connection conn, EntityType entityType,
                                               String entityName) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "SELECT " + entityTypeId + " FROM " + entityTypeId + "s WHERE name=? AND tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, entityName);
    statement.setString(2, account.getTenantId());
    return statement;
  }

  private PreparedStatement getSelectAllStatement(Connection conn, EntityType entityType) throws SQLException {
    String entityTypeId = entityType.getId();
    // immune to sql injection since it comes from the enum.
    String queryStr = "SELECT " + entityTypeId + " FROM " + entityTypeId + "s WHERE tenant_id=?";
    PreparedStatement statement = conn.prepareStatement(queryStr);
    statement.setString(1, account.getTenantId());
    return statement;
  }
}
