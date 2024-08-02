/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.ui.branding.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.ui.branding.api.UIBrandingApi;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;
import it.acsoftware.hyperiot.ui.branding.model.UIBrandingConstants;
import it.acsoftware.hyperiot.ui.branding.model.view.Isolated;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Collection;


/**
 * @author Aristide Cittadino UIBranding rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/ui-branding", info = @Info(description = "HyperIoT UIBranding API", version = "2.0.0", title = "hyperiot UIBranding", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {@ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/ui-branding", produces = "application/json")
@Component(service = UIBrandingRestApi.class, property = {"service.exported.interfaces=it.acsoftware.hyperiot.ui.branding.service.rest.UIBrandingRestApi", "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/ui-branding", "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter", "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class UIBrandingRestApi extends HyperIoTBaseEntityRestApi<UIBranding> {
    private UIBrandingApi entityService;
    private HUserSystemApi hUserSystemApi;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Role Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/uibranding/module/status");
        return Response.ok("UIBranding Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<UIBranding> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = UIBrandingApi.class)
    protected void setEntityService(UIBrandingApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    @Reference
    public void sethUserSystemApi(HUserSystemApi hUserSystemApi) {
        this.hUserSystemApi = hUserSystemApi;
    }

    /**
     * Service get current ui branding for the logged user
     *
     * @return the UIBranding saved
     */
    @GET
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/ui-branding", notes = "Service for getting the current ui branding entity", httpMethod = "GET", produces = "application/json", consumes = "multipart/form-data", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"), @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"), @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(Isolated.class)
    public Response getUIBranding() {
        getLog().debug("In Rest Service GET /hyperiot/uibranding");
        UIBranding uiBranding = findUIBranding();
        if (uiBranding != null) return Response.ok(uiBranding).build();
        return Response.status(404).build();
    }

    /**
     * Service updates a UIBranding
     *
     * @return the UIBranding updated
     */
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/ui-branding", notes = "Service for updating a uibranding entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"), @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"), @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(Isolated.class)
    public Response updateUIBranding(@ApiParam(value = "name the user want to visualize", name = "name", type = "String") @Multipart(value = "name") String name,
                                     @ApiParam(value = "chosen color scheme", name = "colorScheme", type = "String") @Multipart(value = "colorScheme") String colorScheme,
                                     @ApiParam(value = "logo image ", name = "logoFile", type = "String") @Multipart(value = "logo", type = "image/*") Attachment logoFile,
                                     @ApiParam(value = "favicon image", name = "faviconFile", type = "String") @Multipart(value = "favicon", type = "image/*") Attachment faviconFile) {
        getLog().debug("In Rest Service PUT /hyperiot/uibrandings \n Body: {}", name + " - " + colorScheme);
        boolean isNew = false;
        UIBranding currentBranding = findUIBranding();
        if (currentBranding == null) {
            currentBranding = new UIBranding();
            isNew = true;
        }
        currentBranding.setColorScheme(colorScheme);
        currentBranding.setName(name);
        enrichUIBranding(currentBranding, logoFile, faviconFile);
        if (!isNew) return this.update(currentBranding);
        else return this.save(currentBranding);
    }

    /**
     * Service updates a UIBranding
     *
     * @return the UIBranding updated
     */
    @DELETE
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/ui-branding", notes = "Service for resetting a uibranding entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"), @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"), @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(Isolated.class)
    public Response updateUIBranding() {
        getLog().debug("In Rest Service PUT /hyperiot/uibrandings/reset \n Body:");
        UIBranding currentBranding = findUIBranding();
        if (currentBranding != null) {
            this.remove(currentBranding.getId());
            return Response.ok().build();
        }
        return Response.status(404).build();
    }

    private void enrichUIBranding(UIBranding uiBranding, Attachment logoTempFile, Attachment faviconTempFile) {
        HUser huser = this.hUserSystemApi.find(getHyperIoTContext().getLoggedEntityId(), getHyperIoTContext());
        uiBranding.setHuser(huser);
        try {
            String basicPath = UIBrandingConstants.ASSET_FOLDER + File.separator + getHyperIoTContext().getLoggedEntityId();
            File basicPathFile = new File(basicPath);
            if (!basicPathFile.exists()) basicPathFile.mkdirs();

            if (faviconTempFile != null) {
                String faviconExtension = "";
                int index = faviconTempFile.getContentDisposition().getFilename().lastIndexOf(".");
                if (index > 0)
                    faviconExtension = faviconTempFile.getContentDisposition().getFilename().substring(index);
                String faviconPath = basicPath + File.separator + "favicon" + faviconExtension;
                faviconTempFile.transferTo(new File(faviconPath));
                uiBranding.setFaviconPath(faviconPath);
            }
            if (logoTempFile != null) {
                String logoExtension = "";
                int index = logoTempFile.getContentDisposition().getFilename().lastIndexOf(".");
                if (index > 0)
                    logoExtension = logoTempFile.getContentDisposition().getFilename().substring(index);
                String logoPath = basicPath + File.separator + "logo" + logoExtension;
                logoTempFile.transferTo(new File(logoPath));
                uiBranding.setLogoPath(logoPath);
            }
        } catch (IOException e) {
            throw new HyperIoTRuntimeException("Cannot save uploaded logo images ");
        }
    }

    private UIBranding findUIBranding() {
        HyperIoTQuery q = HyperIoTQueryBuilder.newQuery().equals("huser.id", getHyperIoTContext().getLoggedEntityId());
        Collection<UIBranding> result = this.entityService.findAll(q, getHyperIoTContext());
        if (result != null && result.size() == 1) return result.stream().findFirst().get();
        return null;
    }
}
