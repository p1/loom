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

import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.SQLException;

/**
 * Tests for the tenant store.  Test classes for different types of stores must set the
 * protected store field before each test and make sure state is wiped out between tests.
 */
public class SQLTenantStoreTest extends TenantStoreTest {
  protected static SQLTenantStore sqlStore;

  @BeforeClass
  public static void beforeClass() throws SQLException, ClassNotFoundException {
    Configuration sqlConf = Configuration.create();
    sqlConf.set(Constants.JDBC_DRIVER, "org.apache.derby.jdbc.EmbeddedDriver");
    sqlConf.set(Constants.JDBC_CONNECTION_STRING, "jdbc:derby:memory:loom;create=true");
    DBConnectionPool dbConnectionPool = new DBConnectionPool(sqlConf);
    sqlStore = new SQLTenantStore(dbConnectionPool);
    sqlStore.startAndWait();
    sqlStore.clearData();
    store = sqlStore;
  }

  @Override
  public void clearState() throws Exception {
    sqlStore.clearData();
  }

  @AfterClass
  public static void afterClass() {
    DBQueryHelper.dropDerbyDB();
  }
}
