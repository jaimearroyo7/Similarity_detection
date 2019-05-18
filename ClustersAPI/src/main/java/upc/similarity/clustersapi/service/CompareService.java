package upc.similarity.clustersapi.service;


import java.util.List;

import upc.similarity.clustersapi.entity.Dependency;
import upc.similarity.clustersapi.entity.input.InputBuildModel;
import upc.similarity.clustersapi.entity.input.InputComputeClusters;
import upc.similarity.clustersapi.entity.output.OutputComputeClusters;
import upc.similarity.clustersapi.exception.BadRequestException;
import upc.similarity.clustersapi.exception.InternalErrorException;
import upc.similarity.clustersapi.exception.NotFoundException;

public interface CompareService {

    public void buildModel(boolean compare, String organization, InputBuildModel input) throws BadRequestException, InternalErrorException;

    public OutputComputeClusters computeClusters(String organization, double threshold, InputComputeClusters input) throws BadRequestException, NotFoundException, InternalErrorException;

    public void clearDatabase() throws InternalErrorException;

}