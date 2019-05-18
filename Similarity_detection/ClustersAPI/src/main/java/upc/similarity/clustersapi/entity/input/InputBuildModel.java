package upc.similarity.clustersapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import upc.similarity.clustersapi.entity.Requirement;

import java.io.Serializable;
import java.util.List;

public class InputBuildModel implements Serializable {

    @JsonProperty(value="requirements")
    private List<Requirement> requirements;

    public List<Requirement> getRequirements() {
        return requirements;
    }

}
