package rsocket.websockets.consumer;

import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.net.URI;

@Log4j2
@SpringBootApplication
public class ConsumerApplication implements ApplicationListener<ApplicationReadyEvent> {

		public static void main(String[] args) {
				new SpringApplicationBuilder()
					.profiles("ws-consumer")
					.sources(ConsumerApplication.class)
					.run(args);
		}

		private final String websocketEndpointUri;

		public ConsumerApplication(@Value("${ws-uri}") String websocketEndpointUri) {
				this.websocketEndpointUri = "ws://localhost:7000/";
				log.info("connecting websocket client to " + websocketEndpointUri + '.');
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
				RSocketFactory
					.connect()
					.keepAlive()
					.transport(WebsocketClientTransport.create(URI.create(this.websocketEndpointUri)))
					.start()
					.onTerminateDetach()
					.flatMapMany(rsocket ->
						rsocket
							.requestStream(DefaultPayload.create("Josh"))
							.take(10)
							.doOnComplete(rsocket::dispose)
					)
					.subscribe( p -> log.info(p.getDataUtf8()) );
		}
}
