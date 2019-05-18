package upc.similarity.clustersapi.exception;

public class NotFoundException extends ComponentException {

    public NotFoundException(String message) {
        super(message);
        this.error = "Not found";
        this.status = 404;
    }
}