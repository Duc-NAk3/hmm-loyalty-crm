package vn.sofitech.hmm.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Cửa hàng (admin.stores). */
@Entity
@Table(schema = "admin", name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(name = "store_code", nullable = false, unique = true)
    private String storeCode;

    @Column(nullable = false)
    private String name;

    private String region;
    private String address;

    @Column(nullable = false)
    private String status;

    @Column(name = "opened_at")
    private LocalDate openedAt;

    public Store(String storeCode, String name, String region, String address, LocalDate openedAt) {
        this.storeCode = storeCode;
        this.name = name;
        this.region = region;
        this.address = address;
        this.openedAt = openedAt;
        this.status = "ACTIVE";
    }
}
