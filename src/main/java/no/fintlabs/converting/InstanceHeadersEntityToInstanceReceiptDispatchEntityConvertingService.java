package no.fintlabs.converting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.fintlabs.kafka.CaseRequestService;
import no.fintlabs.model.InstanceHeadersEntity;
import no.fintlabs.model.InstanceReceiptDispatchEntity;
import no.fintlabs.model.JournalpostReceipt;
import no.fintlabs.model.SakReceipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class InstanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService {

    private final String tablenameSak;
    private final String tablenameJournalpost;
    private final CaseRequestService caseRequestService;
    private final ObjectMapper objectMapper;
    private final JournalpostToInstanceReceiptDispatchEntityConvertingService journalpostToInstanceReceiptDispatchEntityConvertingService;

    public static final String EGRUNNERVERV_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";


    public InstanceHeadersEntityToInstanceReceiptDispatchEntityConvertingService(
            @Value("${fint.flyt.egrunnerverv.tablenameSak}") String tablenameSak,
            @Value("${fint.flyt.egrunnerverv.tablenameJournalpost}") String tablenameJournalpost,
            CaseRequestService caseRequestService,
            ObjectMapper objectMapper,
            JournalpostToInstanceReceiptDispatchEntityConvertingService journalpostToInstanceReceiptDispatchEntityConvertingService
    ) {
        this.tablenameSak = tablenameSak;
        this.tablenameJournalpost = tablenameJournalpost;
        this.caseRequestService = caseRequestService;
        this.objectMapper = objectMapper;
        this.journalpostToInstanceReceiptDispatchEntityConvertingService =
                journalpostToInstanceReceiptDispatchEntityConvertingService;
    }

    public Optional<InstanceReceiptDispatchEntity> convert(InstanceHeadersEntity instanceHeadersEntity) {
        return switch (instanceHeadersEntity.getSourceApplicationIntegrationId()) {
            case "sak" -> convertSak(instanceHeadersEntity);
            case "journalpost" -> convertJournalpost(instanceHeadersEntity);
            default -> throw new IllegalStateException(
                    "Unexpected value: " + instanceHeadersEntity.getSourceApplicationIntegrationId()
            );
        };
    }

    private Optional<InstanceReceiptDispatchEntity> convertSak(InstanceHeadersEntity instanceHeadersEntity) {

        String archiveInstanceId = instanceHeadersEntity.getArchiveInstanceId();
        String sourceApplicationInstanceId = instanceHeadersEntity.getSourceApplicationInstanceId();

        return caseRequestService.getByMappeId(archiveInstanceId)
                .map(sakResource -> {
                    SakReceipt sakReceipt = SakReceipt.builder()
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
                                    tablenameSak,
                                    sourceApplicationInstanceId
                            )
                            .queryParam("sysparm_fields", "arkivnummer")
                            .queryParam("sysparm_query_no_domain", "true")
                            .toUriString();

                    try {
                        return InstanceReceiptDispatchEntity.builder()
                                .sourceApplicationInstanceId(sourceApplicationInstanceId)
                                .instanceReceipt(objectMapper.writeValueAsString(sakReceipt))
                                .classType(SakReceipt.class)
                                .uri(uri)
                                .build();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                });
    }

    private Optional<InstanceReceiptDispatchEntity> convertJournalpost(InstanceHeadersEntity instanceHeadersEntity) {

        String sourceApplicationInstanceId = instanceHeadersEntity.getSourceApplicationInstanceId();

        String[] splitArchiveInstanceId = instanceHeadersEntity.getArchiveInstanceId().split("-");
        String caseId = splitArchiveInstanceId[0];
        Long journalpostNummer = Long.parseLong(
                splitArchiveInstanceId[1]
                        .replace("[", "")
                        .replace("]", "")
        );
        return caseRequestService.getByMappeId(caseId)
                .map(sakResource -> {
                    JournalpostReceipt journalpostReceipt =
                            journalpostToInstanceReceiptDispatchEntityConvertingService
                                    .map(sakResource, journalpostNummer);

                    String uri = UriComponentsBuilder.newInstance()
                            .pathSegment(
                                    tablenameJournalpost,
                                    sourceApplicationInstanceId
                            )
                            .queryParam("sysparm_fields", "journalpostnr")
                            .queryParam("sysparm_query_no_domain", "true")
                            .toUriString();

                    InstanceReceiptDispatchEntity instanceReceiptDispatchEntity;
                    try {
                        instanceReceiptDispatchEntity = InstanceReceiptDispatchEntity.builder()
                                .sourceApplicationInstanceId(sourceApplicationInstanceId)
                                .instanceReceipt(objectMapper.writeValueAsString(journalpostReceipt))
                                .classType(JournalpostReceipt.class)
                                .uri(uri)
                                .build();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    return instanceReceiptDispatchEntity;

                });
    }

}
