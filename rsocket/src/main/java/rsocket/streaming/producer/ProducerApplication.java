package rsocket.streaming.producer;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@SpringBootApplication
@Log4j2
public class ProducerApplication implements ApplicationListener<ApplicationReadyEvent> {

		public static void main(String[] args) {
				SpringApplication.run(ProducerApplication.class, args);
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
				SocketAcceptor sa = (connectionSetupPayload, rSocket) ->
					Mono.just(new AbstractRSocket() {

							@Override
							public Flux<Payload> requestStream(Payload payload) {
									return Flux
										.interval(Duration.ofMillis(1000))
										.map(aLong -> DefaultPayload.create("interval: " + aLong));
							}
					});

				RSocketFactory
					.receive()
					.acceptor(sa)
					.transport(TcpServerTransport.create("localhost", 7000))
					.start()
					.onTerminateDetach()
					.subscribe(nettyContextCloseable -> log.info("started the server @ " + Instant.now().toString()));
		}
}
