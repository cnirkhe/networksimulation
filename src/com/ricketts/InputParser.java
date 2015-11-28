package com.ricketts;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

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
    public ArrayList<Host> extractHosts(HashMap<Integer, Link> linkMap) {
        ArrayList<Host> output = new ArrayList<>();

        try {
            JSONArray hostArray = jsonObject.getJSONObject("network").getJSONArray("hosts");

            for (int i = 0; i < hostArray.length(); ++i) {
                JSONObject hostJson = hostArray.getJSONObject(i);

                int address = hostJson.getInt("address");
                int linkId = hostJson.getInt("link");
                //Get Link using map
                Link link = linkMap.get(linkId);
                Host host = new Host(address, link);
                output.add(host);
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
    public ArrayList<Link> extractLinks(String filename) {
        ArrayList<Link> output = new ArrayList<>();
        try {
            JSONArray linkArray = jsonObject.getJSONObject("network").getJSONArray("links");
            for (int i = 0; i < linkArray.length(); ++i) {
                JSONObject linkJson = linkArray.getJSONObject(i);
                int id = linkJson.getInt("id");
                int capacity = linkJson.getInt("capacity");
                int transmissionDelay = linkJson.getInt("transmissionDelay");
                int buffer = linkJson.getInt("bufferSize");
                // add in left node and right node
                output.add(new Link(id, capacity, transmissionDelay, buffer, filename));
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }

    /* Given an address book and a list of packets, construct all the flows in
     * the network.
     */
    public ArrayList<Flow> extractFlows(HashMap<Integer, Node> addressBook, String filename) {
        ArrayList<Flow> output = new ArrayList<>();
        try {
            JSONArray flowArray = jsonObject.getJSONObject("network").getJSONArray("flows");
            for (int i = 0; i < flowArray.length(); ++i) {
                JSONObject flowJson = flowArray.getJSONObject(i);
                int id = flowJson.getInt("id");
                int sourceId = flowJson.getInt("source");
                Host source = (Host) addressBook.get(sourceId);
                int destinationId = flowJson.getInt("destination");
                Host destination = (Host) addressBook.get(destinationId);
                int dataAmount = flowJson.getInt("dataAmount");
                int startTime = flowJson.getInt("startTime");
                output.add(new Flow(id, source, destination, dataAmount, startTime, filename));
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }

    // Given a hashmaps of links and ids, extract the router data, including
    // making a list of links it's attached to.
    /*
    public ArrayList<Router> extractRouters(HashMap<Integer, Link> links) {
        ArrayList<Router> output = new ArrayList<>();
        try {
            JSONArray routerArray = jsonObject.getJSONObject("network").getJSONArray("routers");
            for (int i = 0; i < routerArray.length(); ++i) {
                JSONObject routerJson = routerArray.getJSONObject(i);
                int address = routerJson.getInt("address");
                JSONArray linkArray = routerJson.getJSONArray("links");
                ArrayList<Link> linkList = new ArrayList<Link>();
                for (int j = 0; j < linkArray.length(); ++j) {
                    linkList.add(links.get(linkArray.getInt(j)));
                }
                output.add(new Router(address, linkList));
            }
        } catch (JSONException e) {
            System.out.println(e);
        }
        return output;
    }
    */

    // Attach links to their left and right nodes..... hmmm

    // Create a hashmap of (id, link) pairs
    public HashMap<Integer, Link> makeLinkMap(ArrayList<Link> links) {
        HashMap<Integer, Link> output = new HashMap<>();
        for (Link link : links) {
            output.put(link.getID(), link);
        }
        return output;
    }

    // Create a hashmap of (flow_id, flow) pairs
    public HashMap<Integer, Flow> makeFlowMap(ArrayList<Flow> flows) {
        HashMap<Integer, Flow> output = new HashMap<>();
        for (Flow f : flows) {
            output.put(f.getID(), f);
        }
        return output;
    }

    // Create an address book of routers and hosts.
    /*
    public HashMap<Integer, Node> makeNodeMap(ArrayList<Router> routers,
                                              ArrayList<Host> hosts) {
        HashMap<Integer, Node> output = new HashMap<>();
        for (Router router : routers) {
            output.put(router.getAddress(), router);
        }
        for (Host host : hosts) {
            output.put(host.getAddress(), host);
        }
        return output;
    }
    */

    // temporary: addressbook of just hosts
    public HashMap<Integer, Node> makeNodeMap(ArrayList<Host> hosts) {
        HashMap<Integer, Node> output = new HashMap<>();
        for (Host host : hosts) {
            output.put(host.getAddress(), host);
        }
        return output;
    }
}