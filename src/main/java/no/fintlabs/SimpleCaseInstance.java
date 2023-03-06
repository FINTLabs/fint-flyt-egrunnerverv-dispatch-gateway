package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SimpleCaseInstance {
    @NotNull
    private String sysId;
    @NotNull
    private String tableName;
}
