import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Utility class for grabbing service banners from open ports
 */
public class BannerGrabber {
    
    /**
     * Attempt to grab banner from an open port
     * @param host Target host
     * @param port Target port
     * @param timeoutMs Connection timeout in milliseconds
     * @return Banner string or empty string if unavailable
     */
    public static String grabBanner(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            
            // Try to read banner (some services send it immediately)
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            
            // Check if data is available
            if (reader.ready()) {
                StringBuilder banner = new StringBuilder();
                String line;
                int lines = 0;
                while (reader.ready() && lines < 3) { // Read max 3 lines
                    line = reader.readLine();
                    if (line != null) {
                        banner.append(line.trim()).append(" ");
                        lines++;
                    }
                }
                return banner.toString().trim();
            }
            
        } catch (IOException e) {
            // Banner not available or connection failed
        }
        return "";
    }
}