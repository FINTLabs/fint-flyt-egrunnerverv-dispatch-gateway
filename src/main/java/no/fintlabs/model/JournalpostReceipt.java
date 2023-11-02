package no.fintlabs.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class JournalpostReceipt {
    private final String journalpostnr;
    private final String tittel;
    private final String statusId;
    private final String tilgangskode;
    private final String hjemmel;
    private final String dokumentdato;
    private final String dokumenttypeid;
    private final String dokumenttypenavn;
    private final String saksansvarligbrukernavn;
    private final String saksansvarlignavn;
    private final String adminenhetkortnavn;
    private final String adminenhetnavn;
}
