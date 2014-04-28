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

import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.user.Profile;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementation of {@link com.continuuity.loom.store.BaseEntityStore} using a sql database as the persistent store.
 */
public class SQLUserStore extends BaseDBStore implements UserStore {
  private static final Logger LOG  = LoggerFactory.getLogger(SQLUserStore.class);

  // for unit tests only.  Truncate is not supported in derby.
  public void clearData() throws SQLException {
    Connection conn = dbConnectionPool.getConnection();
    try {
      Statement stmt = conn.createStatement();
      try {
        stmt.execute("DELETE FROM profiles");
        stmt = conn.createStatement();
      } finally {
        stmt.close();
      }
    } finally {
      conn.close();
    }
  }

  @Inject
  SQLUserStore(DBConnectionPool dbConnectionPool) throws SQLException {
    super(dbConnectionPool);
    if (dbConnectionPool.isEmbeddedDerbyDB()) {
      LOG.warn("Initializing Derby DB... Tables are not optimized for performance.");

      createDerbyTable("CREATE TABLE profiles ( id VARCHAR(255), profile BLOB )");
    }
  }

  @Override
  public Profile getProfile(String userId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("SELECT profile FROM profiles WHERE id=? ");
      statement.setString(1, userId);
      try {
        return getQueryItem(statement, Profile.class);
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public void writeProfile(Profile profile) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement checkStatement = conn.prepareStatement("SELECT id FROM profiles WHERE id=?");
      checkStatement.setString(1, profile.getId());
      PreparedStatement writeStatement;
      if (hasResults(checkStatement)) {
        writeStatement = conn.prepareStatement("UPDATE profiles SET profile=? WHERE id=?");
      } else {
        writeStatement = conn.prepareStatement("INSERT INTO profiles (profile, id) VALUES (?, ?)");
      }
      writeStatement.setBlob(1, new ByteArrayInputStream(CODEC.serialize(profile, Profile.class)));
      writeStatement.setString(2, profile.getId());
      // perform the update or insert
      try {
        writeStatement.executeUpdate();
      } finally {
        writeStatement.close();
      }
    } finally {
      conn.close();
    }
  }

  @Override
  public void deleteProfile(String userId) throws Exception {
    Connection conn = dbConnectionPool.getConnection();
    try {
      PreparedStatement statement = conn.prepareStatement("DELETE FROM profiles WHERE id=?");
      statement.setString(1, userId);
      // perform the update or insert
      try {
        statement.executeUpdate();
      } finally {
        statement.close();
      }
    } finally {
      conn.close();
    }
  }
}
