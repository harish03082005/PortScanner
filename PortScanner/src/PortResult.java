/**
 * Data class to store port scan results with optional banner information
 */
public class PortResult implements Comparable<PortResult> {
    private final int port;
    private final String banner;
    private final String serviceName;
    
    public PortResult(int port, String banner, String serviceName) {
        this.port = port;
        this.banner = banner;
        this.serviceName = serviceName;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getBanner() {
        return banner;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public int compareTo(PortResult other) {
        return Integer.compare(this.port, other.port);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Port ").append(port);
        if (serviceName != null && !serviceName.isEmpty()) {
            sb.append(" (").append(serviceName).append(")");
        }
        if (banner != null && !banner.isEmpty()) {
            sb.append(" - Banner: ").append(banner);
        }
        return sb.toString();
    }
}