package com.example.rsocketclient;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.URI;

@SpringBootApplication
public class RsocketClientApplication {

		private final Log log = LogFactory.getLog(RsocketClientApplication.class.getName());

		@Bean(destroyMethod = "dispose")
		RSocket client(@Value("${ws-uri}") String websocketUri) {
				WebsocketClientTransport ws = WebsocketClientTransport
					.create(URI.create(websocketUri));
				return RSocketFactory.connect().keepAlive().transport(ws).start().block();
		}

		@Bean
		ApplicationRunner runner(RSocket rSocket) {
				return args ->
					rSocket
						.requestStream(DefaultPayload.create("peace"))
						.subscribe(payload -> this.log.info(payload.getDataUtf8()));
		}

		public static void main(String[] args) {
				SpringApplication.run(RsocketClientApplication.class, args);
		}

}
