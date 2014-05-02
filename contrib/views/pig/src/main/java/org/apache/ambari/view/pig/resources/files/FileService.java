/**
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

package org.apache.ambari.view.pig.resources.files;

import com.google.inject.Inject;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.services.BaseService;
import org.apache.ambari.view.pig.utils.FilePaginator;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * File access resource
 * API:
 * GET /:path
 *      read entire file
 * POST /
 *      create new file
 *      Required: filePath
 *      file should not already exists
 * PUT /:path
 *      update file content
 */
public class FileService extends BaseService {
    @Inject
    ViewResourceHandler handler;

    protected final static Logger LOG =
            LoggerFactory.getLogger(FileService.class);

    /**
     * Get single item
     */
    @GET
    @Path("{filePath:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFile(@PathParam("filePath") String filePath, @QueryParam("page") Long page) throws IOException, InterruptedException {
        LOG.debug("Reading file " + filePath);
        try {
            FilePaginator paginator = new FilePaginator(filePath, context);

            if (page == null)
                page = 0L;

            FileResource file = new FileResource();
            file.filePath = filePath;
            file.fileContent = paginator.readPage(page);
            file.hasNext = paginator.pageCount() > page + 1;
            file.page = page;
            file.pageCount = paginator.pageCount();

            JSONObject object = new JSONObject();
            object.put("file", file);
            return Response.ok(object).status(200).build();
        } catch (FileNotFoundException e) {
            return notFoundResponse(e.toString());
        } catch (IllegalArgumentException e) {
            return badRequestResponse(e.toString());
        }
    }

    /**
     * Delete single item
     */
    @DELETE
    @Path("{filePath:.*}")
    public Response deleteFile(@PathParam("filePath") String filePath) throws IOException, InterruptedException {
        LOG.debug("Deleting file " + filePath);
        if (getHdfsApi().delete(filePath, false)) {
            return Response.status(204).build();
        }
        return notFoundResponse("FileSystem.delete returned false");
    }

    /**
     * Update item
     */
    @PUT
    @Path("{filePath:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateFile(FileResourceRequest request,
                               @PathParam("filePath") String filePath) throws IOException, InterruptedException {
        LOG.debug("Rewriting file " + filePath);
        FSDataOutputStream output = getHdfsApi().create(filePath, true);
        output.writeBytes(request.file.fileContent);
        output.close();
        return Response.status(204).build();
    }

    /**
     * Create script
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFile(FileResourceRequest request,
                               @Context HttpServletResponse response, @Context UriInfo ui)
            throws IOException, InterruptedException {
        LOG.debug("Creating file " + request.file.filePath);
        try {
            FSDataOutputStream output = getHdfsApi().create(request.file.filePath, false);
            if (request.file.fileContent != null) {
                output.writeBytes(request.file.fileContent);
            }
            output.close();
        } catch (FileAlreadyExistsException e) {
            return badRequestResponse(e.toString());
        }
        response.setHeader("Location",
                String.format("%s/%s", ui.getAbsolutePath().toString(), request.file.filePath));
        return Response.status(204).build();
    }

    public static class FileResourceRequest {
        public FileResource file;
    }
}
