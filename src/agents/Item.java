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
    AID goal;

    public static final  String AGENT_TYPE = "ITEM";



    private void giveInfo(ACLMessage msgGetInfo) {
        ACLMessage reply = msgGetInfo.createReply();
        reply.setContent(volume+"-"+goal.toString());
        reply.setPerformative(ACLMessage.PROXY);
        send(reply);
    }

    private void behaviour() {
    }

    protected void setup() {
        System.out.println("ITEM " +  getLocalName()+" STARTED");
        Object[] args = getArguments();
        try {
            this.volume = (int)args[0];

            this.goal = (AID)args[1];
            String localName = this.getLocalName();
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            dfd.setName(getAID());
            sd.setName(getLocalName());
            sd.setType(AGENT_TYPE);
            dfd.addServices(sd);
            //getIncompatibleTypesFromManager();
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
