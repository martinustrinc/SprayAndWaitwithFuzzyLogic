/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.*;
import java.util.*;

/**
 *
 * @author Gregorius Bima, Sanata Dharma Univeristy
 */
public class DecisionEngineRouterImproved extends ActiveRouter
{
        public static final String PUBSUB_NS = "DecisionEngineRouterImproved";
	public static final String ENGINE_SETTING = "decisionEngine";
	public static final String TOMBSTONE_SETTING = "tombstones";
	public static final String CONNECTION_STATE_SETTING = "";
        /** All router static interval */
        public static final int UPDATE_INTERVAL = 900;
	
        protected double lastUpdate = Double.MIN_VALUE;
	protected boolean tombstoning;
	protected RoutingDecisionEngineImproved improvedDecider;
	protected List<Tuple<Message, Connection>> outgoingMessages;
	
	protected Set<String> tombstones;
	
	/** 
	 * Used to save state machine when new connections are made. See comment in
	 * changedConnection() 
	 */
	protected Map<Connection, Integer> conStates;
	
	public DecisionEngineRouterImproved(Settings s)
	{
		super(s);
		
		Settings routeSettings = new Settings(PUBSUB_NS);
		
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		
		improvedDecider = (RoutingDecisionEngineImproved)routeSettings.createIntializedObject(
				"routing." + routeSettings.getSetting(ENGINE_SETTING));
		
		if(routeSettings.contains(TOMBSTONE_SETTING))
			tombstoning = routeSettings.getBoolean(TOMBSTONE_SETTING);
		else
			tombstoning = false;
		
		if(tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
	}

	public DecisionEngineRouterImproved(DecisionEngineRouterImproved r)
	{
		super(r);
		outgoingMessages = new LinkedList<Tuple<Message, Connection>>();
		improvedDecider = (RoutingDecisionEngineImproved) r.improvedDecider.replicate();
		tombstoning = r.tombstoning;
		
		if(this.tombstoning)
			tombstones = new HashSet<String>(10);
		conStates = new HashMap<Connection, Integer>(4);
	}

	@Override
	public MessageRouter replicate()
	{
		return new DecisionEngineRouterImproved(this);
	}

	@Override
	public boolean createNewMessage(Message m)
	{
		if(improvedDecider.newMessage(m))
		{
			if(m.getId().equals("M7"))
				System.out.println("Host: " + getHost() + " Creating M7");
			makeRoomForNewMessage(m.getSize());
			//revised by Matthew
			m.setTtl(this.msgTtl);
			
			addToMessages(m, true);
			
			findConnectionsForNewMessage(m, getHost());
			return true;
		}
		return false;
	}
	
	@Override
	public void changedConnection(Connection con)
	{
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		DecisionEngineRouterImproved otherRouter = (DecisionEngineRouterImproved)otherNode.getRouter();
		if(con.isUp())
		{
			improvedDecider.connectionUp(myHost, otherNode);
			
			/*
			 * This part is a little confusing because there's a problem we have to
			 * avoid. When a connection comes up, we're assuming here that the two 
			 * hosts who are now connected will exchange some routing information and
			 * update their own based on what the get from the peer. So host A updates
			 * its routing table with info from host B, and vice versa. In the real
			 * world, A would send its *old* routing information to B and compute new
			 * routing information later after receiving B's *old* routing information.
			 * In ONE, changedConnection() is called twice, once for each host A and
			 * B, in a serial fashion. If it's called for A first, A uses B's old info
			 * to compute its new info, but B later uses A's *new* info to compute its
			 * new info.... and this can lead to some nasty problems. 
			 * 
			 * To combat this, whichever host calls changedConnection() first calls
			 * doExchange() once. doExchange() interacts with the DecisionEngine to
			 * initiate the exchange of information, and it's assumed that this code
			 * will update the information on both peers simultaneously using the old
			 * information from both peers.
			 */
			if(shouldNotifyPeer(con))
			{
				this.doExchange(con, otherNode);
				otherRouter.didExchange(con);
			}
			
			/*
			 * Once we have new information computed for the peer, we figure out if
			 * there are any messages that should get sent to this peer.
			 */
			Collection<Message> msgs = getMessageCollection();
			for(Message m : msgs)
			{
				if(improvedDecider.shouldSendMessageToHost(m, otherNode))
					outgoingMessages.add(new Tuple<Message,Connection>(m, con));
			}
		}
		else
		{
			improvedDecider.connectionDown(myHost, otherNode);
			
			conStates.remove(con);
			
			/*
			 * If we  were trying to send message to this peer, we need to remove them
			 * from the outgoing List.
			 */
			for(Iterator<Tuple<Message,Connection>> i = outgoingMessages.iterator(); 
					i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getValue() == con)
					i.remove();
			}
		}
	}
	
	protected void doExchange(Connection con, DTNHost otherHost)
	{
		conStates.put(con, 1);
		improvedDecider.doExchangeForNewConnection(con, otherHost);
	}
	
	/**
	 * Called by a peer DecisionEngineRouter to indicated that it already 
	 * performed an information exchange for the given connection.
	 * 
	 * @param con Connection on which the exchange was performed
	 */
	protected void didExchange(Connection con)
	{
		conStates.put(con, 1);
	}
	
	@Override
	protected int startTransfer(Message m, Connection con)
	{
		int retVal;
		
		if (!con.isReadyForTransfer()) {
			return TRY_LATER_BUSY;
		}
		
		retVal = con.startTransfer(getHost(), m);
		if (retVal == RCV_OK) { // started transfer
			addToSendingConnections(con);
		}
		else if(tombstoning && retVal == DENIED_DELIVERED)
		{
			this.deleteMessage(m.getId(), false);
			tombstones.add(m.getId());
		}
		else if (deleteDelivered && (retVal == DENIED_OLD || retVal == DENIED_DELIVERED) && 
				improvedDecider.shouldDeleteOldMessage(m, con.getOtherNode(getHost()))) {
			/* final recipient has already received the msg -> delete it */
			if(m.getId().equals("M7"))
				System.out.println("Host: " + getHost() + " told to delete M7");
			this.deleteMessage(m.getId(), false);
		}
		
		return retVal;
	}

	@Override
	public int receiveMessage(Message m, DTNHost from)
	{
		if(isDeliveredMessage(m) || (tombstoning && tombstones.contains(m.getId())))
			return DENIED_DELIVERED;
			
		return super.receiveMessage(m, from);
	}

	@Override
	public Message messageTransferred(String id, DTNHost from)
	{
		Message incoming = removeFromIncomingBuffer(id, from);
	
		if (incoming == null) {
			throw new SimError("No message with ID " + id + " in the incoming "+
					"buffer of " + getHost());
		}
		
		incoming.setReceiveTime(SimClock.getTime());
		
		Message outgoing = incoming;
		for (Application app : getApplications(incoming.getAppID())) {
			// Note that the order of applications is significant
			// since the next one gets the output of the previous.
			outgoing = app.handle(outgoing, getHost());
			if (outgoing == null) break; // Some app wanted to drop the message
		}
		
		Message aMessage = (outgoing==null)?(incoming):(outgoing);
		
		boolean isFinalRecipient = improvedDecider.isFinalDest(aMessage, getHost());
		boolean isFirstDelivery =  isFinalRecipient && 
			!isDeliveredMessage(aMessage);
		
		if (outgoing!=null && improvedDecider.shouldSaveReceivedMessage(aMessage, getHost())) 
		{
			// not the final recipient and app doesn't want to drop the message
			// -> put to buffer
			addToMessages(aMessage, false);
			
			// Determine any other connections to which to forward a message
			findConnectionsForNewMessage(aMessage, from);
		}
		
		if (isFirstDelivery)
		{
			this.deliveredMessages.put(id, aMessage);
		}
		
		for (MessageListener ml : this.mListeners) {
			ml.messageTransferred(aMessage, from, getHost(),
					isFirstDelivery);
		}
		
		return aMessage;
	}

	@Override
	protected void transferDone(Connection con)
	{
		Message transferred = this.getMessage(con.getMessage().getId());
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
                    i.hasNext();)
                    {
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(transferred.getId()) && 
					t.getValue().equals(con))
			{
				i.remove();
				break;
			}
		}
		
                improvedDecider.transferDone(con);
                
		if(improvedDecider.shouldDeleteSentMessage(transferred, con.getOtherNode(getHost())))
		{
			if(transferred.getId().equals("M7"))
				System.out.println("Host: " + getHost() + " deleting M7 after transfer");
			this.deleteMessage(transferred.getId(), false);
			
			
		}
	}

	@Override
	public void update()
	{
		super.update();
                if (SimClock.getTime()-lastUpdate>=UPDATE_INTERVAL) {
                    improvedDecider.update(getHost());
                    this.lastUpdate = SimClock.getTime() - SimClock.getIntTime() % UPDATE_INTERVAL;
                }
		
				
		
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		tryMessagesForConnected(outgoingMessages);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
			i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(!this.hasMessage(t.getKey().getId()))
			{
				i.remove();
			}
		}
	}
	
	@Override
	public void deleteMessage(String id, boolean drop)
	{
		super.deleteMessage(id, drop);
		
		for(Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); 
		i.hasNext();)
		{
			Tuple<Message, Connection> t = i.next();
			if(t.getKey().getId().equals(id))
			{
				i.remove();
			}
		}
	}

	public RoutingDecisionEngineImproved getDecisionEngine()
	{
		return this.improvedDecider;
	}

	protected boolean shouldNotifyPeer(Connection con)
	{
		Integer i = conStates.get(con);
		return i == null || i < 1;
	}
	
	protected void findConnectionsForNewMessage(Message m, DTNHost from)
	{
//		for(Connection c : getHost()) 
		for(Connection c : getConnections())
		{
			DTNHost other = c.getOtherNode(getHost());
			if(other != from && improvedDecider.shouldSendMessageToHost(m, other))
			{
				if(m.getId().equals("M7"))
					System.out.println("Adding attempt for M7 from: " + getHost() + " to: " + other);
				outgoingMessages.add(new Tuple<Message, Connection>(m, c));
			}
		}
	}
}
