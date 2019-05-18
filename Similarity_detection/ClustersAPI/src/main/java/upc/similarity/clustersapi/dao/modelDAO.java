package upc.similarity.clustersapi.dao;

import upc.similarity.clustersapi.entity.Model;
import upc.similarity.clustersapi.exception.NotFoundException;

import java.sql.SQLException;

public interface modelDAO {

    public void saveModel(String organization, Model model) throws SQLException;

    public Model getModel(String organization) throws SQLException, NotFoundException;

    public void saveResponse(String organizationId, String responseId) throws SQLException;

    public void createDatabase() throws SQLException;


}
