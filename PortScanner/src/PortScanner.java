import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-threaded TCP Port Scanner
 * Identifies open ports on target hosts using concurrent socket connections
 * 
 * @author Harish Varutharasu
 * @version 1.0
 */
public class PortScanner {
    
    // Default configuration
    private static final int DEFAULT_TIMEOUT_MS = 200;
    private static final int DEFAULT_THREADS = 100;
    private static final int MAX_THREADS = 500;
    private static final int MIN_TIMEOUT = 50;
    private static final int MAX_TIMEOUT = 5000;
    
    // Feature flags
    private static boolean enableBannerGrab = false;
    private static boolean showProgress = true;
    private static boolean verbose = false;
    private static String exportFormat = null; // txt, csv, json
    
    /**
     * Main entry point for the port scanner
     */
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            ScanConfig config = parseArguments(args);
            
            if (config == null) {
                showUsage();
                return;
            }
            
            // Display scan configuration
            displayBanner(config);
            
            // Validate target host
            validateHost(config.host);
            
            // Execute port scan
            List<PortResult> results = executeScan(config);
            
            // Display results
            displayResults(results, config);
            
            // Export if requested
            if (exportFormat != null) {
                exportResults(results, config);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parse command line arguments into configuration
     */
    private static ScanConfig parseArguments(String[] args) {
        if (args.length < 1) {
            return null;
        }
        
        // Handle special commands
        if (args[0].equals("--help") || args[0].equals("-h")) {
            return null;
        }
        
        if (args[0].equals("--top-ports")) {
            if (args.length < 2) {
                System.err.println("Error: --top-ports requires a host");
                return null;
            }
            return new ScanConfig(args[1], ServiceMapper.getTopPorts(), 
                                DEFAULT_TIMEOUT_MS, DEFAULT_THREADS);
        }
        
        // Standard scan: host startPort endPort [options]
        if (args.length < 3) {
            return null;
        }
        
        String host = args[0];
        int startPort = parsePort(args[1], 1);
        int endPort = parsePort(args[2], 65535);
        int timeout = DEFAULT_TIMEOUT_MS;
        int threads = DEFAULT_THREADS;
        
        // Parse optional arguments
        for (int i = 3; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "-t":
                case "--timeout":
                    if (i + 1 < args.length) {
                        timeout = Math.max(MIN_TIMEOUT, 
                                  Math.min(MAX_TIMEOUT, Integer.parseInt(args[++i])));
                    }
                    break;
                    
                case "-c":
                case "--threads":
                    if (i + 1 < args.length) {
                        threads = Math.max(1, 
                                 Math.min(MAX_THREADS, Integer.parseInt(args[++i])));
                    }
                    break;
                    
                case "-b":
                case "--banner":
                    enableBannerGrab = true;
                    break;
                    
                case "-v":
                case "--verbose":
                    verbose = true;
                    break;
                    
                case "-q":
                case "--quiet":
                    showProgress = false;
                    break;
                    
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        String format = args[++i].toLowerCase();
                        if (format.equals("txt") || format.equals("csv") || format.equals("json")) {
                            exportFormat = format;
                        }
                    }
                    break;
            }
        }
        
        // Validate port range
        if (startPort < 1) startPort = 1;
        if (endPort > 65535) endPort = 65535;
        if (startPort > endPort) {
            System.err.println("Error: startPort must be <= endPort");
            return null;
        }
        
        return new ScanConfig(host, startPort, endPort, timeout, threads);
    }
    
    /**
     * Execute the port scan with multi-threading
     */
    private static List<PortResult> executeScan(ScanConfig config) 
            throws InterruptedException {
        
        Instant startTime = Instant.now();
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(config.threads);
        List<Future<PortResult>> futures = new ArrayList<>();
        
        // Progress tracking
        AtomicInteger scanned = new AtomicInteger(0);
        int totalPorts = config.getPortCount();
        
        // Start progress monitor if enabled
        ScheduledExecutorService progressMonitor = null;
        if (showProgress) {
            progressMonitor = Executors.newScheduledThreadPool(1);
            progressMonitor.scheduleAtFixedRate(() -> {
                int done = scanned.get();
                double percentage = (done * 100.0) / totalPorts;
                System.out.printf("\r[Progress] %d/%d ports (%.1f%%)  ", 
                                done, totalPorts, percentage);
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
        
        // Submit scanning tasks
        if (config.isRangeScan()) {
            for (int port = config.startPort; port <= config.endPort; port++) {
                final int p = port;
                futures.add(executor.submit(() -> {
                    PortResult result = scanPort(config.host, p, config.timeout);
                    scanned.incrementAndGet();
                    return result;
                }));
            }
        } else {
            for (int port : config.specificPorts) {
                final int p = port;
                futures.add(executor.submit(() -> {
                    PortResult result = scanPort(config.host, p, config.timeout);
                    scanned.incrementAndGet();
                    return result;
                }));
            }
        }
        
        // Shutdown executor and wait
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);
        
        // Stop progress monitor
        if (progressMonitor != null) {
            progressMonitor.shutdown();
            System.out.println(); // New line after progress
        }
        
        // Collect results
        List<PortResult> openPorts = new ArrayList<>();
        for (Future<PortResult> future : futures) {
            try {
                PortResult result = future.get();
                if (result != null) {
                    openPorts.add(result);
                }
            } catch (ExecutionException e) {
                if (verbose) {
                    System.err.println("Scan error: " + e.getMessage());
                }
            }
        }
        
        // Sort results by port number
        Collections.sort(openPorts);
        
        // Store duration
        Instant endTime = Instant.now();
        config.duration = Duration.between(startTime, endTime).toMillis();
        
        return openPorts;
    }
    
    /**
     * Scan a single port
     * @return PortResult if open, null if closed/filtered
     */
    private static PortResult scanPort(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            // Attempt TCP connection
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            
            if (verbose) {
                System.out.println("[OPEN] Port " + port);
            }
            
            // Get service name
            String serviceName = ServiceMapper.getServiceName(port);
            
            // Grab banner if enabled
            String banner = "";
            if (enableBannerGrab) {
                banner = BannerGrabber.grabBanner(host, port, timeoutMs);
            }
            
            return new PortResult(port, banner, serviceName);
            
        } catch (IOException e) {
            // Port is closed or filtered
            if (verbose) {
                System.out.println("[CLOSED] Port " + port);
            }
            return null;
        }
    }
    
    /**
     * Validate that the target host is reachable
     */
    private static void validateHost(String host) throws UnknownHostException {
        InetAddress.getByName(host);
    }
    
    /**
     * Display application banner and scan configuration
     */
    private static void displayBanner(ScanConfig config) {
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("        MULTI-THREADED PORT SCANNER");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println();
        System.out.println("Target: " + config.host);
        
        if (config.isRangeScan()) {
            System.out.println("Ports: " + config.startPort + " - " + config.endPort);
        } else {
            System.out.println("Scanning top " + config.specificPorts.length + " common ports");
        }
        
        System.out.println("Timeout: " + config.timeout + " ms");
        System.out.println("Threads: " + config.threads);
        System.out.println("Banner Grabbing: " + (enableBannerGrab ? "Enabled" : "Disabled"));
        System.out.println();
        System.out.println("Starting scan...");
        System.out.println("───────────────────────────────────────────────");
    }
    
    /**
     * Display scan results
     */
    private static void displayResults(List<PortResult> results, ScanConfig config) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("           SCAN RESULTS");
        System.out.println("═══════════════════════════════════════════════");
        System.out.println();
        
        if (results.isEmpty()) {
            System.out.println("No open TCP ports found in the specified range.");
        } else {
            System.out.println("Open Ports: " + results.size());
            System.out.println();
            
            for (PortResult result : results) {
                System.out.println("  " + result.toString());
            }
        }
        
        System.out.println();
        System.out.println("───────────────────────────────────────────────");
        System.out.println("Scan completed in " + config.duration + " ms");
        System.out.println("═══════════════════════════════════════════════");
    }
    
    /**
     * Export results to file
     */
    private static void exportResults(List<PortResult> results, ScanConfig config) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "scan_" + config.host.replace(".", "_") + "_" + timestamp;
        
        switch (exportFormat) {
            case "txt":
                ResultExporter.exportToText(results, config.host, 
                                          config.duration, filename + ".txt");
                break;
            case "csv":
                ResultExporter.exportToCSV(results, config.host, 
                                         config.duration, filename + ".csv");
                break;
            case "json":
                ResultExporter.exportToJSON(results, config.host, 
                                          config.duration, filename + ".json");
                break;
        }
    }
    
    /**
     * Display usage information
     */
    private static void showUsage() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         MULTI-THREADED TCP PORT SCANNER                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java PortScanner <host> <startPort> <endPort> [options]");
        System.out.println("  java PortScanner --top-ports <host> [options]");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  java PortScanner 192.168.1.1 1 1024");
        System.out.println("  java PortScanner scanme.nmap.org 80 443 -b -o json");
        System.out.println("  java PortScanner --top-ports localhost -b");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  -t, --timeout <ms>    Connection timeout (default: 200ms)");
        System.out.println("  -c, --threads <num>   Number of threads (default: 100)");
        System.out.println("  -b, --banner          Enable banner grabbing");
        System.out.println("  -v, --verbose         Verbose output");
        System.out.println("  -q, --quiet           Disable progress display");
        System.out.println("  -o, --output <format> Export results (txt|csv|json)");
        System.out.println("  -h, --help            Show this help message");
        System.out.println();
        System.out.println("NOTE: Only scan systems you own or have permission to test!");
        System.out.println();
    }
    
    /**
     * Parse port number with bounds checking
     */
    private static int parsePort(String portStr, int defaultValue) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Configuration class for scan parameters
     */
    private static class ScanConfig {
        String host;
        int startPort;
        int endPort;
        int[] specificPorts;
        int timeout;
        int threads;
        long duration;
        
        // Range scan constructor
        ScanConfig(String host, int startPort, int endPort, int timeout, int threads) {
            this.host = host;
            this.startPort = startPort;
            this.endPort = endPort;
            this.timeout = timeout;
            this.threads = threads;
        }
        
        // Specific ports scan constructor
        ScanConfig(String host, int[] ports, int timeout, int threads) {
            this.host = host;
            this.specificPorts = ports;
            this.timeout = timeout;
            this.threads = threads;
        }
        
        boolean isRangeScan() {
            return specificPorts == null;
        }
        
        int getPortCount() {
            return isRangeScan() ? (endPort - startPort + 1) : specificPorts.length;
        }
    }
}
