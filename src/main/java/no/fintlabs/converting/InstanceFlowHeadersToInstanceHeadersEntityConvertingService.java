package no.fintlabs.converting;

import no.fintlabs.flyt.kafka.headers.InstanceFlowHeaders;
import no.fintlabs.model.InstanceHeadersEntity;
import org.springframework.stereotype.Service;

@Service
public class InstanceFlowHeadersToInstanceHeadersEntityConvertingService {
    public InstanceHeadersEntity convert(InstanceFlowHeaders instanceFlowHeaders) {
        return InstanceHeadersEntity
                .builder()
                .sourceApplicationIntegrationId(instanceFlowHeaders.getSourceApplicationIntegrationId())
                .sourceApplicationInstanceId(instanceFlowHeaders.getSourceApplicationInstanceId())
                .archiveInstanceId(instanceFlowHeaders.getArchiveInstanceId())
                .build();
    }
}
