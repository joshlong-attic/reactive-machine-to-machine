package rsocket.duplex.producer;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rsocket.duplex.PingPong;

import java.util.concurrent.CountDownLatch;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Slf4j
@SpringBootApplication
public class ProducerApplication {

	public static void main(String args[]) throws Exception {
		SpringApplication.run(ProducerApplication.class, args);
		new CountDownLatch(1).await();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void produce() {

		SocketAcceptor socketAcceptor = (setup, incoming) -> Mono.just(
			new AbstractRSocket() {

				@Override
				public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
					return Flux
						.from(payloads)
						.map(Payload::getDataUtf8)
						.map(PingPong::reply)
						.doOnNext(log::info)
						.map(DefaultPayload::create);
				}
			}
		);

		RSocketFactory
			.receive()
			.acceptor(socketAcceptor)
			.transport(TcpServerTransport.create("localhost", 7000))
			.start()
			.block();


	}
}
