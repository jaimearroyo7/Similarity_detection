package upc.similarity.clustersapi.entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//Class used to represent dependencies between requirements
public class Dependency implements Serializable {

    private static ObjectMapper mapper = new ObjectMapper();

    @JsonProperty(value="fromid")
    private String fromid;
    @JsonProperty(value="toid")
    private String toid;
    @JsonProperty(value="status")
    private String status;
    @JsonProperty(value="dependency_type")
    private String dependency_type;
    @JsonProperty(value="description")
    private List<String> description;

    public Dependency() {
        description = new ArrayList<>();
    }

    public Dependency(String fromid, String toid, String status, String dependency_type, String component) {
        this.fromid = fromid;
        this.toid = toid;
        this.status = status;
        this.dependency_type = dependency_type;
        this.description = new ArrayList<>();
        description.add(component);
    }

    /*
    Get
     */

    public String getFromid() {
        return fromid;
    }

    public String getToid() {
        return toid;
    }

    public String getStatus() {
        return status;
    }

    public String getDependency_type() {
        return dependency_type;
    }

    public List<String> getDescription() {
        return description;
    }

    /*
    Set
     */

    public void setFromid(String fromid) {
        this.fromid = fromid;
    }

    public void setToid(String toid) {
        this.toid = toid;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDependency_type(String dependency_type) {
        this.dependency_type = dependency_type;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    /*
    Auxiliary operations
     */

    public String print_json() {
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonInString;
    }

    public JSONObject toJSON() {
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject(jsonInString);
    }

    @Override
    public String toString() {
        return "Dependency between requirement " + fromid + "and requirement " + toid + " with type " + dependency_type + " and status " + status + ".";
    }
}
