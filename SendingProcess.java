/**
 * filename: SendingProcess.java
 *
 * to broadcast messages to neighbors
 * One of the class for the RIP implementation
 * Version $1.1
 * @author: ktjosh
 *
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;



class SendingProcess extends Thread {

    private DatagramSocket mysocket;
    private ConcurrentHashMap<InetAddress, Hosts> RTable;
    private InetAddress myAddress;
    private InetAddress netprefix;
    private int nbr;



    /**
     * Constructor
     * @param mysocket my socket object
     * @param RTable : routing table
     * @param myAddress : my ip address
     * @param netprefix : my network prefix
     * @param nbr : sends number of neighbors
     */
    public SendingProcess(DatagramSocket mysocket, ConcurrentHashMap RTable, InetAddress myAddress, InetAddress netprefix, int nbr) {
        this.mysocket = mysocket;
        this.RTable = RTable;
        this.myAddress = myAddress;
        this.nbr = nbr;
        this.netprefix = netprefix;


    }

    /**
     * Run method invoked when a thread of the class starts
     */
    @Override
    public void run() {

        boolean forthefirstpacket = true;


        while (true) {
            //synchronized (RTable)
              try {
                    //** RTable.notify();
                    //it waits for the first packet.
                    if (forthefirstpacket) {

                        sleep(3000);

                    }

                    sleep(1000);

                    broadcast();
                    forthefirstpacket = false;


                    //** RTable.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }

        }

    }

    /**
     * Method broadcast the routing table to all its neighbors
     * @throws IOException
     */
    public void broadcast() throws IOException {
        //created a buffer for the packet
        byte[] mypacketbuffer = new byte[2048];
        //created a packet to be sent
        DatagramPacket sendpacket = new DatagramPacket(mypacketbuffer, mypacketbuffer.length);

        String send = "" + myAddress + " " + netprefix;
        //creating the packet
        for (Hosts h : RTable.values()) {
        // appending all the host information in the packet
            send += h;
        }

        mypacketbuffer = send.getBytes();
        sendpacket.setData(mypacketbuffer);

        // sending it to neighbors
        for (Hosts h : RTable.values()) {
            if (h.nbr) {
                sendpacket.setAddress(h.destinationIP);
                sendpacket.setPort(h.portnum);
                mysocket.send(sendpacket);
            }
        }
        DisplayTable();


    }

    /**
     * Method displays the routing table of the router.
     */
    public void DisplayTable() {
        System.out.println("Routing Table:");
        System.out.println("==========================================================");
        System.out.println("Destination     Next-Hop       Subnet Mask        Cost");
        System.out.println("----------------------------------------------------------");
        for (Hosts h : RTable.values()) {

            System.out.println(h.destip + "      " + h.nexthop + "     " + h.subnetmask + "   " + h.cost);
        }

        System.out.println("==========================================================");
    }
}