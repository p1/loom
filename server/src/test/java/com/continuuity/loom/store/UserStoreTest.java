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

import com.continuuity.loom.user.Profile;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for getting and setting user profiles.  Test classes for different types of stores must set the
 * protected entityStore field before each test and make sure state is wiped out between tests.
 */
public abstract class UserStoreTest {
  protected static UserStore userStore;

  @Test
  public void testGetStoreDeleteProfile() throws Exception {
    JsonObject mods = new JsonObject();
    mods.addProperty("key1", "val1");
    JsonObject nested = new JsonObject();
    nested.addProperty("nestedkey1", "nestedval1");
    mods.add("key2", nested);
    JsonArray arr = new JsonArray();
    arr.add(new JsonPrimitive("val3"));
    arr.add(new JsonPrimitive("val4"));
    mods.add("key3", arr);
    Profile profile = new Profile("user", "skin", mods);
    Assert.assertNull(userStore.getProfile(profile.getId()));

    // write should work
    userStore.writeProfile(profile);
    Profile result = userStore.getProfile(profile.getId());
    Assert.assertEquals(profile, result);

    // overwrite should work
    userStore.writeProfile(profile);
    result = userStore.getProfile(profile.getId());
    Assert.assertEquals(profile, result);

    // delete should work
    userStore.deleteProfile(profile.getId());
    Assert.assertNull(userStore.getProfile(profile.getId()));
  }
}
