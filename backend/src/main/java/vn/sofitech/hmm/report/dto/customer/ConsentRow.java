package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRow {

    private String purpose;
    private String status;
    private String channel;
    private String termsVersion;
    private OffsetDateTime capturedAt;
    private String capturedBy;
}
