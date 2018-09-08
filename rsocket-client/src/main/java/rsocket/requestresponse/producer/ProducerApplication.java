package rsocket.requestresponse.producer;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

import static rsocket.requestresponse.Protocol.PING;
import static rsocket.requestresponse.Protocol.PONG;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@SpringBootApplication
public class ProducerApplication implements ApplicationListener<ApplicationReadyEvent> {

		public static void main(String args[]) {
				SpringApplication.run(ProducerApplication.class, args);
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {

				AbstractRSocket myRSocket = new AbstractRSocket() {

						private final AtomicLong count = new AtomicLong();

						@Override
						public Mono<Payload> requestResponse(Payload requestPayload) {
								String payload = requestPayload.getDataUtf8();
								log.info("server: " + payload);
								if (this.count.incrementAndGet() > 5) {
										return Mono.error(new RuntimeException("we're failing!"));
								}
								else {
										return Mono.just(DefaultPayload.create(payload.equalsIgnoreCase(PONG) ? PING : PONG));
								}
						}
				};

				SocketAcceptor socketAcceptor = (setupPayload, reactiveSocket) -> Mono.just(myRSocket);

				RSocketFactory
					.receive()
					.acceptor(socketAcceptor)
					.transport(TcpServerTransport.create("localhost", 7000))
					.start()
					.onTerminateDetach()
					.subscribe();

		}
}
