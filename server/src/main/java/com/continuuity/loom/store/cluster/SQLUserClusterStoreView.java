package com.continuuity.loom.store.cluster;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.store.DBConnectionPool;
import com.continuuity.loom.store.DBQueryHelper;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The cluster store as viewed by a tenant user. A tenant user can read, write, and delete any cluster
 * they own.
 */
public class SQLUserClusterStoreView extends BaseSQLClusterStoreView {
  private final Account account;

  public SQLUserClusterStoreView(DBConnectionPool dbConnectionPool, Account account) {
    super(dbConnectionPool);
    this.account = account;
  }

  @Override
  protected PreparedStatement getSelectAllClustersStatement(Connection conn) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters WHERE tenant_id=? AND owner_id=? ORDER BY create_time DESC");
    statement.setString(1, account.getTenantId());
    statement.setString(2, account.getUserId());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT cluster FROM clusters WHERE id=? AND tenant_id=? AND owner_id=?");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    statement.setString(3, account.getUserId());
    return statement;
  }

  @Override
  boolean allowedToWrite(Cluster cluster) {
    return this.account.equals(cluster.getAccount());
  }

  @Override
  protected PreparedStatement getSetClusterStatement(
    Connection conn, long id, Cluster cluster, ByteArrayInputStream clusterBytes) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "UPDATE clusters SET cluster=?, owner_id=?, tenant_id=?, status=?, expire_time=?" +
        " WHERE id=? AND tenant_id=? AND owner_id=?");
    statement.setBlob(1, clusterBytes);
    statement.setString(2, cluster.getAccount().getUserId());
    statement.setString(3, cluster.getAccount().getTenantId());
    statement.setString(4, cluster.getStatus().name());
    statement.setTimestamp(5, DBQueryHelper.getTimestamp(cluster.getExpireTime()));
    // where clause
    statement.setLong(6, id);
    statement.setString(7, account.getTenantId());
    statement.setString(8, account.getUserId());
    return statement;
  }

  @Override
  protected PreparedStatement getClusterExistsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT id FROM clusters WHERE id=? AND owner_id=? AND tenant_id=? AND owner_id=?");
    statement.setLong(1, id);
    statement.setString(2, account.getUserId());
    statement.setString(3, account.getTenantId());
    statement.setString(4, account.getUserId());
    return statement;
  }

  @Override
  protected PreparedStatement getDeleteClusterStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "DELETE FROM clusters WHERE id=? AND tenant_id=? AND owner_id=?");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    statement.setString(3, account.getUserId());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterJobsStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT J.job FROM jobs J, clusters C " +
        "WHERE C.id=? AND C.tenant_id=? AND C.owner_id=? AND C.id=J.cluster_id ORDER BY job_num DESC");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    statement.setString(3, account.getUserId());
    return statement;
  }

  @Override
  protected PreparedStatement getSelectClusterNodesStatement(Connection conn, long id) throws SQLException {
    PreparedStatement statement = conn.prepareStatement(
      "SELECT N.node FROM nodes N, clusters C WHERE C.id=? AND C.tenant_id=? AND C.owner_id=? AND N.cluster_id=C.id");
    statement.setLong(1, id);
    statement.setString(2, account.getTenantId());
    statement.setString(3, account.getUserId());
    return statement;
  }
}
