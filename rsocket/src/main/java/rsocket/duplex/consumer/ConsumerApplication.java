package rsocket.duplex.consumer;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootApplication
public class ConsumerApplication {

	public static void main(String args[]) throws InterruptedException {
		SpringApplication.run(ConsumerApplication.class, args);
		new CountDownLatch(1).await();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void consume() throws Exception {

		Function<RSocket, RSocket> socketFunction = rSocket -> new AbstractRSocket() {

			@Override
				public Flux<Payload> requestStream(Payload payload) {
					return Flux
						.interval(Duration.ofSeconds(1))
						.map(aLong -> DefaultPayload.create("Bi-di Response => " + aLong))
						;
				}
			};

		RSocketFactory
			.connect()
			.acceptor(socketFunction)
			.transport(TcpClientTransport.create("localhost", 7000))
			.start()
			.block()
		;
	}
}
