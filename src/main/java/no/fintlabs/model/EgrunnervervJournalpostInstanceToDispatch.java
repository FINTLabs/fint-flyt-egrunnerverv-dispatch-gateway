package no.fintlabs.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class EgrunnervervJournalpostInstanceToDispatch {
    private final String statusId;
    private final String dokumentTypeNavn;
    private final String dokumentkategoriId;
    private final String dokumentkategoriNavn;
    private final String saksansvarligBrukernavn;
    private final String saksansvarligNavn;
    private final String adminEnhetKortnavn;
    private final String adminEnhetNavn;
}
