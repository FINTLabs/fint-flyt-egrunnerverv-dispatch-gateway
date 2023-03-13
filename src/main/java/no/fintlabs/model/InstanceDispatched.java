package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InstanceDispatched {

    @JsonProperty("arkivnummer")
    public String archiveInstanceId;
    @JsonProperty("u_elements")
    private String urlToArchive;
    @JsonProperty("opprettelse_i_elements_fullfort")
    private String archivedTimestamp;
}
