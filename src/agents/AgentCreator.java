package agents;

import com.sun.jdi.IntegerValue;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAException;
import jade.wrapper.AgentController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;


import java.util.Comparator;


public class AgentCreator extends Agent {
    private String name;
    private List<AgentController> deliveryMansControllers = new LinkedList<>();
    private List<AgentController> shopsControllers = new LinkedList<>();
    private String fileName;
    private String deliveryManPATH = "agents.DeliveryMan";
    private String shopPATH = "agents.Shop";

    private class DeliveryMan{
        String name;
        int maxVolume;
        String items;
    }

    private class Shop {
        String name;
        int orderVolume;
        int timeNeedToDelivery;
        int startWork;
        int endWork;
        String items;
    }

    protected void setup() {
        System.out.println("ITEM " + getLocalName() + " STARTED");
        Object[] args = getArguments();
        try {
            System.out.println("ARGUMENTS LENGTH: " + args.length + "ARGUMENTS: " + args[0].toString() + "MY NAME: " + this.getName());
            this.fileName = args[0].toString();
            // create the agent descrption of itself
            DFAgentDescription dfd = new DFAgentDescription();
            createAgents(this.fileName);
            try {
                for (AgentController deliveryMan : deliveryMansControllers) {
                    deliveryMan.start();
                }
                Thread.sleep(1000);

                for (AgentController shop : shopsControllers) {
                    shop.start();
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // register the description with the DF
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " REGISTERED WITH THE DF");

        } catch (FIPAException e) {
            e.printStackTrace();
        }
        takeDown();
    }

    protected void createAgents(String filename) {
        File file = new File(filename);
        DeliveryMan[] myDeliveryMans = null;
        Shop[] myShops = null;
        try {
            FileReader reader = new FileReader(file);
            BufferedReader buffreader = new BufferedReader(reader);
            int count = Integer.valueOf(buffreader.readLine());
            myDeliveryMans = new DeliveryMan[count];
            for (int i = 0; i < count; i++) {
                String line = buffreader.readLine();
                String[] data = line.split(";");
                DeliveryMan dm = new DeliveryMan();
                dm.name = data[0];
                dm.maxVolume = Integer.valueOf(data[1]);
                myDeliveryMans[i] = dm;
            }
            count = Integer.valueOf(buffreader.readLine());
            myShops = new Shop[count];
            int orderVolume = 0;
            for (int i = 0; i < count; i++) {
                String line = buffreader.readLine();
                String[] data = line.split(";");
                Shop shop = new Shop();
                shop.name = data[0];
                shop.timeNeedToDelivery = Integer.valueOf(data[1]);
                shop.startWork = Integer.valueOf(data[2]);
                shop.endWork =Integer.valueOf(data[3]);
                int count1 = Integer.valueOf(data[4]);
                String items = "";
                for(int j = 0; j < count1; j++){
                    line = buffreader.readLine();
                    data = line.split(";");
                    items += (data[0] + ";" + data[1] + ";");
                    orderVolume = orderVolume + Integer.valueOf(data[1]);
                }
                shop.orderVolume = orderVolume;
                shop.items = items;
                orderVolume = 0;
                myShops[i] = shop;
            }

        } catch (Exception e) {
            System.out.println("Error CREATE");
        }
        shopsControllers= createShopsControllers(myShops);
        deliveryMansControllers = createDeliveryMansControllers(myDeliveryMans, myShops);
    }

    private  List<AgentController> createShopsControllers(Shop[] shops){
        AgentContainer container = this.getContainerController();
        List<AgentController> controllerList = new LinkedList<>();
        for (int i = 0; i < shops.length; i++){
            Object[] args = new Object[5];
            args[0] = shops[i].timeNeedToDelivery;
            args[1] = shops[i].startWork;
            args[2] = shops[i].endWork;
            args[3] = shops[i].orderVolume;
            args[4] = shops[i].items;
            try{
                controllerList.add(container.createNewAgent(shops[i].name, shopPATH,args));
            }
            catch(Exception e){
                System.out.println("Error create shops");
            }
        }
        return controllerList;
    }

    private class SortDm implements Comparator<DeliveryMan>{
        public int compare(DeliveryMan a, DeliveryMan b){
            if (a.maxVolume > b.maxVolume)
                return -1;
            else if(a.maxVolume == b.maxVolume)
                return 0;
            else
                return 1;
        }
    }

    private class SortShop implements Comparator<Shop>{
        public int compare(Shop a, Shop b){
            if (a.orderVolume > b.orderVolume)
                return -1;
            else if(a.orderVolume == b.orderVolume)
                return 0;
            else
                return 1;
        }
    }

    private List<AgentController> createDeliveryMansControllers(DeliveryMan[] dms,
                                                                Shop[] shops){
        AgentContainer container = this.getContainerController();
        List<AgentController> controllerList = new LinkedList<>();
        int k = 0;
        Arrays.sort(dms, new SortDm());

        double l = (double)dms.length;

        for (int i = 0; i < dms.length; i++){
            Object[] args = new Object[3];
            args[0] = dms[i].maxVolume;
            String items = "";
            dms[i].items = items;
            args[1] = dms[i].items;
            try{
                controllerList.add(container.createNewAgent(dms[i].name, deliveryManPATH,args));
            }
            catch(Exception e){
                System.out.println("Error create DelyveryMan");
            }
        }
        return controllerList;
    }


    protected void takeDown(){
        try{
            DFService.deregister(this);
            System.out.println(getLocalName() + "DEREGISTER WITH DF");
        }
        catch (FIPAException e){
            e.printStackTrace();
        }
    }

}
