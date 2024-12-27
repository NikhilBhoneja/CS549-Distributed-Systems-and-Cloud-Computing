package edu.stevens.cs549.shell.main;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;

import edu.stevens.cs549.shell.remote.ContextBase;
import edu.stevens.cs549.shell.remote.ControllerServer;
import edu.stevens.cs549.shell.remote.ShellManager;
import edu.stevens.cs549.shell.state.State;

/**
 * Top-level local IO. There should only be one of these per node, used by all
 * shells that are reading commands locally (even if commands are executed on a
 * remote node).
 */
public class LocalContext extends ContextBase {

	protected static final String serverPropsFile = "/server.properties";
	
	/*
	 * Use these statements during program start-up only
	 */

	private static void severe(String s) {
		System.err.println("** "+s);
	}

	private static void info(String s) {
		System.out.println(s);
	}

	/*
	 * Local context: Display responses on stdout/stderr
	 */
	
	@Override
	public void err(String msg, Throwable t) throws IOException {
		System.err.println(msg);
		err(t);
	}
	
	@Override
	public void err(Throwable t) throws IOException {
		t.printStackTrace();		
	}

	@Override
	public void debug(String tag, String msg) {
		if (debug()) {
			System.out.println(debugLine(tag,msg));
		}
	}

	private BufferedReader input;

	@Override
	public void addCommandLine(String[] inputs) {
		StringBuilder sb = new StringBuilder();
		if (inputs.length > 0) {
			for (String input : inputs) {
				sb.append(input);
				sb.append(' ');
			}
		}
		throw new IllegalStateException("Trying to send a command line to a top-level shell: " + sb.toString());
	}

	@Override
	public String[] readline() throws EOFException, IOException {
		String line = input.readLine();
		if (line == null) {
			throw new EOFException();
		}
		String[] inputs = line.split("\\s+");
		return inputs;
	}

	@Override
	public void msg(String m) {
		System.out.print(m);
	}

	@Override
	public void msgln(String m) {
		System.out.println(m);
		System.out.flush();
	}

	@Override
	public void stop() {
		System.exit(-1);
	}

	/*
	 * Hostname and port for HTTP server URL.
	 */
	private static String host;

	private static int wsPort;

	private static String name;

	private static URI CONTROL_URI;

	public static URI getControlUri() {
		try {
			return getControlServerUri(host, Integer.toString(wsPort));
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Badly formed control URI.", e);
		}
	}

	/*
	 * The key for our place in the Chord ring. We will set it to a random id,
	 * and then set if a value was specified on the command line.
	 */


	protected State state;

	private LocalContext(String[] args) {
		/*
		 * Input command stream
		 */
		this.input = new BufferedReader(new InputStreamReader(System.in));
	}

	protected static void initialize(String[] args) throws Exception {

		/*
		 * Load server properties.
		 */
		Properties props = new Properties();
		InputStream in = LocalContext.class.getResourceAsStream(serverPropsFile);
		props.load(in);
		in.close();

		host = (String) props.getProperty("server.host", "localhost");

		wsPort = Integer.parseInt((String) props.getProperty("server.port.ws", "8181"));

		name = (String) props.getProperty("server.name", "anonymous");

		/*
		 * Properties may be overridden by command line options.
		 */
		processArgs(args);

		CONTROL_URI = getControlUri();

	}

	protected static List<String> processArgs(String[] args) {
		List<String> commandLineArgs = new ArrayList<String>();
		int ix = 0;
		Hashtable<String, String> opts = new Hashtable<String, String>();

		while (ix < args.length) {
			if (args[ix].startsWith("--")) {
				String option = args[ix++].substring(2);
				if (ix == args.length || args[ix].startsWith("--"))
					severe("Missing argument for --" + option + " option.");
				else if (opts.containsKey(option))
					severe("Option \"" + option + "\" already set.");
				else
					opts.put(option, args[ix++]);
			} else {
				commandLineArgs.add(args[ix++]);
			}
		}
		/*
		 * Overrides of values from configuration file.
		 */
		Enumeration<String> keys = opts.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement();
			if ("host".equals(k))
				host = opts.get("host");
			else if ("ws".equals(k))
				wsPort = Integer.parseInt(opts.get("ws"));
			else if ("name".equals(k))
				name = opts.get("name");
			else
				severe("Unrecognized option: --" + k);
		}

		return commandLineArgs;
	}

	protected void startStateServer() throws IOException {
		info("Starting state server...");
		State.setState(name);
		info("State server bound.");
	}

	protected Server startWsServer() {
		info("Starting WS server.");
		Server server = new Server(host, wsPort, "/shell", null, ControllerServer.class);
		try {
			server.start();
		} catch (DeploymentException e) {
			throw new IllegalStateException("Failure to start WS server.", e);
		}
		return server;
	}


	public static void main(String[] args) throws Exception {

		initialize(args);

		LocalContext main = new LocalContext(args);
		
		/*
		 * Start the state server for this node.
		 */
		main.startStateServer();

		/*
		 * Push the toplevel shell (local shell, local context)
		 */
		LocalShell toplevel = LocalShell.createTopLevel(name, State.getState(), main);
		ShellManager.getShellManager().addShell(toplevel);

		/*
		 * Start the Websockets server (for remote control).
		 */
		Server wsServer = main.startWsServer();

		try {

			/*
			 * Start the command-line loop.
			 */
			info("Server started with control service at " + CONTROL_URI);
			toplevel.cli();

		} finally {

			/*
			 * Executes when the CLI terminates.
			 */
			
			info("Shutting down Web server...");
			wsServer.stop();

		}

	}

}
