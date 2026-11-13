package vn.sofitech.hmm.admin.dto.store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreDto {

    private Long storeId;
    private String storeCode;
    private String name;
    private String region;
    private String address;
    private String status;
    private LocalDate openedAt;
    private long terminalCount;
}
