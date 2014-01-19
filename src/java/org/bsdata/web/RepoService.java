/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsdata.web;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.bsdata.constants.DataConstants;
import org.bsdata.dao.GitHubDao;
import org.bsdata.model.Repository;
import org.bsdata.utils.Utils;

/**
 * REST Web Service
 *
 * @author Jonskichov
 */
@Path("repos")
public class RepoService {
  
    private static final Logger logger = Logger.getLogger("org.bsdata");
    private GitHubDao dao;

    /**
     * Creates a new instance of RepoService
     */
    public RepoService() {
        try {
            dao = new GitHubDao();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Could not load GitHubDao: {0}", e.getMessage());
            throw new RuntimeException("Could not load GitHubDao: " + e.getMessage());
        }
    }

    @GET
    @Path("/{repoName}/{fileName}")
    @Produces("application/octet-stream")
    public Response getFile(
            @PathParam("repoName") String repoName, 
            @PathParam("fileName") String fileName,
            @Context HttpServletRequest request) {
        
        if (StringUtils.isEmpty(repoName)) {
            // No repo name
        }
        if (StringUtils.isEmpty(fileName)) {
            // No filename
        }
        
        String mimeType;
        if (Utils.isRosterPath(fileName)) {
            mimeType = DataConstants.ROSTER_FILE_MIME_TYPE;
        }
        else if (Utils.isCataloguePath(fileName)) {
            mimeType = DataConstants.CATALOGUE_FILE_MIME_TYPE;
        }
        else if (Utils.isGameSytstemPath(fileName)) {
            mimeType = DataConstants.GAME_SYSTEM_FILE_MIME_TYPE;
        }
        else if (Utils.isIndexPath(fileName)) {
            mimeType = DataConstants.INDEX_FILE_MIME_TYPE;
        }
        else {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        HashMap<String, byte[]> repoData;
        try {
            String baseUrl = Utils.getBaseUrl(request.getRequestURL().toString());
            repoData = dao.getRepoFiles(repoName, baseUrl, null);
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load repo data: {0}", e.getMessage());
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        byte[] fileData = repoData.get(fileName);
        if (fileData == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        return Response.ok()
                .header("Content-Disposition", "attachment;filename=\"" + fileName + "\"")
                .entity(fileData)
                .type(mimeType)
                .build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getRepositories(@Context HttpServletRequest request) {
        List<Repository> repositories;
        try {
            repositories = dao.getRepositories(request.getRequestURL().toString() + "/");
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load repo list: {0}", e.getMessage());
            e.printStackTrace();
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        
        Gson gson = new Gson();
        return gson.toJson(repositories);
    }
}
