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
package com.continuuity.loom.store;

import com.continuuity.loom.codec.json.JsonSerde;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;

import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

/**
 * Base class for database backed stores with some common utility methods.
 */
public class BaseDBStore {
  private final JsonSerde codec = new JsonSerde();
  protected final DBConnectionPool dbConnectionPool;

  public BaseDBStore(DBConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  protected void createDerbyTable(String createString) throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement statement = conn.createStatement();
      try {
        statement.executeUpdate(createString);
      } catch (SQLException e) {
        // code for the table already exists in derby.
        if (!e.getSQLState().equals("X0Y32")) {
          throw Throwables.propagate(e);
        }
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  /**
   * Queries the store for a set of items, deserializing the items and returning an immutable set of them. If no items
   * exist, the set will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution. Will be closed by this method.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @return Set of items queried for, which will be empty if no items exist.
   * @throws SQLException
   */
  protected <T> ImmutableSet<T> getQuerySet(PreparedStatement statement, Class<T> clazz) throws SQLException {
    try {
      ResultSet rs = statement.executeQuery();
      try {
        Set<T> results = Sets.newHashSet();
        while (rs.next()) {
          Blob blob = rs.getBlob(1);
          results.add(deserializeBlob(blob, clazz));
        }
        return ImmutableSet.copyOf(results);
      } finally {
        rs.close();
      }
    } finally {
      statement.close();
    }
  }


  protected <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz) throws SQLException {
    return getQueryList(statement, clazz, Integer.MAX_VALUE);
  }

  /**
   * Queries the store for a list of items, deserializing the items and returning an immutable list of them. If no items
   * exist, the list will be empty.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the items being queried.
   * @param <T> Type of the items being queried.
   * @param limit Max number of items to get.
   * @return List of items queried for, which will be empty if no items exist.
   * @throws SQLException
   */
  protected <T> ImmutableList<T> getQueryList(PreparedStatement statement, Class<T> clazz, int limit)
    throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      List<T> results = Lists.newArrayList();
      int numResults = 0;
      int actualLimit = limit < 0 ? Integer.MAX_VALUE : limit;
      while (rs.next() && numResults < actualLimit) {
        Blob blob = rs.getBlob(1);
        results.add(deserializeBlob(blob, clazz));
        numResults++;
      }
      return ImmutableList.copyOf(results);
    } finally {
      rs.close();
    }
  }

  /**
   * Queries the store for a single item, deserializing the item and returning it or null if the item does not exist.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @param clazz Class of the item being queried.
   * @param <T> Type of the item being queried.
   * @return Item queried for, or null if it does not exist.
   * @throws SQLException
   */
  protected <T> T getQueryItem(PreparedStatement statement, Class<T> clazz) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      if (rs.next()) {
        Blob blob = rs.getBlob(1);
        return deserializeBlob(blob, clazz);
      } else {
        return null;
      }
    } finally {
      rs.close();
    }
  }

  /**
   * Performs the query and returns whether or not there are results.
   *
   * @param statement PreparedStatement of the query, ready for execution.
   * @return True if the query has results, false if not.
   * @throws SQLException
   */
  protected boolean hasResults(PreparedStatement statement) throws SQLException {
    ResultSet rs = statement.executeQuery();
    try {
      return rs.next();
    } finally {
      rs.close();
    }
  }

  protected <T> T deserializeBlob(Blob blob, Class<T> clazz) throws SQLException {
    Reader reader = new InputStreamReader(blob.getBinaryStream(), Charsets.UTF_8);
    T object;
    try {
      object = codec.deserialize(reader, clazz);
    } finally {
      Closeables.closeQuietly(reader);
    }
    return object;
  }
}
