package vn.sofitech.hmm.pos.dto.redemption;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.sofitech.hmm.pos.dto.sale.LineInputDto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequestDto {

    private long customerId;
    private List<LineInputDto> lines;
}
