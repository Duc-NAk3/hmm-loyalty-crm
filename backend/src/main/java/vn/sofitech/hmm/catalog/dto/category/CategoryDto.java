package vn.sofitech.hmm.catalog.dto.category;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private Long categoryId;
    private String code;
    private String name;
    private Long parentId;
    private boolean promoRestricted;
}
