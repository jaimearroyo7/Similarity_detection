package upc.similarity.clustersapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

//Class used to represent requirements
public class Requirement implements Serializable {

    @JsonProperty(value="id")
    private String id;
    @JsonProperty(value="name")
    private String name;
    @JsonProperty(value="text")
    private String text;


    public Requirement() {}

    /*
    Get
     */

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    /*
    Set
     */

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    /*
    Auxiliary operations
     */

    public boolean isOK() {
        if (id == null) return false;
        else return true;
    }

    @Override
    public String toString() {
        return "Requirement with id " + id + ".";
    }
}
