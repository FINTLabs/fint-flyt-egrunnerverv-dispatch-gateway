package no.fintlabs.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@ToString
@Jacksonized
public class EgrunnervervJournalpostInstanceToDispatch {
    private final String journalpostnr;
    private final String tittel;
    private final String statusId;
    private final String tilgangskode;
    private final String hjemmel;
    private final String dokumentDato;
    private final String dokumentTypeId;
    private final String dokumentTypeNavn;
    private final String dokumentkategoriId;
    private final String dokumentkategoriNavn;
    private final String saksansvarligBrukernavn;
    private final String saksansvarligNavn;
    private final String adminEnhetKortnavn;
    private final String adminEnhetNavn;
    private final Long antallVedlegg;
}
