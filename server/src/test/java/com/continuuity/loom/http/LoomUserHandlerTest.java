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
package com.continuuity.loom.http;

import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.user.Profile;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 *
 */
public class LoomUserHandlerTest extends LoomServiceTestBase {
  private static final Gson GSON = new JsonSerde().getGson();

  @Test
  public void testGetProfile() throws Exception {
    Profile profile = new Profile(USER1, "skin1", new JsonObject());
    userStore.writeProfile(profile);
    HttpResponse response = doGet("/v1/loom/profiles/" + USER1, USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Profile actual = GSON.fromJson(reader, Profile.class);

    Assert.assertEquals(profile, actual);
  }

  @Test
  public void testPutProfile() throws Exception {
    Profile profile = new Profile(USER1, "skin1", new JsonObject());
    // write the profile
    assertResponseStatus(doPut("/v1/loom/profiles/" + USER1, GSON.toJson(profile), USER1_HEADERS),
                         HttpResponseStatus.OK);
    // get it back
    HttpResponse response = doGet("/v1/loom/profiles/" + USER1, USER1_HEADERS);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Profile actual = GSON.fromJson(reader, Profile.class);
    Assert.assertEquals(profile, actual);

    // overwrite profile
    profile = new Profile(USER1, "skin2", new JsonObject());
    assertResponseStatus(doPut("/v1/loom/profiles/" + USER1, GSON.toJson(profile), USER1_HEADERS),
                         HttpResponseStatus.OK);
    // get it back
    response = doGet("/v1/loom/profiles/" + USER1, USER1_HEADERS);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    actual = GSON.fromJson(reader, Profile.class);
    Assert.assertEquals(profile, actual);
  }

  @Test
  public void testDeleteProfile() throws Exception {
    Profile profile = new Profile(USER1, "skin1", new JsonObject());
    userStore.writeProfile(profile);
    // should be able to get it
    assertResponseStatus(doGet("/v1/loom/profiles/" + USER1, USER1_HEADERS), HttpResponseStatus.OK);
    // delete the profile
    assertResponseStatus(doDelete("/v1/loom/profiles/" + USER1, USER1_HEADERS), HttpResponseStatus.OK);
    // should not be able to get it back
    assertResponseStatus(doGet("/v1/loom/profiles/" + USER1, USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testNonexistantProfileReturns404() throws Exception {
    assertResponseStatus(doGet("/v1/loom/profiles/" + USER1, USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }

  @Test
  public void testInvalidPutBodyReturns400() throws Exception {
    assertResponseStatus(doPut("/v1/loom/profiles/" + USER1, "", USER1_HEADERS), HttpResponseStatus.BAD_REQUEST);
    assertResponseStatus(doPut("/v1/loom/profiles/" + USER1, "{}", USER1_HEADERS), HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testUnauthorizedUserReturns403() throws Exception {
    assertResponseStatus(doGet("/v1/loom/profiles/" + USER2, USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete("/v1/loom/profiles/" + USER2, USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doPut("/v1/loom/profiles/" + USER2, "", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
  }
}
