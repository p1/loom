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

import com.continuuity.loom.admin.AutomatorType;
import com.continuuity.loom.admin.ClusterTemplate;
import com.continuuity.loom.admin.HardwareType;
import com.continuuity.loom.admin.ImageType;
import com.continuuity.loom.admin.Provider;
import com.continuuity.loom.admin.ProviderType;
import com.continuuity.loom.admin.Service;

import java.io.IOException;
import java.util.Collection;

/**
 * A view of the entity store for adding, modifying, retrieving, and deleting entities that are accessible by
 * a tenant admin user.
 * TODO: introduce concept of owner or group acls for these entities
 */
public interface EntityStoreView {

  /**
   * Get the {@link com.continuuity.loom.admin.Provider} associated with the given unique name
   * or null if no such provider exists.
   *
   * @param providerName Unique name of the provider to get.
   * @return Provider matching the given name or null if no such provider exists.
   * @throws Exception
   */
  Provider getProvider(String providerName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.Provider}s.
   *
   * @return Collection of all providers.
   * @throws Exception
   */
  Collection<Provider> getAllProviders() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.Provider} to the store. Will overwrite the
   * existing {@link com.continuuity.loom.admin.Provider} if it exists.
   *
   * @param provider Provider to write.
   * @throws Exception
   */
  void writeProvider(Provider provider) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.Provider} associated with the given unique name.
   *
   * @param providerName Name of the provider to delete.
   * @throws Exception
   */
  void deleteProvider(String providerName) throws IOException, IllegalAccessException;

  /**
   * Get the {@link com.continuuity.loom.admin.HardwareType} associated with the
   * given unique name or null if no such provider exists.
   *
   * @param hardwareTypeName Unique name of the provider to get.
   * @return Hardware type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  HardwareType getHardwareType(String hardwareTypeName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.HardwareType}s.
   *
   * @return Collection of all hardware types.
   * @throws Exception
   */
  Collection<HardwareType> getAllHardwareTypes() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.HardwareType} to the store.
   * Will overwrite the existing {@link com.continuuity.loom.admin.HardwareType} if it exists.
   *
   * @param hardwareType Hardware type to write.
   * @throws Exception
   */
  void writeHardwareType(HardwareType hardwareType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.HardwareType} associated with the given unique name.
   *
   * @param hardwareTypeName Name of the hardware type to delete.
   * @throws Exception
   */
  void deleteHardwareType(String hardwareTypeName) throws IOException, IllegalAccessException;

  /**
   * Get the {@link com.continuuity.loom.admin.ImageType} associated with the
   * given unique name or null if no such provider exists.
   *
   * @param imageTypeName Unique name of the provider to get.
   * @return Image type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ImageType getImageType(String imageTypeName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.ImageType}s.
   *
   * @return Collection of all image types.
   * @throws Exception
   */
  Collection<ImageType> getAllImageTypes() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.ImageType} to the store.
   * Will overwrite the existing {@link com.continuuity.loom.admin.ImageType} if it exists.
   *
   * @param imageType Image type to write.
   * @throws Exception
   */
  void writeImageType(ImageType imageType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.ImageType} associated with the given unique name.
   *
   * @param imageTypeName Name of the image type to delete.
   * @throws Exception
   */
  void deleteImageType(String imageTypeName) throws IOException, IllegalAccessException;

  /**
   * Get the {@link com.continuuity.loom.admin.Service} associated with the given
   * unique name or null if no such provider exists.
   *
   * @param serviceName Unique name of the provider to get.
   * @return Service matching the given name or null if no such provider exists.
   * @throws Exception
   */
  Service getService(String serviceName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.Service}s.
   *
   * @return Collection of all services.
   * @throws Exception
   */
  Collection<Service> getAllServices() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.Service} to the store.
   * Will overwrite the existing {@link com.continuuity.loom.admin.Service} if it exists.
   *
   * @param service Service to write.
   * @throws Exception
   */
  void writeService(Service service) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.Service} associated with the given unique name.
   *
   * @param serviceName Name of the service to delete.
   * @throws Exception
   */
  void deleteService(String serviceName) throws IOException, IllegalAccessException;

  /**
   * Get the {@link com.continuuity.loom.admin.ClusterTemplate} associated with the given unique name
   * or null if no such provider exists.
   *
   * @param clusterTemplateName Unique name of the provider to get.
   * @return Cluster template matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ClusterTemplate getClusterTemplate(String clusterTemplateName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.ClusterTemplate}s.
   *
   * @return Collection of all cluster templates.
   * @throws Exception
   */
  Collection<ClusterTemplate> getAllClusterTemplates() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.ClusterTemplate} to the store.
   * Will overwrite the existing {@link com.continuuity.loom.admin.ClusterTemplate} if it exists.
   *
   * @param clusterTemplate Cluster template to write.
   * @throws Exception
   */
  void writeClusterTemplate(ClusterTemplate clusterTemplate) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.ClusterTemplate} associated with the given unique name.
   *
   * @param clusterTemplateName Name of the cluster template to delete.
   * @throws Exception
   */
  void deleteClusterTemplate(String clusterTemplateName) throws IOException, IllegalAccessException;

  /**
   * Get the {@link com.continuuity.loom.admin.ProviderType} associated with the given unique name
   * or null if no such provider type exists.
   *
   * @param providerTypeName Unique name of the provider type to get.
   * @return Provider type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  ProviderType getProviderType(String providerTypeName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.ProviderType}s.
   *
   * @return Collection of all provider types.
   * @throws Exception
   */
  Collection<ProviderType> getAllProviderTypes() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.ProviderType} to the store.
   * Will overwrite the existing {@link com.continuuity.loom.admin.ProviderType} if it exists.
   *
   * @param providerType Provider type to write.
   * @throws Exception
   */
  void writeProviderType(ProviderType providerType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.ProviderType} associated with the given unique name.
   *
   * @param providerTypeName Name of the provider type to delete.
   * @throws Exception
   */
  void deleteProviderType(String providerTypeName) throws IOException, IllegalAccessException;

  /**
   * Get the {@link com.continuuity.loom.admin.AutomatorType} associated with the given unique name or null if
   * no such automator type exists.
   *
   * @param automatorTypeName Unique name of the automator type to get.
   * @return Automator type matching the given name or null if no such provider exists.
   * @throws Exception
   */
  AutomatorType getAutomatorType(String automatorTypeName) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.AutomatorType}s.
   *
   * @return Collection of all automator types.
   * @throws Exception
   */
  Collection<AutomatorType> getAllAutomatorTypes() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.AutomatorType} to the store.
   * Will overwrite the existing {@link com.continuuity.loom.admin.AutomatorType} if it exists.
   *
   * @param automatorType Automator type to write.
   * @throws Exception
   */
  void writeAutomatorType(AutomatorType automatorType) throws IOException, IllegalAccessException;

  /**
   * Delete the {@link com.continuuity.loom.admin.AutomatorType} associated with the given unique name.
   *
   * @param automatorTypeName Name of the automator type to delete.
   * @throws Exception
   */
  void deleteAutomatorType(String automatorTypeName) throws IOException, IllegalAccessException;
}
