package vn.sofitech.hmm.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Ngành hàng, có thể nhiều cấp (catalog.categories). */
@Entity
@Table(schema = "catalog", name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** Ngành hàng bị hạn chế khuyến mại (BRL-05). */
    @Column(name = "promo_restricted", nullable = false)
    private boolean promoRestricted;

    public Category(String code, String name, boolean promoRestricted) {
        this.code = code;
        this.name = name;
        this.promoRestricted = promoRestricted;
    }
}
