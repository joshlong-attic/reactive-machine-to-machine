package rsocket.streaming.consumer;

import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
	* @author <a href="mailto:josh@joshlong.com">Josh Long</a>
	*/
@Log4j2
@SpringBootApplication
public class ConsumerApplication implements ApplicationListener<ApplicationReadyEvent> {

		public static void main(String[] args) {
				SpringApplication.run(ConsumerApplication.class, args);
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent evt) {
				RSocketFactory
					.connect()
					.transport(TcpClientTransport.create("localhost", 7000))
					.start()
					.flatMapMany(socket ->
							socket
								.requestStream(DefaultPayload.create("Hello"))
								.map(Payload::getDataUtf8)
								.doFinally(signal -> socket.dispose())
					)
					.subscribe(name -> log.info("consuming " + name + "."));
		}
}
