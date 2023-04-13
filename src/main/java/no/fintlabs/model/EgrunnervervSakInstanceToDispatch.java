package no.fintlabs.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EgrunnervervSakInstanceToDispatch {
    private final String arkivnummer;
    private final String opprettelse_i_elements_fullfort;
}
