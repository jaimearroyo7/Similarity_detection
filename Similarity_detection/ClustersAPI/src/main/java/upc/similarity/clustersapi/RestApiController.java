package upc.similarity.clustersapi;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import upc.similarity.clustersapi.entity.input.InputBuildModel;
import upc.similarity.clustersapi.entity.input.InputComputeClusters;
import upc.similarity.clustersapi.exception.ComponentException;
import upc.similarity.clustersapi.service.CompareService;

import java.util.LinkedHashMap;

@RestController
@RequestMapping(value = "/upc/Compare")
public class RestApiController {

    @Autowired
    CompareService compareService;

    @PostMapping(value = "/BuildModel")
    @ApiOperation(value = "Builds a model with the input requirements", notes = "Builds a model with the entry requirements. " +
            "The generated model is assigned to the specified organization and stored in an internal database. Each organization" +
            " only can have one model at a time. If at the time of generating a new model the corresponding organization already has" +
            " an existing model, it is replaced by the new one.")
    @ApiResponses(value = {
            @ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request")})
    public ResponseEntity<?> buildModel(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                        @ApiParam(value="Use text attribute?", required = false, example = "true") @RequestParam(value = "compare",required = false) boolean compare,
                                        @RequestBody InputBuildModel input) {
        try {
            compareService.buildModel(compare,organization,input);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @PostMapping(value = "/ComputeClusters")
    @ApiOperation(value = "Generates a set of clusters", notes = "Generates a set of clusters with the input requirements. Receives an array with the requirements ids and returns an array with " +
            "the resulting clusters.")
    @ApiResponses(value = {
            @ApiResponse(code=200, message = "OK"),
            @ApiResponse(code=400, message = "Bad request"),
            @ApiResponse(code=404, message = "Not found")})
    public ResponseEntity<?> computeClusters(@ApiParam(value="Organization", required = true, example = "UPC") @RequestParam("organization") String organization,
                                             @ApiParam(value="Double between 0 and 1 that establishes the minimum similarity score that the added requirements should have", required = true, example = "0.1") @RequestParam("threshold") double threshold,
                                             @RequestBody InputComputeClusters input) {
        try {
            return new ResponseEntity<>(compareService.computeClusters(organization,threshold,input),HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }

    @DeleteMapping(value = "/ClearDatabase")
    @ApiOperation(value = "Deletes all data from the database", notes = "Deletes all data from the database.")
    @ApiResponses(value = {
            @ApiResponse(code=200, message = "OK")})
    public ResponseEntity<?> clearDatabase() {
        try {
            compareService.clearDatabase();
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (ComponentException e) {
            return getComponentError(e);
        }
    }


    private ResponseEntity<?> getComponentError(ComponentException e) {
        if (e.getStatus() == 500) e.printStackTrace();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("status", e.getStatus()+"");
        result.put("error", e.getError());
        result.put("message", e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.valueOf(e.getStatus()));
    }

}