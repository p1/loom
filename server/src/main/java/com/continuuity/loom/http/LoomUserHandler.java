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

import com.continuuity.http.HttpResponder;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.store.UserStore;
import com.continuuity.loom.user.Profile;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Handler for getting, adding, modifying, and deleting admin defined Loom entities.
 * GET calls work for any user, non-GET calls work only for admin.
 */
@Path("/v1/loom")
public class LoomUserHandler extends LoomAuthHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(LoomUserHandler.class);
  private static final Gson GSON = new JsonSerde().getGson();

  private final UserStore userStore;

  @Inject
  public LoomUserHandler(UserStore userStore) {
    this.userStore = userStore;
  }

  /**
   * Get a specific user {@link com.continuuity.loom.user.Profile}.
   *
   * @param request The request for the user profile.
   * @param responder Responder for sending the response.
   * @param userId Id of the user whose profile to get.
   * @throws Exception
   */
  @GET
  @Path("/profiles/{user-id}")
  public void getUserProfile(HttpRequest request, HttpResponder responder, @PathParam("user-id") String userId) {
    String requestedUserId = authorizeUser(request, responder, userId);
    if (requestedUserId == null) {
      return;
    }

    try {
      Profile profile = userStore.getProfile(requestedUserId);

      if (profile == null) {
        responder.sendError(HttpResponseStatus.NOT_FOUND, "Profile for user " + userId + " not found.");
        return;
      }

      responder.sendJson(HttpResponseStatus.OK, profile);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      LOG.error("Exception fetching profile for user {}", userId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception fetching profile for user " + userId);
    }
  }

  /**
   * Write the {@link com.continuuity.loom.user.Profile} for a specific user.
   *
   * @param request The request to write the user profile.
   * @param responder Responder for sending the response.
   * @param userId Id of the user whose profile will be written.
   * @throws Exception
   */
  @PUT
  @Path("/profiles/{user-id}")
  public void putUserProfile(HttpRequest request, HttpResponder responder, @PathParam("user-id") String userId) {
    String requestedUserId = authorizeUser(request, responder, userId);
    if (requestedUserId == null) {
      return;
    }

    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    Profile profile = null;
    try {
      profile = GSON.fromJson(reader, Profile.class);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid user profile.");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.error("Exception closing reader after writing profile for user {}", userId, e);
      }
    }
    if (profile == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "Invalid user profile.");
      return;
    }

    try {
      userStore.writeProfile(profile);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (Exception e) {
      LOG.error("Exception writing profile for user {}", userId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception writing profile for user " + userId);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.error("Exception closing reader after writing profile for user {}", userId, e);
      }
    }
  }

  /**
   * Delete the {@link com.continuuity.loom.user.Profile} for a specific user.
   *
   * @param request The request to delete the user profile.
   * @param responder Responder for sending the response.
   * @param userId Id of the user whose profile will be deleted.
   * @throws Exception
   */
  @DELETE
  @Path("/profiles/{user-id}")
  public void deleteUserProfile(HttpRequest request, HttpResponder responder, @PathParam("user-id") String userId) {
    String requestedUserId = authorizeUser(request, responder, userId);
    if (requestedUserId == null) {
      return;
    }

    try {
      userStore.deleteProfile(requestedUserId);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (Exception e) {
      LOG.error("Exception deleting profile for user {}", userId, e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Exception deleting profile for user " + userId);
    }
  }

  /**
   * Validates that the user making the request has permission to access the user being requested.
   */
  private String authorizeUser(HttpRequest request, HttpResponder responder, String requestedUserId) {
    String userId = getAndAuthenticateUser(request, responder);
    if (userId != null && !userId.equals(requestedUserId)) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "Unauthorized to get profile for user " + requestedUserId);
      return null;
    }
    return userId;
  }
}
