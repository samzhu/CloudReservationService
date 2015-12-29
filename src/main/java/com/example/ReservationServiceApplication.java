package com.example;

import java.util.Arrays;
import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableBinding(Sink.class)
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationServiceApplication {
	
	@Bean
	AlwaysSampler alwaysSampler(){
		return new AlwaysSampler();
	}
	
	//起動的時候預先塞測試資料
	@Bean
	CommandLineRunner runner(ReservationRepository rr){
		return args -> {
			Arrays.asList("Dr. rod,Dr. Syer,Juergen,ALL THE COMMUNITY,Josh".split(","))
			.forEach( x -> rr.save(new Reservation(x)));;
			rr.findAll().forEach( System.out::println);
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

@MessageEndpoint
class MessageReservationReceiver{
	@Autowired
	private ReservationRepository reservationRepository;
	
	@ServiceActivator(inputChannel = Sink.INPUT)
	public void acceptReservation(String rn){
		this.reservationRepository.save(new Reservation(rn));
	}
}

@RefreshScope
@RestController
class MessageRestControler{	
	@Value("${message}")
	private String message;
	
	@RequestMapping("/message")
	String message(){
		return this.message;
	}	
}

//這個註解是把你的Repository直接變成Rest API
@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long>{

	@RestResource(path = "by-name")
	Collection<Reservation> findByReservationName( @Param("rn") String rn);
}

@Entity
class Reservation{
	@Id
	@GeneratedValue
	private Long id;

	private String reservationName;
	public Reservation(){}

	public Reservation(String reservationName) {
		this.reservationName = reservationName;
	}

	public Long getId() {
		return id;
	}

	public String getReservationName() {
		return reservationName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Reservation{");
		sb.append("id=").append(id);
		sb.append(", reservationName='").append(reservationName).append("'}");
		return sb.toString();
	}
}