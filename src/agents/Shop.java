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
    private String itemPATH = "agents.Item";
    private boolean order_getted = false;

    public static final  String AGENT_TYPE = "SHOP";
    int step = 0;
    void behaviour(){
        if (step == 0){
            AgentContainer container = this.getContainerController();
            itemsAgents = new LinkedList<>();
            itemsInOrder = new LinkedList<>();
            for (int i = 0; i < items.length; i+=2){
                Object[] args = new Object[2];
                args[0] = items[i +1]; //volume
                args[1] = this.getAID(); //
                try{
                    itemsAgents.add(container.createNewAgent(items[i], itemPATH,args));
                    //itemsInOrder.add(getItemAIDByLocalName(itemsAgents.get(i/2).getName()));
                }
                catch(Exception e) {
                    System.out.println("Error create item Agent");
                }
            }
            try{
                for (AgentController item: itemsAgents) {
                    item.start();
                }
                Thread.sleep(1000);
            }catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < itemsAgents.size(); i++){
                try{
                    itemsInOrder.add(getItemAIDByLocalName(items[i*2]));
                }
                catch(Exception e) {
                    System.out.println("Error add item AID");
                }
            }
            step +=1;
        }
        else{
            ACLMessage msgRequest = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            if (msgRequest != null){
                if (msgRequest.getContent().equals("GET_ORDER_VOLUME")){
                    System.out.println("SHOP " + getLocalName() + " RECEIVED INFORM REQUEST FROM " +
                            msgRequest.getSender().getLocalName());
                    sendDeliveryManOrderVolume(msgRequest);
                }
                else if(msgRequest.getContent().equals("GET_ITEMS_INFO")){
                    System.out.println("SHOP " + getLocalName() + " RECEIVED ITEMS INFORM REQUEST FROM " +
                            msgRequest.getSender().getLocalName());
                    sendDeliveryManItemsInform(msgRequest);
                }
            }
            //System.out.println("Go on");
            ACLMessage msgGetPropose = receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            if (msgGetPropose != null){
                System.out.println("SHOP " + getLocalName() + " RECEIVED PROPOSE FROM " +
                        msgGetPropose.getSender().getLocalName());
                if (order_getted == false){
                    if (msgGetPropose.getContent().equals("ALL_ITEMS")){
                        order_getted = true;
                        acceptPropose(msgGetPropose);
                    }
                    else{

                    }
                }
                else{
                    rejectPropose(msgGetPropose);
                }
            }
        }
    }

    private void acceptPropose(ACLMessage propose){
        if (propose.getContent().equals("ALL_ITEMS")){
            ACLMessage reply = propose.createReply();
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            String content = "";
            for (int i = 0; i < itemsInOrder.size(); i++){
                try{
                    //отправляем запрос на получение сообщения о объеме товара
                    ACLMessage getVolumeInfoMsg = new ACLMessage(ACLMessage.REQUEST);
                    getVolumeInfoMsg.addReceiver(itemsInOrder.get(i));
                    getVolumeInfoMsg.setContent("GET_VOLUME");
                    send(getVolumeInfoMsg);

                    //ждем ответ
                    ACLMessage volumeMsg = this.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    int volume = Integer.parseInt(volumeMsg.getContent());
                    String itemAgentName = itemsInOrder.get(i).getName().split("@")[0];
                    content += (itemAgentName + ";" + String.valueOf(volume) + ";");
                }
                catch (Exception e) {
                    System.out.println("Error get volume");
                    e.printStackTrace();
                }
            }
            reply.setContent(content);
            send(reply);
        }
        else{
            int volume = Integer.parseInt(propose.getContent());

        }
    }

    private void rejectPropose(ACLMessage propose){
        ACLMessage reply = propose.createReply();
        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
        reply.setContent("REJECT");
        send(reply);
    }

    private void sendDeliveryManOrderVolume(ACLMessage msg){
        ACLMessage reply = msg.createReply();
        reply.setContent(String.valueOf(orderVolume));
        reply.setPerformative(ACLMessage.INFORM);
        send(reply);
    }

    private void sendDeliveryManItemsInform(ACLMessage msg){
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        String content = "";
        for (int i = 0; i < itemsInOrder.size(); i++){
            try{
                //отправляем запрос на получение сообщения о объеме товара
                ACLMessage getVolumeInfoMsg = new ACLMessage(ACLMessage.REQUEST);
                getVolumeInfoMsg.addReceiver(itemsInOrder.get(i));
                getVolumeInfoMsg.setContent("GET_VOLUME");
                send(getVolumeInfoMsg);

                //ждем ответ
                ACLMessage volumeMsg = this.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                int volume = Integer.parseInt(volumeMsg.getContent());
                String itemAgentName = itemsInOrder.get(i).getName().split("@")[0];
                content += (itemAgentName + ";" + String.valueOf(volume) + ";");
            }
            catch (Exception e) {
                System.out.println("Error get volume");
                e.printStackTrace();
            }
        }
        reply.setContent(content);
        send(reply);
    }

    AID getItemAIDByLocalName(String name) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ITEM");
        dfd.addServices(sd);

        //SearchConstraints ALL = new SearchConstraints();
        //ALL.setMaxResults(2L);

        try{
            DFAgentDescription[] result = DFService.search(this, dfd);
            AID[] agents = new AID[result.length];
            for (int i=0; i<result.length; i++) {
                agents[i] = result[i].getName();
                String agentName = agents[i].getName().split("@")[0];
                if(agentName.equals(name))
                    return agents[i];
            }
        }
        catch (FIPAException fe) {
            System.out.println("Error");
            fe.printStackTrace();
        }
        System.out.println("null");
        return null;
    }

    protected void setup(){
        System.out.println("Shop " +  getLocalName()+" STARTED");
        Object[] args = getArguments();
        try {
            this.timeNeedToDelivery = Integer.parseInt(args[0].toString());
            this.startWork = Integer.parseInt(args[1].toString());
            this.endWork = Integer.parseInt(args[2].toString());
            this.orderVolume = Integer.parseInt(args[3].toString());
            this.items = ((String)args[4]).split(";");
            String localName = this.getLocalName();
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            dfd.setName(getAID());
            sd.setName(getLocalName());
            sd.setType(AGENT_TYPE);
            dfd.addServices(sd);
            //getIncompatibleTypesFromManager();
            addBehaviour(new Behaviour() {
                @Override
                public void action() {
                    behaviour();
                }
                @Override
                public boolean done(){
                    return false;
                }
            });
            DFService.register(this, dfd);
            System.out.println(getLocalName()+" REGISTERED WITH THE DF");
        } catch (FIPAException e) {
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