package com.sitepulse.engine.visualization.application.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class VisualizationBatchResult {

    private int imagesFound;
    private int imagesProcessed;
    private List<String> errors;
}
