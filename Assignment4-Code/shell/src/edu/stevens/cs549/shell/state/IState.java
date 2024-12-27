package edu.stevens.cs549.shell.state;

/*
 * Interface for local store
 */
public interface IState {

	/*
	 * The key for a node.
	 */
	public String getName();

	/*
	 * Get all values stored under a key.
	 */
	public String[] get(String k);

	/*
	 * Add a binding under a key (always cumulative).
	 */
	public void add(String k, String v);

	/*
	 * Delete a binding under a key.
	 */
	public void delete(String k, String v);

	/*
	 * Clear all bindings (necessary if a node joins a network with pre-existing
	 * bindings).
	 */
	public void clear();
	
}
