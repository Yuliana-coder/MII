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

public class Item extends Agent{
    int volume;
    String goal;

    public static final  String AGENT_TYPE = "ITEM";



    private void giveInfo(ACLMessage msgGetInfo) {
        ACLMessage reply = msgGetInfo.createReply();
        reply.setContent(String.valueOf(volume));
        reply.setPerformative(ACLMessage.INFORM);
        send(reply);
    }

    private void behaviour() {
        ACLMessage infoRequest = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        if (infoRequest != null){
            giveInfo(infoRequest);
        }
    }

    protected void setup() {
        System.out.println("ITEM " +  getLocalName()+" STARTED");
        Object[] args = getArguments();
        try {
            this.volume = Integer.valueOf(args[0].toString());
            this.goal = args[1].toString();
            String localName = this.getLocalName();
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            dfd.setName(getAID());
            sd.setName(getLocalName());
            sd.setType(AGENT_TYPE);
            dfd.addServices(sd);

            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    behaviour();
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
