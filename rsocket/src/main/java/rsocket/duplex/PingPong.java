package rsocket.duplex;

import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
public class PingPong {
	public static String reply(String incoming) {
		if (incoming.equalsIgnoreCase("ping")) return "pong";
		if (incoming.equalsIgnoreCase("pong")) return "ping";
		throw new IllegalArgumentException("incoming must be either 'ping' or 'pong'");
	}

	public static Payload replyPayload(Payload p) {
		return DefaultPayload.create(reply(p.getDataUtf8()));
	}
}
