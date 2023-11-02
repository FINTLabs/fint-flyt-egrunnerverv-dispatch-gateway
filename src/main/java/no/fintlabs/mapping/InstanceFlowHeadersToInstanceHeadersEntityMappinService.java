package no.fintlabs.mapping;

import no.fintlabs.flyt.kafka.headers.InstanceFlowHeaders;
import no.fintlabs.model.InstanceHeadersEntity;
import org.springframework.stereotype.Service;

@Service
public class InstanceFlowHeadersToInstanceHeadersEntityMappinService {
    public InstanceHeadersEntity map(InstanceFlowHeaders instanceFlowHeaders) {
        return InstanceHeadersEntity
                .builder()
                .sourceApplicationIntegrationId(instanceFlowHeaders.getSourceApplicationIntegrationId())
                .sourceApplicationInstanceId(instanceFlowHeaders.getSourceApplicationInstanceId())
                .archiveInstanceId(instanceFlowHeaders.getArchiveInstanceId())
                .build();
    }
}
