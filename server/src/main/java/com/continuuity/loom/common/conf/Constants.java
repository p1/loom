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
package com.continuuity.loom.common.conf;

import com.continuuity.loom.scheduler.ClusterAction;
import com.continuuity.loom.scheduler.callback.HttpPostClusterCallback;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Constants used by Loom.
 */
public class Constants {

  public static final String PORT = "server.port";
  public static final String HOST = "server.host";
  public static final String ZOOKEEPER_QUORUM = "server.zookeeper.quorum";
  public static final String ZOOKEEPER_SESSION_TIMEOUT_MILLIS = "server.zookeeper.session.timeout.millis";
  public static final String ZOOKEEPER_NAMESPACE = "server.zookeeper.namespace";

  public static final String JDBC_DRIVER = "server.jdbc.driver";
  public static final String JDBC_CONNECTION_STRING = "server.jdbc.connection.string";
  public static final String DB_USER = "server.db.user";
  public static final String DB_PASSWORD = "server.db.password";
  public static final String DB_VALIDATION_QUERY = "server.jdbc.validation.query";
  public static final String DB_MAX_ACTIVE_CONNECTIONS = "server.jdbc.max.active.connections";
  public static final String LOCAL_DATA_DIR = "server.local.data.dir";
  public static final String EMBEDDED_DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

  public static final String SCHEDULER_INTERVAL_SECS = "server.scheduler.run.interval.seconds";
  public static final String SOLVER_NUM_THREADS = "server.solver.num.threads";
  public static final String TASK_TIMEOUT_SECS = "server.task.timeout.seconds";
  public static final String CLUSTER_CLEANUP_SECS = "server.cluster.cleanup.seconds";
  public static final String NETTY_EXEC_NUM_THREADS = "server.netty.exec.num.threads";
  public static final String NETTY_WORKER_NUM_THREADS = "server.netty.worker.num.threads";

  public static final String MAX_PER_NODE_LOG_LENGTH = "server.node.max.log.length";
  public static final String MAX_PER_NODE_NUM_ACTIONS = "server.node.max.num.actions";
  public static final String MAX_ACTION_RETRIES = "server.max.action.retries";
  public static final String MAX_CLUSTER_SIZE = "server.max.cluster.size";

  public static final String ID_START_NUM = "server.ids.start.num";
  public static final String ID_INCREMENT_BY = "server.ids.increment.by";

  public static final String CALLBACK_CLASS = "server.callback.class";

  /**
   * {@link HttpPostClusterCallback} config settings.
   */
  public static final class HttpCallback {
    private static final String prefix = "loom.callback.http.";

    public static final String START_URL = prefix + "start.url";
    public static final String SUCCESS_URL = prefix + "success.url";
    public static final String FAILURE_URL = prefix + "failure.url";

    public static final String START_TRIGGERS = prefix + "start.triggers";
    public static final String DEFAULT_START_TRIGGERS = Joiner.on(',').join(ClusterAction.values());

    public static final String SUCCESS_TRIGGERS = prefix + "success.triggers";
    public static final String DEFAULT_SUCCESS_TRIGGERS = Joiner.on(',').join(ClusterAction.values());

    public static final String FAILURE_TRIGGERS = prefix + "failure.triggers";
    public static final String DEFAULT_FAILURE_TRIGGERS = Joiner.on(',').join(ClusterAction.values());

    public static final String SOCKET_TIMEOUT = prefix + "socket.timeout";
    public static final int DEFAULT_SOCKET_TIMEOUT = 10000;

    public static final String MAX_CONNECTIONS = prefix + "max.connections";
    public static final int DEFAULT_MAX_CONNECTIONS = 100;
  }

  /**
   * Queue related constants.
   */
  public static final class Queue {
    public static final String PROVISIONER = "nodeprovisioner.queue";
    public static final String CLUSTER = "cluster.queue";
    public static final String SOLVER = "solver.queue";
    public static final String JOB = "internal.job.queue";
    public static final String CALLBACK = "callback.queue";
    public static final Set<String> ALL = ImmutableSet.of(PROVISIONER, CLUSTER, SOLVER, JOB, CALLBACK);
  }

  public static final String TASK_NAMESPACE = "/tasks";
  public static final String LOCK_NAMESPACE = TASK_NAMESPACE + "/lock";
  public static final String USER_HEADER = "X-Loom-UserID";
  public static final String API_KEY_HEADER = "X-Loom-ApiKey";
  public static final String TENANT_HEADER = "X-Loom-TenantID";
  public static final String SUPERADMIN_TENANT = "loom";
  public static final String ADMIN_USER = "admin";
  public static final String SYSTEM_USER = "system";
}
