import java.util.Enumeration;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ListInterfaces 
{
    public static void main(String[] args) throws SocketException, UnknownHostException {

        Enumeration<NetworkInterface> nwInterfaces = NetworkInterface.getNetworkInterfaces();

        while (nwInterfaces.hasMoreElements()) {

            NetworkInterface nwInterface = nwInterfaces.nextElement();
            System.out.print(nwInterface.getName() + ": " +
                             nwInterface.getDisplayName());

            Enumeration<InetAddress> addresses = nwInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                System.out.print(" - " + address.getHostAddress());
            }
            System.out.println();
        }
    }
}