package com.miniapi.router.core.intent;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class IntentResult {
    private String intent;
    private int score;
    private String reasoning;
}
