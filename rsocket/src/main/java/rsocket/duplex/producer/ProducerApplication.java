package rsocket.duplex.producer;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/

@SpringBootApplication
public class ProducerApplication {

	public static void main(String args[]) throws Exception {
		SpringApplication.run(ProducerApplication.class, args);
		new CountDownLatch(1).await();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void produce() throws Exception {

		SocketAcceptor socketAcceptor = (setup, incoming) -> {
			incoming
				.requestStream(DefaultPayload.create("Hello-Bidi"))
				.map(Payload::getDataUtf8)
				.log()
				.subscribe();

			return Mono.just(new AbstractRSocket() {
			});
		};

		RSocketFactory
			.receive()
			.acceptor(socketAcceptor)
			.transport(TcpServerTransport.create("localhost", 7000))
			.start()
			.block();


	}
}
