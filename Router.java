/**
 * filename: Router.java
 *
 * The main class which will be run
 * One of the class for the RIP implementation
 * Version $1.1
 * @author: ktjosh
 *
 */
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;


class Router{

DatagramSocket mysocket;
String routername;
InetAddress rourtIP;
InetAddress netprefix;
InetAddress subnetmask;
Integer Routerport;

ConcurrentHashMap<InetAddress,Hosts> RTable;
int nbr;

    /**
     * Main method
     * @param args : command line argument.
     */
    public static void main(String[] args) {

        Router itsme = new Router();
        itsme.initialize();
        itsme.startTheProcess();

    }

    /**
     * the method initializes the router its neighbor values and the routing table
     */
    public void initialize()
    {
        try {

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter my name my ipaddress , my subnet mask my portnumber with spaces in between");

            //**Input for my router
            this.routername = sc.next(); //name
            String myrouterIp = sc.next();//ip
            String mysubnetmask = sc.next(); // subnetmask
            this.rourtIP = InetAddress.getByName(myrouterIp);
            this.subnetmask = InetAddress.getByName(mysubnetmask);
            this.netprefix = InetAddress.getByName(giveCIDR(myrouterIp,mysubnetmask));// fining my network prefix
            this.Routerport =Integer.parseInt(sc.next());
            this.mysocket = new DatagramSocket(this.Routerport);
            RTable = new ConcurrentHashMap<>();


            //*****INPUT*********
            System.out.println("Enter the number of neighbors (Numeric Value)");
            this.nbr= sc.nextInt();

            for(int i =0;i<this.nbr;i++)
            {
                Hosts h = new Hosts();
                System.out.print("Enter the name,IP address ,subnet mask and portnumber of neighbor "+(i+1)+" with spaces in between");
               h.name = sc.next();
               String destinationIP = sc.next();
               String subnet = sc.next();
               String destip = giveCIDR(destinationIP,subnet);//cidr net prefix
                h.destip = InetAddress.getByName(destip); // net prefix
                h.destinationIP = InetAddress.getByName(destinationIP);// ip address of neighbor
                h.nexthop = h.destip;
                h.subnetmask = InetAddress.getByName(subnet);
                h.portnum = Integer.parseInt(sc.next());



                System.out.println("Enter the cost to reach the neighbor "+i+1);
                h.cost =Double.parseDouble(sc.next());
                h.original_cost = h.cost;
                h.nbr =true;
                RTable.put(h.destip,h);
                h.firstmsg=  true;

            }

        }

        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

    }

    /**
     * The method starts the sending and receiving process
     */
    public void startTheProcess()
    {
       SendingProcess s = new SendingProcess(this.mysocket,RTable,rourtIP,netprefix,nbr);
        ReceivingProcess r =new ReceivingProcess(this.mysocket,RTable,rourtIP,netprefix,nbr,s);
        s.start();
        r.start();
    }

    /**
     * The method returns the network prefix values
     * @param ip : ip address
     * @param subnet : subnet mask
     * @return : returns the network prefix
     */
    public  String giveCIDR(String ip , String subnet)
    {
        String[] s1= ip.trim().split("[.]");
        String[] s2= subnet.trim().split("[.]");
        String cidr="";
        for(int i=0;i<4;i++)
        {
            int a1 = Integer.parseInt(s1[i]);
            int b1 = Integer.parseInt(s2[i]);
            cidr += (a1 & b1); //bitwise and
            if(i!=3) {
                cidr +=  ".";
            }
        }

        return cidr;
    }
}


