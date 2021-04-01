package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.sniffer.Message;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.*;

public class Shop extends Agent{
    int orderVolume;
    int timeNeedToDelivery;
    int startWork;
    int endWork;
    String[] items;
    List <AID> itemsInOrder;
    List <AgentController> itemsAgents;
    private String itemPATH = "Agents.Items";

    public static final  String AGENT_TYPE = "SHOP";
    int step = 0;
    void behaviour(){
        if (step == 0){
            AgentContainer container = this.getContainerController();
            itemsAgents = new LinkedList<>();
            for (int i = 0; i < items.length; i+=2){
                Object[] args = new Object[2];
                args[0] = items[i +1]; //volume
                args[1] = this.getAID(); //
                try{
                    itemsAgents.add(container.createNewAgent(items[i], itemPATH,args));
                }
                catch(Exception e){
                    System.out.println("Error");
                }
            }
            for (int i = 0; i< itemsAgents.size(); i++){
                try {
                    itemsInOrder.add(getItemAIDByLocalName(itemsAgents.get(i).getName()));
                }
                catch(Exception e){
                        System.out.println("Error");
                }
            }
        }
        else{
            System.out.println("Go on");
        }
    }

    AID getItemAIDByLocalName(String name) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ITEM");
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));

        try
        {
            DFAgentDescription[] result = DFService.search(this, dfd, ALL);
            AID[] agents = new AID[result.length];
            for (int i=0; i<result.length; i++) {
                agents[i] = result[i].getName();
                if(agents[i].getName().equals(name))
                    return agents[i];
            }


        }
        catch (FIPAException fe) { fe.printStackTrace(); }

        return null;
    }


    protected void setup(){
        System.out.println("Shop " +  getLocalName()+" STARTED");
        Object[] args = getArguments();
        try {
            this.orderVolume = (int)args[0];
            this.timeNeedToDelivery = (int)args[1];
            this.startWork = (int)args[2];
            this.endWork = (int)args[3];
            this.items = ((String)args[4]).split(";");
            System.out.println("HUI");
//            String localName = this.getLocalName();
//            DFAgentDescription dfd = new DFAgentDescription();
//            ServiceDescription sd = new ServiceDescription();
//            dfd.setName(getAID());
//            sd.setName(getLocalName());
//            sd.setType(AGENT_TYPE);
//            dfd.addServices(sd);
//            //getIncompatibleTypesFromManager();
//            addBehaviour(new Behaviour() {
//                @Override
//                public void action() {
//                    behaviour();
//                }
//                @Override
//                public boolean done(){
//                    return false;
//                }
//            });
//            DFService.register(this, dfd);
//            System.out.println(getLocalName()+" REGISTERED WITH THE DF");
        }
        catch (Exception e) {
            System.out.println("ERROR HUI");
            e.printStackTrace();
        }
    }

    protected void takeDown() {
        // Deregister with the DF
        try {
            DFService.deregister(this);
            System.out.println(getLocalName()+" DEREGISTERED WITH THE DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }


}
