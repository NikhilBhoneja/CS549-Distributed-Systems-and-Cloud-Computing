package edu.stevens.cs549.dhts.resource;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.sse.SseFeature;

import edu.stevens.cs549.dhts.activity.DHTBase.Invalid;

@Path("/dht")
public class NodeResource {

	/*
	 * Web service API.
	 * 
	 * TODO: Fill in the missing operations.
	 */

	Logger log = Logger.getLogger(NodeResource.class.getCanonicalName());

	@Context
	UriInfo uriInfo;

	@Context
	HttpHeaders headers;

	@GET
	@Path("info")
	@Produces("application/json")
	public Response getNodeInfo() {
		return new NodeService(headers, uriInfo).getNodeInfo();
	}

	@GET
	@Path("pred")
	@Produces("application/json")
	public Response getPred() {
		return new NodeService(headers, uriInfo).getPred();
	}

	@PUT
	@Path("notify")
	@Consumes("application/json")
	@Produces("application/json")
	/*
	 * Actually returns a TableRep
	 */
	public Response putNotify(TableRep predDb) {
		/*
		 * See the comment for WebClient::notify (the client side of this logic).
		 */
		return new NodeService(headers, uriInfo).notify(predDb);
	}

	@GET
	@Path("find")
	@Produces("application/json")
	public Response findSuccessor(@QueryParam("id") String index) {
		int id = Integer.parseInt(index);
		return new NodeService(headers, uriInfo).findSuccessor(id);
	}
	
	@GET
	@Path("succ")
	@Produces("application/json")
	public Response getSucc() {
		return new NodeService(headers, uriInfo).getSucc();
	}
	
	@GET
	@Path("finger")
	@Produces("application/json")
	public Response getClosestPrecedingFinger(@QueryParam("id") String index) {
		int id = Integer.parseInt(index);
		return new NodeService(headers, uriInfo).getClosestPrecedingFinger(id);
	}
	
	@GET
	@Produces("application/json")
	public Response getKeyValue(@QueryParam("key") String key) throws Invalid {
		return new NodeService(headers,uriInfo).getKeyValue(key);
	}
	
	@PUT
	/*@Consumes("application/xml")*/
	@Produces("application/json")
	public Response addKeyValue(@QueryParam("key") String key, @QueryParam("val") String value) throws Invalid {
		return new NodeService(headers,uriInfo).addKeyValue(key,value);
	}
	
	@DELETE
	public Response deleteKeyValue(@QueryParam("key") String key, @QueryParam("val") String value) throws Invalid {
		return new NodeService(headers,uriInfo).deleteKeyValue(key,value);
	}

	@DELETE
	@Path("listen")
	public Response listenOff(@QueryParam("id") int id, @QueryParam("key") String key) {
		return new NodeService(headers, uriInfo).stopListening(id, key);
	}
	
	@GET
	@Path("listen")
	@Produces(SseFeature.SERVER_SENT_EVENTS)
	public Response listenForBindings(@QueryParam("id") int id, @QueryParam("key") String key) {
		return new NodeService(headers,uriInfo).listenForBindings(id,key);
	}

}
