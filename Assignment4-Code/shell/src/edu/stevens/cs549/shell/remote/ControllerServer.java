package edu.stevens.cs549.shell.remote;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import edu.stevens.cs549.shell.main.IShell;

/*
 * TODO annotate this as a server endpoint, including callback operations and decoders.
 */

@ServerEndpoint(
		value="/control/{client}",
		encoders = {CommandLineEncoder.class}, 
		decoders = {CommandLineDecoder.class})

public class ControllerServer {
	
	private static final String TAG = ControllerServer.class.getCanonicalName();
	
	private ShellManager shellManager = ShellManager.getShellManager();

	private SessionManager sessionManager = SessionManager.getSessionManager();
	
	private Session session;
	
	private boolean initializing = true;
	
	public Session getSession() {
		return session;
	}
	
	public void endInitialization() {
		initializing = false;
	}
	
    @OnOpen
    /*
     * This is where receipt of a connection request starts.
     */
    public void onOpen(Session session, @PathParam("client") String client) throws IOException {
    	/*
    	 * Cache the session in this controller.
    	 */
    	this.session = session;
    	/*
    	 * Try to set the current (pending) session, fail if there is already a (pending?) session.
    	 */
        if (sessionManager.setCurrentSession(this)) {
        	/*
        	 * Notify the current shell that we have a remote control request.
        	 */
        	shellManager.getCurrentShell().notifyPendingSession(client);
        } else {
        	SessionManager.rejectSession(session);
        }
    }

    @OnMessage
    /*
     * This is where we receive commands from the node remotely controlling us.
     */
    public void onMessage(String[] commandLine) {
		if (initializing) {
			throw new IllegalStateException("Communication from client before ack of remote control request: " + commandLine[0]);
		} else if (commandLine.length > 0 && IShell.QUIT.equals(commandLine[0])) {
			/*
			 * TODO Stop the current toplevel (local) shell.  It is sufficient to close the session,
			 * which will trigger a callback on onClose() on both sides of the connection.
			 */
			sessionManager.closeCurrentSession();
		} else {
    		/*
    		 * TODO add the commandLine to the input of the current shell
    		 */
			shellManager.getCurrentShell().addCommandLine(commandLine);
		}
    }
    
    /**
     * Stop the current local shell and remove it from the shell stack.
     * This is only called when the shell is being remotely controlled, so the context is a proxy.
     */
    private void quitShell() {
    	shellManager.getCurrentShell().stop();
   		shellManager.removeShell();
    }

	@OnError
	public void onError(Throwable t) {
		shellManager.getCurrentShell().fatalError(TAG, "Error on connection", t);
		if (!initializing) {
			quitShell();
		}
	}

	@OnClose
	public void onClose(Session session) {
		/*
		 * A client may close the session without sending the QUIT command.
		 */
		if (!initializing) {
			quitShell();
		}
	}

}
