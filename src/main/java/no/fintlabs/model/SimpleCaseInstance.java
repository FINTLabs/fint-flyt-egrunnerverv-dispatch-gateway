package no.fintlabs.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SimpleCaseInstance {
    private String sysId;
    private String tableName;
}
