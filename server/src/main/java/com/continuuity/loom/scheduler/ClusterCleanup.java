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
package com.continuuity.loom.scheduler;

import com.continuuity.loom.account.Account;
import com.continuuity.loom.cluster.Cluster;
import com.continuuity.loom.cluster.Node;
import com.continuuity.loom.common.conf.Configuration;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.common.queue.Element;
import com.continuuity.loom.common.queue.QueuedElement;
import com.continuuity.loom.common.queue.TrackingQueue;
import com.continuuity.loom.scheduler.task.ClusterService;
import com.continuuity.loom.scheduler.task.ClusterTask;
import com.continuuity.loom.scheduler.task.NodeService;
import com.continuuity.loom.scheduler.task.TaskId;
import com.continuuity.loom.scheduler.task.TaskService;
import com.continuuity.loom.store.cluster.ClusterStore;
import com.continuuity.loom.store.cluster.ClusterStoreService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Run cleanup tasks.  Leader election is performed between servers sharing a zookeeper quorum so this only happens
 * on the leader server.  If an id increment is specified, will only clean up clusters that the server could have
 * created to prevent conflicts with other loom servers.
 */
public class ClusterCleanup implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterCleanup.class);

  private final ClusterService clusterService;
  private final ClusterStore clusterStore;
  private final NodeService nodeService;
  private final TaskService taskService;
  private final TrackingQueue provisionerQueue;
  private final TrackingQueue jobQueue;
  private final long taskTimeout;
  private final long myMod;
  private final long incrementBy;

  @Inject
  private ClusterCleanup(ClusterStoreService clusterStoreService,
                         NodeService nodeService,
                         TaskService taskService,
                         ClusterService clusterService,
                         @Named(Constants.Queue.PROVISIONER) TrackingQueue provisionerQueue,
                         @Named(Constants.Queue.JOB) TrackingQueue jobQueue,
                         Configuration conf) {
    this.clusterService = clusterService;
    this.clusterStore = clusterStoreService.getSystemView();
    this.nodeService = nodeService;
    this.taskService = taskService;
    this.provisionerQueue = provisionerQueue;
    this.jobQueue = jobQueue;
    this.taskTimeout = conf.getLong(Constants.TASK_TIMEOUT_SECS);
    this.incrementBy = conf.getLong(Constants.ID_INCREMENT_BY);
    this.myMod = conf.getLong(Constants.ID_START_NUM) % this.incrementBy;

    LOG.info("Task timeout in seconds = {}", this.taskTimeout);
  }

  // for unit tests
  ClusterCleanup(ClusterStore clusterStore,
                 ClusterService clusterService,
                 NodeService nodeService,
                 TaskService taskService,
                 TrackingQueue provisionerQueue,
                 TrackingQueue jobQueue,
                 long taskTimeout,
                 long startId,
                 long incrementBy) {
    this.clusterStore = clusterStore;
    this.clusterService = clusterService;
    this.nodeService = nodeService;
    this.taskService = taskService;
    this.provisionerQueue = provisionerQueue;
    this.jobQueue = jobQueue;
    this.taskTimeout = taskTimeout;
    this.incrementBy = incrementBy;
    this.myMod = startId % incrementBy;
  }

  @Override
  public void run() {
    try {
      long currentTime = System.currentTimeMillis();

      timeoutTasks(currentTime);

      expireClusters(currentTime);

    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }

  private void timeoutTasks(long currentTime) {
    try {
      long taskFailTime = currentTime - TimeUnit.MILLISECONDS.convert(taskTimeout, TimeUnit.SECONDS);
      LOG.debug("Task fail time = {}", taskFailTime);

      Iterator<QueuedElement> beingConsumed = provisionerQueue.getBeingConsumed();

      while (beingConsumed.hasNext()) {
        QueuedElement queuedElement = beingConsumed.next();

        if (queuedElement.getStatusTime() > taskFailTime) {
          LOG.trace("Task {} with queue time {} has not timed out yet", queuedElement.getElement().getId(),
                    queuedElement.getStatusTime());
          continue;
        }

        String taskId = queuedElement.getElement().getId();
        ClusterTask task = clusterStore.getClusterTask(TaskId.fromString(taskId));

        if (task == null) {
          LOG.warn("provisioner queue contains task {} which is not in the cluster store, removing it from the queue.",
                   taskId);
          provisionerQueue.remove(taskId);
          continue;
        }

        if (provisionerQueue.remove(task.getTaskId())) {
          LOG.debug("Timing out task {} whose queue time is {}", task.getTaskId(), queuedElement.getStatusTime());

          // Fail the task
          String statusMessage = String.format("Timed out by after %d secs", taskTimeout);
          task.setStatusMessage(statusMessage);
          taskService.failTask(task, -1);

          // Update node status
          Node node = clusterStore.getNode(task.getNodeId());
          nodeService.failAction(node, "", statusMessage);

          // Schedule the job
          jobQueue.add(new Element(task.getJobId()));
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }

  private void expireClusters(long currentTime) {
    try {
      LOG.debug("Expiring clusters older than {}", currentTime);

      Set<Cluster> clusters = clusterStore.getExpiringClusters(currentTime);

      if (clusters.isEmpty()) {
        LOG.debug("Got 0 clusters to be expired for time {}", currentTime);
        return;
      }

      LOG.debug("Got {} possible clusters to expire for time {}", clusters.size(), currentTime);

      for (Cluster cluster : clusters) {
        // mod check done here instead of db to avoid full table scan.
        if (Long.valueOf(cluster.getId()) % incrementBy == myMod) {
          LOG.debug("Deleting cluster {} with expire time {}", cluster.getId(), cluster.getExpireTime());
          clusterService.requestClusterDelete(cluster.getId(), Account.SYSTEM_ACCOUNT);
        }
      }
    } catch (Throwable e) {
      LOG.error("Got exception: ", e);
    }
  }
}
