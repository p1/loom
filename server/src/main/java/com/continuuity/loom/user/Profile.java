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
package com.continuuity.loom.user;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

/**
 *
 */
public class Profile {
  private final String id;
  private final String skin;
  private final JsonObject mods;

  public Profile(String id, String skin, JsonObject mods) {
    Preconditions.checkArgument(id != null, "Id must be specified.");
    this.id = id;
    this.skin = skin;
    this.mods = mods;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Profile)) {
      return false;
    }

    Profile that = (Profile) o;

    return Objects.equal(id, that.id) &&
      Objects.equal(skin, that.skin) &&
      Objects.equal(mods, that.mods);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, skin, mods);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("id", id)
      .add("skin", skin)
      .add("mods", mods)
      .toString();
  }
}
