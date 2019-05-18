package upc.similarity.clustersapi.service;

import org.springframework.stereotype.Service;
import smile.math.Math;
import upc.similarity.clustersapi.dao.modelDAO;
import upc.similarity.clustersapi.dao.SQLiteDAO;
import upc.similarity.clustersapi.entity.*;
import upc.similarity.clustersapi.entity.input.InputBuildModel;
import upc.similarity.clustersapi.entity.input.InputComputeClusters;
import upc.similarity.clustersapi.entity.output.OutputComputeClusters;
import upc.similarity.clustersapi.exception.BadRequestException;
import upc.similarity.clustersapi.exception.InternalErrorException;
import upc.similarity.clustersapi.exception.NotFoundException;
import upc.similarity.clustersapi.util.CosineSimilarity;
import upc.similarity.clustersapi.util.Tfidf;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@Service("comparerService")
public class CompareServiceImpl implements CompareService {

    private static Double cutoffParameter=-1.0;
    private static String component = "Similarity-UPC";
    private static String status = "proposed";
    private static String dependency_type = "duplicates";
    private static int MAX_PAGE_DEPS = 20000;
    private modelDAO modelDAO = getValue();

    private modelDAO getValue() {
        try {
            return new SQLiteDAO();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void buildModel(boolean compare, String organization, InputBuildModel input) throws BadRequestException, InternalErrorException {
        show_time("start buildModel");
        if (input.getRequirements().size() < 10) throw new BadRequestException("At a minimum there must be 10 entry requirements");
        Tfidf tfidf = Tfidf.getInstance();
        Map<String, Integer> corpusFrequency = new HashMap<>();
        List<String> text = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        buildCorpus(compare,input.getRequirements(),text,ids);
        Map<String, Map<String, Double>> docs = tfidf.extractKeywords(text,ids,corpusFrequency);
        Model model = new Model(docs,corpusFrequency);
        saveModel(organization, model);
        show_time("finish buildModel");
    }

    @Override
    public OutputComputeClusters computeClusters(String organization, double input_threshold, InputComputeClusters input) throws BadRequestException, NotFoundException, InternalErrorException {
        if (input.getRequirements().size() < 10) throw new BadRequestException("At a minimum there must be 10 entry requirements");

        show_time("start computeClusters");

        CosineSimilarity cosineSimilarity = CosineSimilarity.getInstance();

        double MIN_VARIANCE = 0.05; // %
        int MAX_CLUSTER_SIZE = 25;
        int MAX_ITERATIONS = 3;
        double MAX_COR = input_threshold; // 0.15
        double PLUS_COR = input_threshold*2; //0.3
        double threshold = 0.13;

        Model model = null;
        try {
            model = modelDAO.getModel(organization);
        } catch (SQLException e) {
            throw new InternalErrorException("Error while loading the model from the database");
        }

        List<String> requirements = input.getRequirements();
        int max_size = requirements.size();
        double[][] distances = new double[max_size][max_size];
        double[] variances = new double[max_size];

        HashSet<Integer> reqs_to_delete = new HashSet<>();

        for (int i = 0; i < max_size; ++i) {
            String req1 = requirements.get(i);
            if (!model.getDocs().containsKey(req1)) throw new BadRequestException("The loaded model does not contain a requirement with id " + req1);
            double sum = 0;
            double sumSq = 0;
            double k = -1;
            int count_low = 0;
            for (int j = 0; j < max_size; ++j) {
                if (j == i) {
                    distances[i][j] = 1;
                    distances[j][i] = 1;
                } else {
                    if (j > i) {
                        String req2 = requirements.get(j);
                        if (!model.getDocs().containsKey(req2)) throw new BadRequestException("The loaded model does not contain a requirement with id " + req2);
                        double score = cosineSimilarity.cosineSimilarity(model.getDocs(), req1, req2);
                        if (score > threshold) ++count_low;
                        if (k == -1) k = score;
                        distances[i][j] = score;
                        distances[j][i] = score;
                        sum += score - k;
                        sumSq += (score - k) * (score - k);
                    } else {
                        double score = distances[i][j];
                        if (score > threshold) ++count_low;
                        if (k == -1) k = score;
                        sum += score - k;
                        sumSq += (score - k) * (score - k);
                    }
                }
            }
            double variance = (sumSq - (sum * sum)/max_size)/max_size;
            variances[i] = variance;
            //if (count_low < MIN_SIZE_CLUSTER || count_low > (MAX_SIZE_CLUSTER + 20)) reqs_to_delete.add(i);
        }

        reqs_to_delete.addAll(filter_per_variance(0.000005,MIN_VARIANCE,variances));

        HashMap<Integer, List<Integer>> clusters = new HashMap<>();
        HashMap<Integer, double[]> centroids = new HashMap<>();

        for (int i = 0; i < requirements.size(); ++i) {
            if (!reqs_to_delete.contains(i)) {
                List<Integer> aux = new ArrayList<>();
                aux.add(i);
                clusters.put(i, aux);
                centroids.put(i, distances[i]);
            }
        }

        boolean merged = true;
        while(merged){
            merged = false;
            for(int i = 0; i < clusters.size(); ++i){
                if (clusters.containsKey(i))
                    for(int j = i + 1; j < clusters.size(); ++j){
                        if (clusters.containsKey(j)) {
                            double cor = Math.cor(centroids.get(i), centroids.get(j));
                            if (cor > MAX_COR && (clusters.get(i).size() + clusters.get(j).size() <= MAX_CLUSTER_SIZE)) {
                                // re-compute centroid
                                double[] re_computed_centroid = re_compute_centroide(i, j, centroids);
                                centroids.put(i, re_computed_centroid);
                                List<Integer> aux = clusters.get(i);
                                aux.addAll(clusters.get(j));
                                clusters.put(i, aux);
                                clusters.remove(j);
                                centroids.remove(j);
                                merged = true;
                            }
                        }
                    }
            }
        }

        /*for (int i = 0; i < max_size; ++i) {
            if (clusters.containsKey(i)) {
                boolean correct = true;
                for (int j = i + 1; correct && j < max_size; ++j) {
                    if (clusters.containsKey(j)) {
                        double cor = Math.cor(distances[i], distances[j]);
                        if (cor > MAX_COR) {
                            if (variances[i] >= variances[j]) {
                                List<Integer> aux = clusters.get(i);
                                aux.addAll(clusters.get(j));
                                clusters.put(i, aux);
                                clusters.remove(j);
                            }
                            else {
                                correct = false;
                                List<Integer> aux = clusters.get(j);
                                aux.addAll(clusters.get(i));
                                clusters.put(j, aux);
                                clusters.remove(i);
                            }
                        }
                    }
                }
            }
        }

        int deleted_reqs = 0;

        for (int value: reqs_to_delete) {
            double max_cor = -1;
            int max_cluster = -1;
            Iterator it4 = clusters.entrySet().iterator();
            while (it4.hasNext()) {
                Map.Entry pair = (Map.Entry)it4.next();
                int id = (int) pair.getKey();
                double cor = Math.cor(distances[id],distances[value]);
                if (cor > MAX_COR && cor > max_cor) {
                    max_cor = cor;
                    max_cluster = id;
                }
            }
            if (max_cluster != -1) {
                List<Integer> aux = clusters.get(max_cluster);
                aux.add(value);
                clusters.put(max_cluster, aux);
            } else ++deleted_reqs;
        }

        for (int i = 0; i < MAX_ITERATIONS; ++i) {
            loop_clusters(distances, variances, clusters, MAX_CLUSTER_SIZE, MAX_COR, PLUS_COR);
        }*/


        int count_cluster_requirements = 0;
        int count_low = 0;
        int count_clusters = 0;

        List<Dependency> result = new ArrayList<>();
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            int id = (int) pair.getKey();
            List<Integer> cluster_values = (List<Integer>) pair.getValue();
            //System.out.println(id + " = " + cluster_values.size());
            if (cluster_values.size() > 1) {
                count_cluster_requirements += cluster_values.size();
                ++count_clusters;

                String id1 = requirements.get(id);
                for (int value: cluster_values) {
                    String id2 = requirements.get(value);
                    if (!id1.equals(id2)) result.add(new Dependency(id1,id2,status,dependency_type,component));
                }
            }
            else ++count_low;
            it.remove(); // avoids a ConcurrentModificationException
        }

        /*System.out.println("totals reqs: " + requirements.size());
        System.out.println("total clusters: " + count_clusters);
        System.out.println("reqs in big clusters: " + count_cluster_requirements);
        System.out.println("reqs in little clusters: " + count_low);
        System.out.println("deleted reqs: " + deleted_reqs);*/

        show_time("finish computeClusters");

        return new OutputComputeClusters(result);
    }

    @Override
    public void clearDatabase() throws InternalErrorException {
        try {
            File file = new File("../models.db");
            if (!file.delete()) throw new InternalErrorException("Database does not exist");
            file = new File("../models.db");
            if (!file.createNewFile()) throw new InternalErrorException("Error while clearing the database");
            modelDAO.createDatabase();
        } catch (IOException e) {
            throw new InternalErrorException("IO error while clearing the database");
        } catch (SQLException e) {
            throw new InternalErrorException("SQL error while clearing the database");
        }
    }



    /*
    auxiliary operations
     */

    private double[] re_compute_centroide(int i, int j, HashMap<Integer, double[]> centroids) {
        double[] new_centroide = new double[centroids.get(i).length];
        double[] centroid1 = centroids.get(i);
        double[] centroid2 = centroids.get(j);
        int p = centroid2.length / (centroid1.length + centroid2.length);
        for (int k = 0; k < centroid1.length; k++) {
            new_centroide[k] = (1 -  p)*centroid1[k] + centroid2[k]*p;
        }
        return  new_centroide;
    }

    private void loop_clusters(double[][] matrix, double[] variances, HashMap<Integer,List<Integer>> clusters, int MAX_CLUSTER_SIZE, double MAX_COR, double PLUS_COR) {
        HashMap<Integer,List<Integer>> total_new_cluster = new HashMap<>();
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            int id = (int) pair.getKey();
            List<Integer> cluster_values = (List<Integer>) pair.getValue();
            int size = cluster_values.size();
            if (size > MAX_CLUSTER_SIZE) {
                clusters.put(id, new ArrayList<>());
                List<Integer> cluster_requirements = new ArrayList<>(cluster_values);
                List<Integer> cluster_requirements_to_delete = new ArrayList<>();
                for (int value: cluster_requirements) {
                    double max_cor = -1;
                    int max_cluster = -1;
                    Iterator it2 = clusters.entrySet().iterator();
                    while (it2.hasNext()) {
                        Map.Entry pair2 = (Map.Entry) it2.next();
                        int id_cluster = (int) pair2.getKey();
                        double cor = Math.cor(matrix[id_cluster],matrix[value]);
                        if (cor > PLUS_COR && cor > max_cor) {
                            max_cor = cor;
                            max_cluster = id_cluster;
                        }
                    }
                    if (max_cluster != -1) {
                        cluster_requirements_to_delete.add(value);
                        List<Integer> aux = clusters.get(max_cluster);
                        aux.add(value);
                        clusters.put(max_cluster,aux);
                    }
                }
                cluster_requirements.removeAll(cluster_requirements_to_delete);
                HashMap<Integer,List<Integer>> new_clusters = compute_clusters_plus(matrix,PLUS_COR,variances,cluster_requirements);
                total_new_cluster.putAll(new_clusters);
            }
        }

        clusters.putAll(total_new_cluster);
    }

    private HashMap<Integer,List<Integer>> compute_clusters_plus(double[][] matrix, double MAX_COR, double[] variances, List<Integer> requirements) {
        HashMap<Integer,List<Integer>> clusters = new HashMap<>();

        for (int value: requirements) {
            List<Integer> aux = new ArrayList<>();
            aux.add(value);
            clusters.put(value, aux);
        }

        for (int i = 0; i < requirements.size(); ++i) {
            int value1 = requirements.get(i);
            if (clusters.containsKey(value1)) {
                boolean correct = true;
                for (int j = i + 1; correct && j < requirements.size(); ++j) {
                    int value2 = requirements.get(j);
                    if (clusters.containsKey(value2)) {
                        double cor = Math.cor(matrix[value1], matrix[value2]);
                        if (cor > MAX_COR) {
                            if (variances[value1] >= variances[value2]) {
                                List<Integer> aux = clusters.get(value1);
                                aux.addAll(clusters.get(value2));
                                clusters.put(value1, aux);
                                clusters.remove(value2);
                            }
                            else {
                                correct = false;
                                List<Integer> aux = clusters.get(value2);
                                aux.addAll(clusters.get(value1));
                                clusters.put(value2, aux);
                                clusters.remove(value1);
                            }
                        }
                    }
                }
            }
        }
        return clusters;
    }

    private void saveModel(String organization, Model model) throws InternalErrorException {
        try {
            modelDAO.saveModel(organization, model);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new InternalErrorException("Error while saving the new model to the database");
        }
    }

    private void show_time(String text) {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        System.out.println(text + " -- " + hour + ":" + minute + "  " + month + "/" + day + "/" + year);
    }

    private void buildCorpus(boolean compare, List<Requirement> requirements, List<String> array_text, List<String> array_ids) throws BadRequestException {
        for (Requirement requirement: requirements) {
            if (requirement.getId() == null) throw new BadRequestException("There is a requirement without id.");
            array_ids.add(requirement.getId());
            String text = "";
            if (requirement.getName() != null) text = text.concat(clean_text(requirement.getName(),1) + ". ");
            if (compare && (requirement.getText() != null)) text = text.concat(clean_text(requirement.getText(),2));
            array_text.add(text);
        }
    }

    private String clean_text(String text, int clean) {
        text = text.replaceAll("(\\{.*?})", " code ");
        text = text.replaceAll("[.$,;\\\"/:|!?=%,()><_0-9\\-\\[\\]{}']", " ");
        String[] aux2 = text.split(" ");
        String result = "";
        for (String a : aux2) {
            if (a.length() > 1) {
                result = result.concat(" " + a);
            }
        }
        return result;
    }

    private List<Integer> filter_per_variance(double threshold, double MIN_VARIANCE, double[] variances) {
        List<Integer> result = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < variances.length; ++i) {
            double value = variances[i];
            if (value < threshold) {
                ++count;
                result.add(i);
            }
        }
        double aux = (double)count/(double)variances.length;
        if (aux > MIN_VARIANCE) return result;
        else return filter_per_variance(threshold+0.0000005,MIN_VARIANCE,variances);
    }
}
