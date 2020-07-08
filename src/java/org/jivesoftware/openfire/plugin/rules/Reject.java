package org.jivesoftware.openfire.plugin.rules;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.plugin.pf.PacketFilterConstants;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

public class Reject extends AbstractRule implements Rule {

    private static final Logger Log = LoggerFactory.getLogger(Reject.class);
    
    @Override
    public String getDisplayName() {
        return "Reject";
    }

    @Override
    public Packet doAction(Packet packet) throws PacketRejectedException {
        SessionManager sessionManager = SessionManager.getInstance();
        ClientSession clientSession = sessionManager.getSession(packet.getFrom());
        Packet rejectPacket;
        String pfFrom = JiveGlobals.getProperty("pf.From", "packetfilter");


        if (packet instanceof Message) {
            Message in = (Message) packet.createCopy();
            String rejectSubject = JiveGlobals.getProperty("pf.rejectSubject", "[[Rejected]]");
            if (rejectSubject.equals(in.getSubject())) {
                Log.info("Allowing rejection message from " + in.getFrom() + " to " + in.getTo());
                return null;
            } else {
                Message out = new Message();
                if (clientSession != null && in.getBody() != null) {
                    Log.info("Inbound message will be rejected " + in.getFrom() + " to " + in.getTo());
                    Log.debug(in.toString());

                    out.setFrom(new JID(pfFrom));
                    String rejectMessage = JiveGlobals.getProperty("pf.rejectMessage", "Your message was rejected by the packet filter");
                    out.setBody(rejectMessage);
                    //in.setType(Message.Type.error);
                    out.setType(Message.Type.chat);
                    out.setTo(packet.getFrom().toBareJID());
                    out.setSubject(rejectSubject);

                    if (JiveGlobals.getBooleanProperty("pf.rejectAsUser", false)) {
                        out.setThread(in.getThread());
                        out.setFrom(packet.getTo().asBareJID());
                        out.setType(Message.Type.chat);
                        Log.info("Sending rejection message as " + out.getFrom() + " to " + out.getTo());
                        Log.debug(out.toString());
                    }
                    clientSession.process(out);
                }
            }

        } else if (packet instanceof Presence) {
            rejectPacket = new Presence();
            rejectPacket.setTo(packet.getFrom());
            rejectPacket.setError(PacketError.Condition.forbidden);

        } else if (packet instanceof IQ) {
            rejectPacket = new IQ();
            rejectPacket.setTo(packet.getFrom());
            rejectPacket.setError(PacketError.Condition.forbidden);

        }
        if (doLog()) {
            Log.info("Rejecting packet [" + packet.getClass().getSimpleName() + "] from " + packet.getFrom() + " to " + packet.getTo());
        }
        throw new PacketRejectedException();
    }

    
}
