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
import com.google.common.util.concurrent.Service;

import java.io.IOException;
import java.util.List;

/**
 * Store for adding, modifying, retrieving, and deleting tenants.
 */
public interface TenantStore extends Service {

  /**
   * Get the {@link com.continuuity.loom.admin.Tenant} associated with the given id or null if none exists.
   *
   * @param id Id of the tenant.
   * @return Tenant for the given id or null if no such tenant exists.
   * @throws IOException
   */
  Tenant getTenant(String id) throws IOException;

  /**
   * Get all {@link com.continuuity.loom.admin.Tenant}s.
   *
   * @return Collection of all tenants.
   * @throws IOException
   */
  List<Tenant> getAllTenants() throws IOException;

  /**
   * Write the given {@link com.continuuity.loom.admin.Tenant} to the store. Will overwrite the existing
   * {@link com.continuuity.loom.admin.Tenant} if one exists.
   *
   * @param tenant Tenant to write.
   * @throws IOException
   */
  void writeTenant(Tenant tenant) throws IOException;

  /**
   * Delete the {@link com.continuuity.loom.admin.Tenant} associated with the given id.
   *
   * @param id Id of the tenant to delete.
   * @throws Exception
   */
  void deleteTenant(String id) throws IOException;
}
