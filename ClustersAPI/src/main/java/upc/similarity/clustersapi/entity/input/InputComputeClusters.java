package upc.similarity.clustersapi.entity.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class InputComputeClusters implements Serializable {

    @JsonProperty(value="requirements")
    private List<String> requirements;

    public List<String> getRequirements() {
        return requirements;
    }
}
