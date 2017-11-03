/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.entities;

import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "serviceconfig")
@TableGenerator(name = "service_config_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "service_config_id_seq"
  , initialValue = 1
)
@NamedQueries({
    @NamedQuery(
        name = "ServiceConfigEntity.findAll",
        query = "SELECT serviceConfig FROM ServiceConfigEntity serviceConfig WHERE serviceConfig.clusterId=:clusterId ORDER BY serviceConfig.version DESC"),
    @NamedQuery(
        name = "ServiceConfigEntity.findNextServiceConfigVersion",
        query = "SELECT COALESCE(MAX(serviceConfig.version), 0) + 1 AS nextVersion FROM ServiceConfigEntity serviceConfig WHERE serviceConfig.serviceId=:serviceId AND serviceConfig.clusterId=:clusterId"),
    @NamedQuery(
        name = "ServiceConfigEntity.findServiceConfigsByStack",
        query = "SELECT serviceConfig FROM ServiceConfigEntity serviceConfig WHERE serviceConfig.clusterId=:clusterId AND serviceConfig.stack=:stack AND serviceConfig.serviceId=:serviceId"),
    @NamedQuery(
        name = "ServiceConfigEntity.findLatestServiceConfigsByStack",
        query = "SELECT serviceConfig FROM ServiceConfigEntity serviceConfig WHERE serviceConfig.clusterId = :clusterId AND (serviceConfig.groupId = null OR serviceConfig.groupId IN (SELECT cg.groupId from ConfigGroupEntity cg)) AND serviceConfig.version = (SELECT MAX(serviceConfig2.version) FROM ServiceConfigEntity serviceConfig2 WHERE serviceConfig2.clusterId= :clusterId AND serviceConfig2.stack = :stack AND serviceConfig2.serviceId = serviceConfig.serviceId)"),
    @NamedQuery(
        name = "ServiceConfigEntity.findLatestServiceConfigsByService",
        query = "SELECT scv FROM ServiceConfigEntity scv WHERE scv.clusterId = :clusterId AND scv.serviceId = :serviceId AND (scv.groupId = null OR scv.groupId IN (SELECT cg.groupId from ConfigGroupEntity cg)) AND scv.version = (SELECT MAX(scv2.version) FROM ServiceConfigEntity scv2 WHERE (scv2.serviceId = :serviceId AND scv2.clusterId = :clusterId) AND (scv2.groupId = scv.groupId OR (scv2.groupId IS NULL AND scv.groupId IS NULL)))"),
    @NamedQuery(
        name = "ServiceConfigEntity.findLatestServiceConfigsByCluster",
        query = "SELECT scv FROM ServiceConfigEntity scv WHERE scv.clusterId = :clusterId AND scv.serviceConfigId IN (SELECT MAX(scv1.serviceConfigId) FROM ServiceConfigEntity scv1 WHERE (scv1.clusterId = :clusterId) AND (scv1.groupId IS NULL) GROUP BY scv1.serviceId)") })
public class ServiceConfigEntity {
  @Id
  @Column(name = "service_config_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_config_id_generator")
  private Long serviceConfigId;

  @Basic
  @Column(name = "cluster_id", insertable = false, updatable = false, nullable = false)
  private Long clusterId;

  @Basic
  @Column(name = "service_id", nullable = false, insertable = false, updatable = false)
  private Long serviceId;

  @Basic
  @Column(name = "service_group_id", nullable = false, insertable = false, updatable = false)
  private Long serviceGroupId;

  @ManyToOne
  @JoinColumns(
      {
          @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, insertable = false, updatable = false),
          @JoinColumn(name = "service_group_id", referencedColumnName = "service_group_id", nullable = false),
          @JoinColumn(name = "service_id", referencedColumnName = "id", nullable = false)
      })
  private ClusterServiceEntity clusterServiceEntity;

  @Basic
  @Column(name = "group_id", nullable = true)
  private Long groupId;

  @Basic
  @Column(name = "version", nullable = false)
  private Long version;

  @Basic
  @Column(name = "create_timestamp", nullable = false)
  private Long createTimestamp = System.currentTimeMillis();

  @Basic
  @Column(name = "user_name")
  private String user = "_db";

  @Basic
  @Column(name = "note")
  private String note;

  @ElementCollection()
  @CollectionTable(name = "serviceconfighosts", joinColumns = {@JoinColumn(name = "service_config_id")})
  @Column(name = "host_id")
  private List<Long> hostIds;

  /**
   * Cascading remove from serviceConfig to ClusterConfig results in breaking
   * the contract of configs being associated with only the cluster and the
   * same config can technically belong to multiple serviceConfig versions.
   */
  @ManyToMany
  @JoinTable(
    name = "serviceconfigmapping",
    joinColumns = {@JoinColumn(name = "service_config_id", referencedColumnName = "service_config_id")},
    inverseJoinColumns = {@JoinColumn(name = "config_id", referencedColumnName = "config_id")}
  )
  private List<ClusterConfigEntity> clusterConfigEntities;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private ClusterEntity clusterEntity;

  /**
   * Unidirectional one-to-one association to {@link StackEntity}
   */
  @OneToOne
  @JoinColumn(name = "stack_id", unique = false, nullable = false, insertable = true, updatable = true)
  private StackEntity stack;

  public Long getServiceConfigId() {
    return serviceConfigId;
  }

  public void setServiceConfigId(Long serviceConfigId) {
    this.serviceConfigId = serviceConfigId;
  }

  public Long getServiceId() {
    return serviceId;
  }

  public void setServiceId(Long serviceId) {
    this.serviceId = serviceId;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Long getCreateTimestamp() {
    return createTimestamp;
  }

  public void setCreateTimestamp(Long create_timestamp) {
    createTimestamp = create_timestamp;
  }

  public List<ClusterConfigEntity> getClusterConfigEntities() {
    return clusterConfigEntities;
  }

  public void setClusterConfigEntities(List<ClusterConfigEntity> clusterConfigEntities) {
    this.clusterConfigEntities = clusterConfigEntities;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  public List<Long> getHostIds() {
    return hostIds;
  }

  public void setHostIds(List<Long> hostIds) {
    this.hostIds = hostIds;
  }

  /**
   * Gets the service configuration's stack.
   *
   * @return the stack.
   */
  public StackEntity getStack() {
    return stack;
  }

  /**
   * Sets the service configuration's stack.
   *
   * @param stack
   *          the stack to set for the service configuration (not {@code null}).
   */
  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = prime * result
        + ((serviceConfigId == null) ? 0 : serviceConfigId.hashCode());

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    ServiceConfigEntity other = (ServiceConfigEntity) obj;
    if (serviceConfigId == null) {
      if (other.serviceConfigId != null) {
        return false;
      }
    } else if (!serviceConfigId.equals(other.serviceConfigId)) {
      return false;
    }
    return true;
  }

  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  public void setServiceGroupId(Long serviceGroupId) {
    this.serviceGroupId = serviceGroupId;
  }

  public ClusterServiceEntity getClusterServiceEntity() {
    return clusterServiceEntity;
  }

  public void setClusterServiceEntity(ClusterServiceEntity clusterServiceEntity) {
    this.clusterServiceEntity = clusterServiceEntity;
  }

  public String getServiceGroupName() {
    if (clusterServiceEntity != null) {
      return clusterServiceEntity.getServiceGroupName();
    }
    return null;
  }

  public String getServiceName() {
    if (clusterServiceEntity != null) {
      return clusterServiceEntity.getServiceName();
    }
    return null;
  }
}
