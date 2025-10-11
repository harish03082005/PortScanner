import java.util.HashMap;
import java.util.Map;

/**
 * Maps port numbers to their commonly associated service names
 */
public class ServiceMapper {
    private static final Map<Integer, String> SERVICE_MAP = new HashMap<>();
    
    static {
        // Common TCP ports and their services
        SERVICE_MAP.put(20, "FTP-Data");
        SERVICE_MAP.put(21, "FTP");
        SERVICE_MAP.put(22, "SSH");
        SERVICE_MAP.put(23, "Telnet");
        SERVICE_MAP.put(25, "SMTP");
        SERVICE_MAP.put(53, "DNS");
        SERVICE_MAP.put(80, "HTTP");
        SERVICE_MAP.put(110, "POP3");
        SERVICE_MAP.put(111, "RPC");
        SERVICE_MAP.put(135, "MS-RPC");
        SERVICE_MAP.put(139, "NetBIOS");
        SERVICE_MAP.put(143, "IMAP");
        SERVICE_MAP.put(443, "HTTPS");
        SERVICE_MAP.put(445, "SMB");
        SERVICE_MAP.put(465, "SMTPS");
        SERVICE_MAP.put(587, "SMTP-Submission");
        SERVICE_MAP.put(993, "IMAPS");
        SERVICE_MAP.put(995, "POP3S");
        SERVICE_MAP.put(1433, "MS-SQL");
        SERVICE_MAP.put(1521, "Oracle-DB");
        SERVICE_MAP.put(1723, "PPTP");
        SERVICE_MAP.put(3306, "MySQL");
        SERVICE_MAP.put(3389, "RDP");
        SERVICE_MAP.put(5432, "PostgreSQL");
        SERVICE_MAP.put(5900, "VNC");
        SERVICE_MAP.put(6379, "Redis");
        SERVICE_MAP.put(8080, "HTTP-Proxy");
        SERVICE_MAP.put(8443, "HTTPS-Alt");
        SERVICE_MAP.put(9090, "WebSphere");
        SERVICE_MAP.put(27017, "MongoDB");
    }
    
    /**
     * Get service name for a given port
     * @param port Port number
     * @return Service name or "Unknown" if not mapped
     */
    public static String getServiceName(int port) {
        return SERVICE_MAP.getOrDefault(port, "Unknown");
    }
    
    /**
     * Get all top ports for quick scanning
     * @return Array of commonly scanned ports
     */
    public static int[] getTopPorts() {
        return new int[]{
            21, 22, 23, 25, 53, 80, 110, 111, 135, 139, 
            143, 443, 445, 993, 995, 1723, 3306, 3389, 
            5432, 5900, 8080, 27017
        };
    }
}