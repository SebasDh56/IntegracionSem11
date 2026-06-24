package ec.udla.integracion.campus;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CampusRequestRoute extends RouteBuilder {

    private static final String CAMPUS_EXCHANGE = "campus.exchange";
    private static final String INPUT_QUEUE = "campus.requests.in";
    private static final String ADMISSIONS_QUEUE = "campus.admissions.queue";
    private static final String PAYMENTS_QUEUE = "campus.payments.queue";
    private static final String SUPPORT_QUEUE = "campus.support.queue";
    private static final String ACADEMIC_QUEUE = "campus.academic.queue";
    private static final String MANUAL_REVIEW_QUEUE = "campus.manual-review.queue";

    private final CanonicalRequestTranslator canonicalRequestTranslator;

    public CampusRequestRoute(CanonicalRequestTranslator canonicalRequestTranslator) {
        this.canonicalRequestTranslator = canonicalRequestTranslator;
    }

    @Override
    public void configure() {
        from("spring-rabbitmq:" + CAMPUS_EXCHANGE
                + "?queues=" + INPUT_QUEUE
                + "&routingKey=" + INPUT_QUEUE
                + "&exchangeType=direct"
                + "&autoDeclare=false")
            .routeId("smart-campus-request-router")
            .log("Mensaje recibido desde campus.requests.in: ${body}")
            .process(canonicalRequestTranslator)
            .log("Mensaje transformado a formato canonico: ${body}")
            .log("Tipo de solicitud detectado: ${exchangeProperty.requestType}")
            .choice()
                .when(exchangeProperty("requestType").isEqualTo("ADMISSION"))
                    .log("Enrutando solicitud de admision")
                    .to(rabbitMqProducerUri(ADMISSIONS_QUEUE))
                .when(exchangeProperty("requestType").isEqualTo("PAYMENT"))
                    .log("Enrutando solicitud de pago")
                    .to(rabbitMqProducerUri(PAYMENTS_QUEUE))
                .when(exchangeProperty("requestType").isEqualTo("SUPPORT"))
                    .log("Enrutando solicitud de soporte")
                    .to(rabbitMqProducerUri(SUPPORT_QUEUE))
                .when(exchangeProperty("requestType").isEqualTo("ACADEMIC"))
                    .log("Enrutando solicitud academica")
                    .to(rabbitMqProducerUri(ACADEMIC_QUEUE))
                .otherwise()
                    .log("Solicitud no reconocida o invalida. Enviando a revision manual")
                    .to(rabbitMqProducerUri(MANUAL_REVIEW_QUEUE))
            .end();
    }

    private String rabbitMqProducerUri(String routingKey) {
        return "spring-rabbitmq:" + CAMPUS_EXCHANGE
                + "?routingKey=" + routingKey
                + "&exchangeType=direct"
                + "&autoDeclareProducer=false";
    }
}
