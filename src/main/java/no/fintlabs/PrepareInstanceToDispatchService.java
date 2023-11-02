package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.arkiv.noark.SakResource;
import no.fintlabs.kafka.CaseRequestService;
import no.fintlabs.model.EgrunnervervJournalpostInstanceToDispatch;
import no.fintlabs.model.EgrunnervervSakInstanceToDispatch;
import no.fintlabs.model.InstanceToDispatchEntity;
import no.fintlabs.model.PrepareInstanceToDispatchEntity;
import no.fintlabs.repositories.InstanceToDispatchEntityRepository;
import no.fintlabs.repositories.PrepareInstanceToDispatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class PrepareInstanceToDispatchService {
    private final PrepareInstanceToDispatchRepository prepareInstanceToDispatchRepository;
    public static final String EGRUNNERVERV_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private final CaseRequestService caseRequestService;
    private final EgrunnervervJournalpostInstanceToDispatchMappingService journalpostInstanceToDispatchMappingService;
    private final InstanceToDispatchEntityRepository instanceToDispatchEntityRepository;
    private final WebClientRequestService webClientRequestService;
    private final ObjectMapper objectMapper;
    private final String tablenameSak;
    private final String tablenameJournalpost;

    public PrepareInstanceToDispatchService(
            CaseRequestService caseRequestService,
            EgrunnervervJournalpostInstanceToDispatchMappingService journalpostInstanceToDispatchMappingService,
            InstanceToDispatchEntityRepository instanceToDispatchEntityRepository,
            WebClientRequestService webClientRequestService, ObjectMapper objectMapper,
            @Value("${fint.flyt.egrunnerverv.tablenameSak}") String tablenameSak,
            @Value("${fint.flyt.egrunnerverv.tablenameJournalpost}") String tablenameJournalpost,
            PrepareInstanceToDispatchRepository prepareInstanceToDispatchRepository) {
        this.caseRequestService = caseRequestService;
        this.journalpostInstanceToDispatchMappingService = journalpostInstanceToDispatchMappingService;
        this.instanceToDispatchEntityRepository = instanceToDispatchEntityRepository;
        this.webClientRequestService = webClientRequestService;
        this.objectMapper = objectMapper;
        this.tablenameSak = tablenameSak;
        this.tablenameJournalpost = tablenameJournalpost;
        this.prepareInstanceToDispatchRepository = prepareInstanceToDispatchRepository;
    }

    @Scheduled(
            initialDelayString = "${fint.flyt.egrunnerverv.instance-dispatch-initial-delay}",
            fixedDelayString = "${fint.flyt.egrunnerverv.instance-dispatch-fixed-delay}"
    )
    private synchronized void prepareInstancesToDispatch() {
        prepareInstanceToDispatchRepository.findAll()
                .forEach(this::prepareInstanceToDispatch);
    }

    public synchronized void prepareInstanceToDispatch(PrepareInstanceToDispatchEntity prepareInstanceToDispatchEntity) {
        try {
            Optional<InstanceToDispatchEntity> instanceToDispatchEntity =
                    switch (prepareInstanceToDispatchEntity.getSourceApplicationIntegrationId()) {
                        case "sak" -> storeSakInstanceToDispatch(
                                tablenameSak,
                                prepareInstanceToDispatchEntity.getSourceApplicationInstanceId(),
                                prepareInstanceToDispatchEntity.getArchiveInstanceId()
                        );
                        case "journalpost" -> storeJournalpostInstanceToDispatch(
                                tablenameJournalpost,
                                prepareInstanceToDispatchEntity.getSourceApplicationInstanceId(),
                                prepareInstanceToDispatchEntity.getArchiveInstanceId()
                        );
                        default ->
                                throw new IllegalStateException("Unexpected value: " + prepareInstanceToDispatchEntity.getSourceApplicationIntegrationId());
                    };
            instanceToDispatchEntity.ifPresent(webClientRequestService::dispatchInstance);

            prepareInstanceToDispatchRepository.delete(prepareInstanceToDispatchEntity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<InstanceToDispatchEntity> storeSakInstanceToDispatch(
            String tableName,
            String sourceApplicationInstanceId,
            String archiveInstanceId
    ) throws JsonProcessingException {
        SakResource sakResource = caseRequestService.getByMappeId(archiveInstanceId).orElseThrow();

        EgrunnervervSakInstanceToDispatch egrunnervervSakInstanceToDispatch = EgrunnervervSakInstanceToDispatch.builder()
                .arkivnummer(archiveInstanceId)
                .opprettelse_i_elements_fullfort(
                        sakResource
                                .getOpprettetDato()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern(EGRUNNERVERV_DATETIME_FORMAT))
                )
                .build();

        String uri = UriComponentsBuilder.newInstance()
                .pathSegment(
                        tableName,
                        sourceApplicationInstanceId
                )
                .queryParam("sysparm_fields", "arkivnummer")
                .queryParam("sysparm_query_no_domain", "true")
                .toUriString();

        InstanceToDispatchEntity instanceToDispatchEntity = InstanceToDispatchEntity.builder()
                .sourceApplicationInstanceId(sourceApplicationInstanceId)
                .instanceToDispatch(objectMapper.writeValueAsString(egrunnervervSakInstanceToDispatch))
                .classType(EgrunnervervSakInstanceToDispatch.class)
                .uri(uri)
                .build();

        instanceToDispatchEntityRepository.save(instanceToDispatchEntity);
        return Optional.of(instanceToDispatchEntity);
    }

    private Optional<InstanceToDispatchEntity> storeJournalpostInstanceToDispatch(
            String tableName,
            String sourceApplicationInstanceId,
            String archiveInstanceId
    ) throws JsonProcessingException {
        String[] splitArchiveInstanceId = archiveInstanceId.split("-");
        String caseId = splitArchiveInstanceId[0];
        Long journalpostNummer = Long.parseLong(
                splitArchiveInstanceId[1]
                        .replace("[", "")
                        .replace("]", "")
        );
        SakResource sakResource = caseRequestService.getByMappeId(caseId).orElseThrow();
        EgrunnervervJournalpostInstanceToDispatch egrunnervervJournalpostInstanceToDispatch =
                journalpostInstanceToDispatchMappingService.map(sakResource, journalpostNummer);

        String uri = UriComponentsBuilder.newInstance()
                .pathSegment(
                        tableName,
                        sourceApplicationInstanceId
                )
                .queryParam("sysparm_fields", "journalpostnr")
                .queryParam("sysparm_query_no_domain", "true")
                .toUriString();

        InstanceToDispatchEntity instanceToDispatchEntity = InstanceToDispatchEntity.builder()
                .sourceApplicationInstanceId(sourceApplicationInstanceId)
                .instanceToDispatch(objectMapper.writeValueAsString(egrunnervervJournalpostInstanceToDispatch))
                .classType(EgrunnervervJournalpostInstanceToDispatch.class)
                .uri(uri)
                .build();

        instanceToDispatchEntityRepository.save(instanceToDispatchEntity);
        return Optional.of(instanceToDispatchEntity);
    }

}
