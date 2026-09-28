package cl.duoc.bancoxyz.core.saga.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJms
public class JmsConfig {

    public static final String COLA_TRANSFERENCIAS_SAGA = "banco.saga.transferencias";
    public static final String COLA_COMPENSACIONES_SAGA = "banco.saga.compensaciones";
    public static final String COLA_AUDITORIA_EVENTOS = "banco.eventos.auditoria";

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        converter.setObjectMapper(objectMapper);

        Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put("SagaTransferenciaEvent", cl.duoc.bancoxyz.core.saga.event.SagaTransferenciaEvent.class);
        typeIdMappings.put("SagaCompensacionEvent", cl.duoc.bancoxyz.core.saga.event.SagaCompensacionEvent.class);
        typeIdMappings.put("AuditoriaEventoDto", cl.duoc.bancoxyz.core.saga.event.AuditoriaEventoDto.class);
        converter.setTypeIdMappings(typeIdMappings);

        return converter;
    }
}
