package no.fintlabs.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@ToString
@Jacksonized
public class SakReceipt {
    private final String arkivnummer;
    private final String opprettelse_i_elements_fullfort;
}
