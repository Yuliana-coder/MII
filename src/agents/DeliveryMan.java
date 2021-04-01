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
    List <AID> shopsDel;
    ArrayList <int> volumes;
    int maxVolume;
    int currentVolume = 0;
    public static final  String AGENT_TYPE = "DELIVERYMAN";
    AID[] shops;
    int [] shopOrderVolums;
    int step = 0;

    boolean whole_order = true;

    public void behaviour(){
        if (step == 0){
            itemsInCar = new LinkedList<>();
            shopsDel = new LinkedList<>();
            volumes = new ArrayList<int>();

            shopOrderVolums = new int[shops.length];

            //отправляем запрос на получение orderVolume у всех магазинов
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            for(int i = 0; i < shops.length; i++){
                msg.addReceiver(shops[i]);
            }
            msg.setContent("GET_ORDER_VOLUME");
            send(msg);
            //получаем ответы
            for (int i = 0; i < shops.length; i++){
                ACLMessage recvMes = this.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (recvMes!=null) {
                    System.out.println("DM " + getLocalName() + " RECEIVED INFORM FROM " +
                           recvMes.getSender().getLocalName());
                    int volume = Integer.parseInt(recvMes.getContent());
                    //int shop = recvMes.getSender();
                    shopOrderVolums[i] = volume;
                }
            }
            step +=1;
        }
        else{
            if (whole_order == true){
                whole_order = searchOrder();
            }
            else{
                //searchItems();
                step ++;
            }
            System.out.println("DM " + getLocalName() + " CURRENT VOLUME " +  String.valueOf(this.currentVolume));
        }

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

    // возвращает false, если к нему уже никакой заказ полностью не лезет
    private boolean searchOrder(){
        int maxOrderVolume = 0;
        int order = -1;
        for (int i = 0; i < shopOrderVolums.length; i++){
            if ((shopOrderVolums[i] != -1) && (this.currentVolume + shopOrderVolums[i] <= this.maxVolume)){
                if (shopOrderVolums[i] >= maxOrderVolume)
                    maxOrderVolume = shopOrderVolums[i];
                order = i;
            }
        }
        if (order != -1){
            sentPropose(order, maxOrderVolume);
            return true;
        }
        else{
            for (int i = 0; i < shopOrderVolums.length; i++){
                if ((shopOrderVolums[i] != -1) && (shopOrderVolums[i] <= this.maxVolume)){
                    if (shopOrderVolums[i] >= maxOrderVolume)
                        maxOrderVolume = shopOrderVolums[i];
                    order = i;
                }
            }
            if (order != -1){
                sentPropose(order, maxOrderVolume);
                return true;
            }
            else{
                return false;
            }
        }
    }

    private void sentPropose(int order, int maxOrderVolume){
        // отправляем запрос на доставку заказа
        ACLMessage msgPropToDel = new ACLMessage(ACLMessage.PROPOSE);
        msgPropToDel.addReceiver(shops[order]);
        msgPropToDel.setContent("ALL_ITEMS");
        send(msgPropToDel);

        // ждем ответ от магазина
        ACLMessage msgReplyPropose = this.blockingReceive();
        if (msgReplyPropose.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
            System.out.println("DM " + getLocalName() + " RECEIVED ACCEPT FROM " +
                    msgReplyPropose.getSender().getLocalName());
            String[] itemsInOrder = msgReplyPropose.getContent().split(";");
            if (currentVolume > maxOrderVolume){
                currentVolume = maxOrderVolume;
            }
            volumes.add(maxOrderVolume);
            shopsDel.add(shops[order]);
            for (int i = 0; i < itemsInOrder.length; i +=2){
                itemsInCar.add(getItemAIDByLocalName(itemsInOrder[i]));
            }
        }
        else if (msgReplyPropose.getPerformative() == ACLMessage.REJECT_PROPOSAL){
            System.out.println("DM " + getLocalName() + " RECEIVED REJECT FROM " +
                    msgReplyPropose.getSender().getLocalName());
        }
        shopOrderVolums[order] = -1;
    }

    //кортеж из названия айтема и его объема
    private class Pair{
        String a;
        int b;
    }
    private class SortPair implements Comparator<Pair>{
        public int compare(Pair x, Pair y){
            if (x.b > y.b)
                return -1;
            else if(x.b == y.b)
                return 0;
            else
                return 1;
        }
    }

    private Pair[] sortItems(String[] items){
        Pair[] its = new Pair[items.length/2];
        for (int i = 0; i < items.length; i+=2) {
            Pair item = new Pair();
            item.a = items[i];
            item.b = Integer.parseInt(items[i+1]);
            its[i/2] = item;
        }
        //Arrays.sort(its, new SortPair());

        return its;
    }

    private void searchItems(){
        for (int i = 0; i < shopOrderVolums.length; i++){
            if (shopOrderVolums[i] != -1){
                // отправляем запрос на получение списка товаров из заказа
                ACLMessage msgReq = new ACLMessage(ACLMessage.REQUEST);
                msgReq.addReceiver(shops[i]);
                msgReq.setContent("GET_ITEMS_INFO");
                send(msgReq);

                ACLMessage msgItemsInform = this.blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                Pair[] items = sortItems(msgItemsInform.getContent().split(";"));

                int maxCurrentVolume = maxVolume - currentVolume;
                int max = findMaxGettedVolume(items, maxCurrentVolume);
                ACLMessage msgPropToDel = new ACLMessage(ACLMessage.PROPOSE);
                msgPropToDel.addReceiver(shops[i]);
                msgPropToDel.setContent(String.valueOf(max));
                send(msgPropToDel);

            }
        }
    }

    private  int findMaxGettedVolume(Pair[] items, int maxVolume){
        int n = items.length;
        int dp[][] = new int [maxVolume+1][n+1];
        for (int j = 1; j < n; j++){
            for(int w=1; w <= maxVolume; w++){
                if (items[j-1].b <= w){
                    dp[w][j] = Math.max(dp[w][j-1], dp[w-items[j-1].b][j-1] + 1);
                }
                else{
                    dp[w][j] = dp[w][j-1];
                }
            }
        }
        return dp[maxVolume][n];

    }





    //поиск агентов - магазинов
    AID[] searchShops() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("SHOP");
        dfd.addServices(sd);

//        SearchConstraints ALL = new SearchConstraints();
//        ALL.setMaxResults(new Long(-1));
        try{
            DFAgentDescription[] result = DFService.search(this, dfd);
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
            dfd.addServices(sd);

            //ищем агентов магазинов
            this.shops = searchShops();
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
