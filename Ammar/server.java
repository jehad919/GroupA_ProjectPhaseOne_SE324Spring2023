package de.mfwk.sudokuservice.server;

import de.mfwk.sudokuservice.core.Sudoku;
import de.mfwk.sudokuservice.core.SudokuService;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.net.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 */
@WebService(endpointInterface = "de.mfwk.sudokuservice.core.SudokuService")
public class SudokuServer implements SudokuService {

    private final List<Endpoint> endpoints = new ArrayList<>();

    /**
     * Starts a server on all available network interfaces for the given port
     *
     * @param portarg The port on which to start the net server
     */
    public void start(int portarg) {
        if (portarg <= 0)
            System.out.println("No port specified, will use port 1337");
        final int port = portarg <= 0 ? 1337 : portarg;

        if (!this.endpoints.isEmpty())
            throw new IllegalStateException("Error: server already running");

        //Start the service for the public host and localhost
        Collection<URL> urls = new HashSet<>();
        try {                                         //Generate URLs for the service
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    URL url = new URL("http", address.getCanonicalHostName(), port, "/SudokuService-1.0/services/sudoku");
                    urls.add(url);
                    System.out.println();
                    System.out.println("Network interface " + ni.getName());
                    System.out.println("Host name: " + address.getCanonicalHostName());
                    System.out.println("Port: " + port);
                    System.out.println("IP Address: " + address.getHostAddress());
                    System.out.println("Complete URL: " + url);
                }
            }
        } catch (MalformedURLException e) {
            System.err.println("Error: could not create server configuration.");
            System.exit(1);
        } catch (SocketException e) {
            System.err.println("Error: could not access network interfaces.");
        }
        urls.stream()
                .map(Object::toString)
                .map(this::publishEndpoint)
                .filter(Objects::nonNull)
                .forEach(this.endpoints::add);

        if (this.endpoints.isEmpty())
            throw new IllegalStateException("Error: was unable to start the service on any network interface");

        System.out.println();
        System.out.println("Sudoku web service started.");
        System.out.println();
    }

    /**
     * Publishes a new endpoint on the given URL
     *
     * @param url The URL to publish the endpoint on
     * @return An endpoint if the publication was successful, null if the publication was unsuccessful
     */
    private Endpoint publishEndpoint(String url) {
        try {
            return Endpoint.publish(url, this);
        } catch (Exception e) {
            new Exception("Trouble with URL " + url, e).printStackTrace();
            return null;
        }
    }

    /**
     * Shuts the server down by stopping all registered endpoints
     */
    public void shutdown() {
        if (this.endpoints.isEmpty())
            throw new IllegalStateException("Error: Server is not running");

        System.out.println("Server will shut down now.");
        this.endpoints.forEach(Endpoint::stop);
        this.endpoints.clear();
    }

    /**
     * Solves a sudoku, but also uses backtracking.
     *
     * @param sudoku The sudoku to solve.
     * @return The solved sudoku.
     */
    @Override
    public Sudoku solveSudokuGuessing(Sudoku sudoku) {
        System.out.println("Sudoku solution with backtracking requested.");
        SudokuSolver solver = new SudokuSolver(sudoku);
        solver.solve();
        return solver;
    }

    /**
     * Solves a sudoku.
     *
     * @param sudoku The sudoku to solve.
     * @return The solved sudoku.
     */
    @Override
    public Sudoku solveSudoku(Sudoku sudoku) {
        System.out.println("Sudoku solution requested.");
        SudokuSolver solver = new SudokuSolver(sudoku);
        solver.solvelogically();
        return solver;
    }

    /**
     * Determines the status of the sudoku.
     *
     * @param sudoku The sudoku to check.
     * @return -1 if invalid, 0 if complete, the number of open fields if incomplete but valid.
     */
    @Override
    public int validateSudoku(Sudoku sudoku) {
        System.out.println("Sudoku validation requested.");
        return new SudokuSolver(sudoku).getStatus();
    }

    /**
     * An attempt to test whether the server is accessible.
     *
     * @return Always true if the server answers.
     */
    @Override
    public boolean ping() {
        System.out.println("Ping requested.");
        return true;
    }

    /**
     * Main method to start the server from the command line.
     *
     * @param args The first optional argument is the port number to use, all other parameters are ignored.
     */
    public static void main(String[] args) {
        int port = 1337;
        if (args.length > 0)                           //Try to get port number from program arguments
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error: first argument was no legal port number, will use standard port 1337");
            }
        else
            System.out.println("No port number specified, will use standard port 1337.");


        SudokuServer server = new SudokuServer();
        server.start(port);
        System.out.println("Use Ctrl + C to stop the server.");
        System.out.println();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    }
}
