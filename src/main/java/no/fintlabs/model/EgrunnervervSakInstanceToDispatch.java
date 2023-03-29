package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class EgrunnervervSakInstanceToDispatch {
    @JsonProperty("arkivnummer")
    private final String archiveInstanceId;
    @JsonProperty("u_elements")
    private final String urlToArchive;
    @JsonProperty("opprettelse_i_elements_fullfort")
    private final String archivedTimestamp;
}
