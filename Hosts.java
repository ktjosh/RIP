/**
 * filename: Hosts.java
 *
 * class to gather information about hosts
 * One of the class for the RIP implementation
 * Version $1.1
 * @author: ktjosh
 *
 */
import java.net.InetAddress;


class Hosts
{
    String name;
    InetAddress destip;// CIDR net prefix
    InetAddress destinationIP;//ip address
    InetAddress subnetmask;
    Integer portnum;
    double cost;
    double original_cost;
    boolean nbr;// true if the host is my neighbor
    InetAddress nexthop;
    long timestamp;
    boolean firstmsg;

    /**
     * custom tostring method of the class to get the host information.
     * @return
     */
    public String toString()
    {
        return " "+destip+" "+nexthop+" "+subnetmask+" "+cost;
    }

}