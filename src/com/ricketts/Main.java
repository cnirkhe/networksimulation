package com.ricketts;

import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args)
    {
        /**
         * For now I'm going to artifically construct the Network
         * TODO adjust to building the Network from a definition JSON
         */

        //Construct Hosts
        Host h1 = new Host(1);
        Host h2 = new Host(2);

        //Construct Links
        Link l1 = new Link(1, 10 * 1024 * 1024, 10, 8* 64 * 1024, h1, h2);

        //Add links to Host definitions
        h1.setLink(l1);
        h2.setLink(l1);

        //Construct Flow
        Flow f1 = new Flow(1, h1, h2, 20, 1.0);

        List<Updatable> updatableList = new LinkedList<>();

        updatableList.add(h1);
        updatableList.add(h2);
        updatableList.add(l1);

        //Testing
        h1.addFlow(f1);

        for(int i = 0; i < 4000; i+= 5)
        {
            for(Updatable updatable : updatableList)
            {
                updatable.update(5,i);
            }
        }
    }
}