package edu.stevens.cs549.shell.remote;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import edu.stevens.cs549.shell.main.LocalShell;

/**
 * Maintain a stack of shells.
 * @author dduggan
 *
 */
public class SessionManager {
	
	private static final String TAG = SessionManager.class.getCanonicalName();
	
	public static final String ACK = "ACK";
	
	private static final SessionManager SESSION_MANAGER = new SessionManager();
	
	public static SessionManager getSessionManager() {
		return SESSION_MANAGER;
	}
	
	private static void fatalError(String mesg, Throwable t) {
		ShellManager.getShellManager().getCurrentShell().fatalError(TAG, mesg, t);
	}
	
	private static Lock lock = new ReentrantLock();
	
	/*
	 * The proxy for any node that is currently controlling this node, or requesting control.
	 * 
	 * The terminology is confusing: We refer to the controlling node that sends us commands
	 * as the "client", but we receive its commands using the "server" endpoint API for a Websocket.
	 */
	private ControllerServer currentClient;
	
	public boolean isSession() {
		return currentClient != null;
	}

	public Session getCurrentSession() {
		return currentClient != null ? currentClient.getSession() : null;
	}

	/**
	 * The start of the protocol for considering a connection request is to record the
	 * "server" that is making the connection request.
	 * @param server
	 * @return
	 */
	public boolean setCurrentSession(ControllerServer proxy) {
		lock.lock();
		try {
			if (currentClient == null) {
				currentClient = proxy;
				return true;
			} else {
				return false;
			}
		} finally {
			lock.unlock();
		}
	}
	
	public void acceptSession() throws IOException {
		lock.lock();
		try {
			/*
			 *  We are accepting a remote control request.  Push a local shell with a proxy context
			 *  on the shell stack and flag that initialization has completed.  Confirm acceptance of the 
			 *  remote control request by sending an ACK to the client.  The CLI of the newly installed shell
			 *  will be executed by the underlying CLI as part of the "accept" command.
			 */
			if (currentClient != null) {
				ShellManager shellManager = ShellManager.getShellManager();
				LocalShell shell = shellManager.getCurrentShell().getLocal();
				
				ProxyContext proxyContext = null;
				// TODO create the proxy context (need other endpoint of the WS connection)

				proxyContext = ProxyContext.createProxyContext( this.getCurrentSession().getBasicRemote());
				shellManager.addShell(LocalShell.createRemotelyControlled(shell, proxyContext));
				currentClient.endInitialization();
				
				// TODO Send ACK back to the requester of the connection
				currentClient.getSession().getBasicRemote().sendText(ACK);
			}
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Reject a connection request if we already have a connection request (pending or accepted)
	 * @param session
	 * 	The session for the connection request being rejected.
	 */
	public static void rejectSession(Session session) {
		lock.lock();
		try {
			session.close( new CloseReason(CloseCodes.CANNOT_ACCEPT , "There is already a (pending?) session.") );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Reject the current pending connection request (represented by "currentClient")
	 */
	public void rejectSession() {
		lock.lock();
		try {
			// Reject remote control request by closing the session (provide a reason!)
			if (currentClient != null) {
				currentClient.getSession().close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Rejecting request for remote control session."));
				currentClient = null;
			}
		} catch (IOException e) {
			fatalError("Error while rejecting remote control request.", e);
		} finally {
			lock.unlock();
		}
	}
	
	public void closeCurrentSession() {
		lock.lock();
		try {
			// Normal shutdown of remote control session (provide a reason!)
			if (currentClient != null) {
				currentClient.getSession().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal shutdown of remote control session."));
				currentClient = null;
			}
		} catch (IOException e) {
			fatalError("Error while closing remote session.", e);
		} finally {
			lock.unlock();
		}
	}

}
