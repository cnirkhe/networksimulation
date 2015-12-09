package com.ricketts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * The InputParser is used to read the JSON definition for the parameters of the project and convert it to objects.
 */
public class InputParser {

    /**
     * Java Library Object to ease parsing
     */
    private JSONObject jsonObject;

    public InputParser() {}

    /**
     * Reads file and generates equivalent string.
     * @param filename include directory
     * @return string equivalent
     */
    public static String readFile(String filename) {
        String result = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Construct the JSON object to assist with parsing
     * @param fileLocation Location of JSON object in relation to
     */
    public void parseJSON(String fileLocation) {
        this.jsonObject = null;
        try {
            String jsonData = readFile(fileLocation);
            this.jsonObject = new JSONObject(jsonData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Given a HashMap of Link Ids to Links and the JSON definition, Extract information about the Hosts
     * @param linkMap HashMap of Link Ids to Links
     * @return ArrayList of Hosts
     */
    public ArrayList<Host> extractHosts(HashMap<Integer, Link> linkMap, int protocol) {
        ArrayList<Host> output = new ArrayList<>();

        try {
            JSONArray hostArray = jsonObject.getJSONObject("network").getJSONArray("hosts");

            for (int i = 0; i < hostArray.length(); ++i) {
                JSONObject hostJson = hostArray.getJSONObject(i);

                String address = hostJson.getString("address");
                int linkId = hostJson.getInt("link");
                //Get Link using map
                Link link = linkMap.get(linkId);
                Host host = new Host(address, link, protocol);
                output.add(host);
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }

    /**
     * Given a HashMap of Link Ids to Links and the JSON definition, Extract information about the Hosts
     * @param linkMap HashMap of Link Ids to Links
     * @return ArrayList of Hosts
     */
    public ArrayList<Router> extractRouters(HashMap<Integer, Link> linkMap) {
        ArrayList<Router> output = new ArrayList<>();

        try {
            JSONArray routerArray = jsonObject.getJSONObject("network").getJSONArray("routers");

            for (int i = 0; i < routerArray.length(); ++i) {
                JSONObject routerJson = routerArray.getJSONObject(i);

                String address = routerJson.getString("address");

                JSONArray linksJson = routerJson.getJSONArray("links");
                ArrayList<Link> links = new ArrayList<>(linksJson.length());

                for(int j = 0; j < linksJson.length(); ++j) {
                    int linkId = linksJson.getInt(j);
                    //Get Link using map
                    Link link = linkMap.get(linkId);
                    links.add(link);
                }

                Router router = new Router(address, links);
                output.add(router);
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }

    /**
     * Using the JSON definition, produce ArrayList of Links
     * @return ArrayList of Links
     */
    public ArrayList<Link> extractLinks(String filename, int protocol) {
        ArrayList<Link> output = new ArrayList<>();
        try {
            JSONArray linkArray = jsonObject.getJSONObject("network").getJSONArray("links");
            for (int i = 0; i < linkArray.length(); ++i) {
                JSONObject linkJson = linkArray.getJSONObject(i);
                int id = linkJson.getInt("id");
                int capacity = (int) (linkJson.getDouble("capacity") * 1048.576);
                int transmissionDelay = linkJson.getInt("transmissionDelay");
                int buffer = linkJson.getInt("bufferSize") * 8192;
                boolean graph = linkJson.getBoolean("graph");
                // add in left node and right node
                output.add(new Link(id, capacity, transmissionDelay, buffer, filename, graph));
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }

    /* Given an address book and a list of packets, construct all the flows in
     * the network.
     */
    public ArrayList<Flow> extractFlows(HashMap<String, Node> addressBook, String filename, int protocol) {
        ArrayList<Flow> output = new ArrayList<>();
        try {
            JSONArray flowArray = jsonObject.getJSONObject("network").getJSONArray("flows");
            for (int i = 0; i < flowArray.length(); ++i) {
                JSONObject flowJson = flowArray.getJSONObject(i);
                int id = flowJson.getInt("id");
                String sourceId = flowJson.getString("source");
                Host source = (Host) addressBook.get(sourceId);
                String destinationId = flowJson.getString("destination");
                Host destination = (Host) addressBook.get(destinationId);
                int dataAmount = flowJson.getInt("dataAmount") * 8388608;
                int startTime = flowJson.getInt("startTime");
                output.add(new Flow(id, source, destination, dataAmount, startTime, filename, protocol));
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }

    public static HashMap<Integer, Link> makeLinkMap(ArrayList<Link> links) {
        HashMap<Integer, Link> output = new HashMap<>();
        for (Link link : links) {
            output.put(link.getID(), link);
        }
        return output;
    }

    // Create a hashmap of (flow_id, flow) pairs
    public static HashMap<Integer, Flow> makeFlowMap(ArrayList<Flow> flows) {
        HashMap<Integer, Flow> output = new HashMap<>();
        for (Flow f : flows) {
            output.put(f.getID(), f);
        }
        return output;
    }


    /**
     * Produce a addressbook of addresses to Nodes
     * @param nodes
     * @return
     */
    public static HashMap<String, Node> makeNodeMap(ArrayList<Node> nodes) {
        HashMap<String, Node> output = new HashMap<>();
        for (Node node : nodes) {
            output.put(node.getAddress(), node);
        }
        return output;
    }

    private static void setNodeOnLink(Link link, Node node)
    {
        if (link.getLeftNode() == null) {
            link.setLeftNode(node);
        } else if (link.getRightNode() == null) {
            link.setRightNode(node);
        } else {
            System.out.println("Bad Network Definition.");
        }
    }

    public static void addNodesToLinks(ArrayList<Node> nodes)
    {
        for(Node node : nodes) {
            if(node instanceof Host) {
                Host host = (Host) node;
                Link link = host.getLink();
                setNodeOnLink(link, node);
            }
            else if (node instanceof Router) {
                Router router = (Router) node;
                for(Link link : router.getLinks()) {
                    setNodeOnLink(link, node);
                }
            }
            else {
                System.out.println("Bad Network Definition.");
            }
        }
    }
}