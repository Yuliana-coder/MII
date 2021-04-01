package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.sniffer.Message;

import java.util.*;

public class DeliveryMan extends Agent{
    List <AID> itemsInCar;
    int maxVolume;
    String items;
    public static final  String AGENT_TYPE = "DELIVERYMAN";
    AID[] shops;
    int[] shopOrderVolums;
    int step = 0;

    public void behaviour(){
        if (step == 0){
            //отправляем запрос на получение orderVolume у всех магазинов
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            for(int i = 0; i < shops.length; i++){
                msg.addReceiver(shops[i]);
            }
            msg.setContent("GET_ORDER_VOLUME");
            send(msg);
            //получаем ответы
            for (int i = 0; i < shops.length; i++){
                ACLMessage recvMes = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (recvMes!=null) {
                    System.out.println("DM " + getLocalName() + " RECEIVED INFORM FROM " +
                           recvMes.getSender().getLocalName());
                    int volume = Integer.valueOf(recvMes.getContent());
                    //int shop = recvMes.getSender();
                    shopOrderVolums[i] = volume;
                }

            }

        }


    }

    //поиск агентов - магазинов
    AID[] searchShops() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("SHOP");
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));
        try{
            DFAgentDescription[] result = DFService.search(this, dfd, ALL);
            AID[] agents = new AID[result.length];
            for (int i=0; i<result.length; i++)
                agents[i] = result[i].getName();
            return agents;

        }
        catch (FIPAException fe) { }
        return null;
    }

    protected void setup() {
        System.out.println("DELIVERYMAN " + getLocalName() + " STARTED");
        Object[] args = getArguments();
        try {
            this.maxVolume = (int)args[0];
            String localName = this.getLocalName();
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            dfd.setName(getAID());
            sd.setName(getLocalName());
            sd.setType(AGENT_TYPE);

            //ищем агентов магазинов
            this.shops = searchShops();

            dfd.addServices(sd);
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    behaviour();
                }
            });
            DFService.register(this, dfd);
            System.out.println(getLocalName()+" REGISTERED WITH THE DF");
        }
        catch (FIPAException e) {
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
