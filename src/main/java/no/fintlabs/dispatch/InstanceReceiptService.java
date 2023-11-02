package no.fintlabs.dispatch;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.mapping.InstanceHeadersEntityToInstanceReceiptDispatchEntityMappingService;
import no.fintlabs.model.InstanceHeadersEntity;
import no.fintlabs.model.InstanceReceiptDispatchEntity;
import no.fintlabs.repositories.InstanceHeadersRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class InstanceReceiptService {
    private final InstanceHeadersRepository instanceHeadersRepository;
    private final InstanceHeadersEntityToInstanceReceiptDispatchEntityMappingService instanceHeadersEntityToInstanceReceiptDispatchEntityMappingService;

    public InstanceReceiptService(
            InstanceHeadersRepository instanceHeadersRepository, InstanceHeadersEntityToInstanceReceiptDispatchEntityMappingService instanceHeadersEntityToInstanceReceiptDispatchEntityMappingService) {
        this.instanceHeadersEntityToInstanceReceiptDispatchEntityMappingService = instanceHeadersEntityToInstanceReceiptDispatchEntityMappingService;
        this.instanceHeadersRepository = instanceHeadersRepository;
    }

    public void prepareInstancesToDispatch() {
        instanceHeadersRepository.findAll()
                .forEach(this::prepareInstanceToDispatch);
    }

    public void handleNewInstanceToDispatch(InstanceHeadersEntity instanceHeadersEntity) {
        instanceHeadersRepository.save(instanceHeadersEntity);
        prepareInstanceToDispatch(instanceHeadersEntity);
    }

    private void prepareInstanceToDispatch(InstanceHeadersEntity instanceHeadersEntity) {
        Optional<InstanceReceiptDispatchEntity> instanceToDispatchEntity = instanceHeadersEntityToInstanceReceiptDispatchEntityMappingService.map(instanceHeadersEntity);



        instanceToDispatchEntity.ifPresent(webClientRequestService::dispatchInstance);
    }

    public void delete(InstanceHeadersEntity instanceHeadersEntity) {
        instanceHeadersRepository.delete(instanceHeadersEntity);
    }


}
