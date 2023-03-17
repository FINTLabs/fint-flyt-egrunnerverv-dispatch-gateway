package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@Jacksonized
public class InstanceCaseToDispatch {
    @JsonProperty("arkivnummer")
    public String archiveInstanceId;
    @JsonProperty("u_elements")
    private String urlToArchive;
    @JsonProperty("opprettelse_i_elements_fullfort")
    private String archivedTimestamp;
}
