package upc.similarity.clustersapi.entity.output;

import upc.similarity.clustersapi.entity.Dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OutputComputeClusters implements Serializable {

    private List<Dependency> dependencies;

    public OutputComputeClusters() {
        this.dependencies = new ArrayList<>();
    }

    public OutputComputeClusters(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
