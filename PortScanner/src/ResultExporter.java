import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

//exports scan results
public class ResultExporter {
    
     // Export results to plain text file
     
    public static void exportToText(List<PortResult> results, String host, 
                                    long durationMs, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("═══════════════════════════════════════════════");
            writer.println("           PORT SCAN REPORT");
            writer.println("═══════════════════════════════════════════════");
            writer.println();
            writer.println("Target Host: " + host);
            writer.println("Scan Date: " + getCurrentTimestamp());
            writer.println("Duration: " + durationMs + " ms");
            writer.println("Total Open Ports: " + results.size());
            writer.println();
            writer.println("───────────────────────────────────────────────");
            writer.println("OPEN PORTS:");
            writer.println("───────────────────────────────────────────────");
            
            for (PortResult result : results) {
                writer.println(result.toString());
            }
            
            writer.println();
            writer.println("═══════════════════════════════════════════════");
            writer.println("End of Report");
            writer.println("═══════════════════════════════════════════════");
            
            System.out.println("✓ Results exported to: " + filename);
            
        } catch (IOException e) {
            System.err.println("✗ Failed to export results: " + e.getMessage());
        }
    }
    
    /**
     * Export results to CSV format
     */
    public static void exportToCSV(List<PortResult> results, String host, 
                                   long durationMs, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // CSV Header
            writer.println("Port,Service,Banner,Status");
            
            // CSV Data
            for (PortResult result : results) {
                writer.printf("%d,%s,\"%s\",OPEN%n",
                    result.getPort(),
                    result.getServiceName(),
                    result.getBanner().replace("\"", "\"\"") // Escape quotes
                );
            }
            
            System.out.println("✓ Results exported to CSV: " + filename);
            
        } catch (IOException e) {
            System.err.println("✗ Failed to export CSV: " + e.getMessage());
        }
    }
    
    /**
     * Export results to JSON format
     */
    public static void exportToJSON(List<PortResult> results, String host, 
                                    long durationMs, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("{");
            writer.println("  \"scan_info\": {");
            writer.println("    \"target\": \"" + host + "\",");
            writer.println("    \"timestamp\": \"" + getCurrentTimestamp() + "\",");
            writer.println("    \"duration_ms\": " + durationMs + ",");
            writer.println("    \"total_open_ports\": " + results.size());
            writer.println("  },");
            writer.println("  \"open_ports\": [");
            
            for (int i = 0; i < results.size(); i++) {
                PortResult result = results.get(i);
                writer.println("    {");
                writer.println("      \"port\": " + result.getPort() + ",");
                writer.println("      \"service\": \"" + result.getServiceName() + "\",");
                writer.println("      \"banner\": \"" + escapeJson(result.getBanner()) + "\"");
                writer.print("    }");
                if (i < results.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            
            writer.println("  ]");
            writer.println("}");
            
            System.out.println("✓ Results exported to JSON: " + filename);
            
        } catch (IOException e) {
            System.err.println("✗ Failed to export JSON: " + e.getMessage());
        }
    }
    
    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }
    
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }

}
