/**
 * filename: RecevingProcess.java
 *
 * to receive messages from neighbor
 *One of the class for the RIP implementation
 * Version $1.1
 * @author: ktjosh
 *
 */
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;


class ReceivingProcess extends Thread {

    private DatagramSocket mysocket;
    private ConcurrentHashMap<InetAddress, Hosts> RTable;
    private InetAddress myAddress;
    private InetAddress netprefix;
    private int nbr;
    SendingProcess s;

    /**
     * Constructor
     * @param mysocket :my socket
     * @param RTable: my routing table
     * @param myAddress : my ip address
     * @param netprefix : my network prefix
     * @param nbr : my number of neighbor
     * @param s : object of thr sending process
     */
    public ReceivingProcess(DatagramSocket mysocket, ConcurrentHashMap RTable, InetAddress myAddress,
                            InetAddress netprefix, int nbr, SendingProcess s) {
        this.mysocket = mysocket;
        this.RTable = RTable;
        this.myAddress = myAddress;
        this.nbr = nbr;
        this.s = s;
        this.netprefix = netprefix;
    }

    /**
     * Method invoked when the thread of this class runs
     */
    @Override
    public void run() {
        //buffer for the packet
        byte[] mypacketbuffer = new byte[2048];
        boolean forthefirstpacket = true;
        DatagramPacket receivepackate = new DatagramPacket(mypacketbuffer, mypacketbuffer.length);

        while (true) {

            if (forthefirstpacket) {
                try {
                    mysocket.receive(receivepackate);
                    mypacketbuffer = receivepackate.getData();
                    updateTable(mypacketbuffer, receivepackate);
                } catch (Exception e) {
                    System.out.print("timeout reached");//e.printStackTrace();
                }
            }
            // synchronized (RTable)
            {
                try {

                    //** RTable.notify();
                    if (!forthefirstpacket) {
                // if you have only neighbor and it stops sending rhe process will
                 //blocked here forever hence need a timeout
                        mysocket.setSoTimeout(12000);

                        mysocket.receive(receivepackate);
                        mypacketbuffer = receivepackate.getData();
                        updateTable(mypacketbuffer, receivepackate);

                    }
                    //**  RTable.wait();


                    // RTable.notify();
                } catch (SocketTimeoutException e) {
                    checktime();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                forthefirstpacket = false;
            }


        }

    }

    /**
     * upon reeiving the packet this method updates therouting table
     * @param mypacketbuffer : the buffer of the packet
     * @param receivepackate : the datagrm packet
     */
    public void updateTable(byte[] mypacketbuffer, DatagramPacket receivepackate) {
        // extracting the packet
        String receive = new String(mypacketbuffer, 0, receivepackate.getLength());

        String[] data = receive.split(" ");
        try {
            // extracting the ip address and its network prefix
            InetAddress sourcehostaddress = InetAddress.getByName(data[0].substring(1));
            InetAddress sourcehost = InetAddress.getByName(data[1].substring(1));

            System.out.println();
            System.out.println("ReceivingProcess: Received a packet from the address: " + sourcehostaddress);
            System.out.println();

            //reording the time stamp
            RTable.get(sourcehost).timestamp = System.currentTimeMillis();

            if (RTable.get(sourcehost).firstmsg) {
                RTable.get(sourcehost).firstmsg = false;
            }

            checktime();

            //information sent by each router is analysed to see the updates.
            for (int i = 2; i < data.length; i += 4) {

                InetAddress destip = InetAddress.getByName(data[i].substring(1));
                InetAddress nexthop = InetAddress.getByName(data[i + 1].substring(1));
                InetAddress subnet = InetAddress.getByName(data[i + 2].substring(1));
                //implementing posoned reverse, if the next hop is myself the cost to me is considered infinity.
                double cost = nexthop.equals(netprefix) ? Double.POSITIVE_INFINITY : Double.parseDouble(data[i + 3]);

                //if its yor own ip ignore
                if (!destip.equals(netprefix)) {

                  //if the dest ip is not in the routing table
                    if (!RTable.containsKey(destip)) {
                        Hosts h = new Hosts();
                        h.destip = destip;
                        h.nexthop = sourcehost;
                        h.subnetmask = subnet;
                        h.cost = cost + RTable.get(sourcehost).cost + cost;
                        ;
                        h.nbr = false;
                        RTable.put(destip, h);
                    }
                    //else check for route updates
                    else {

                        double newcostz = RTable.get(sourcehost).cost + cost; // new cost;

                        //some other node
                        if (RTable.get(destip).nbr) {
                            if (RTable.get(destip).original_cost < RTable.get(destip).cost &&
                                    RTable.get(destip).original_cost < newcostz) {
                                System.out.println("----------**::**---------------");
                                RTable.get(destip).cost = RTable.get(destip).original_cost;
                                RTable.get(destip).nexthop = destip;
                            }
                        }
                        // if the nexthop to the dest ip is sending me the packet
                        // the entry which i have, des it have the same value still.
                        if (RTable.get(destip).nexthop.equals(sourcehost)) {
                            RTable.get(destip).cost = newcostz;
                        } else if (newcostz < RTable.get(destip).cost) {
                            //if there is a better path  than the neighbpt
                            RTable.get(destip).cost = newcostz;
                            RTable.get(destip).nexthop = sourcehost;

                        }

                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This methid copares the timestamp value of each router to check if the router is dead or not
     */
    private void checktime() {
        boolean routerdead = false;

        for (Hosts h : RTable.values()) {
            //only check if its nbr and this is nt first msg
            if (!h.firstmsg && h.nbr && System.currentTimeMillis() - h.timestamp > 6000) {

                System.out.println();
                System.out.println("*DEAD*:: Link to " + h.destip + " is Dead due to timeout");
                System.out.println("Sending Triggered Updates");

                h.cost = Double.POSITIVE_INFINITY;
                h.original_cost = h.cost;
                routerdead = true;
                h.nbr = false;
                // for all nodes whos next hop is ead router make them infinity
                for (Hosts h2 : RTable.values()) {
                    if (h2.nexthop.equals(h.destip)) {
                        //my neighbor wont send me the distance to itself, it will send its outing table
                        //if router dead is next hop for that neighbor then the new distabce will be the
                        //original distance
                        if (h2.nbr) {
                            h2.cost = h2.original_cost;
                            h2.nexthop = h2.destip;
                        } else
                            h2.cost = Double.POSITIVE_INFINITY;
                    }
                }
            }
        }

        //if the router is dead it sends triggered update
        if (routerdead) {
            TriggeredUpdate();
        }
    }

    /**
     * this method sends triggered updates to all its neighbors
     */
    private void TriggeredUpdate() {
        try {
            this.s.broadcast();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
