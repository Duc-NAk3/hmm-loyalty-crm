package vn.sofitech.hmm.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Quầy thu ngân (máy POS) đã đăng ký với cửa hàng (admin.pos_terminals). */
@Entity
@Table(schema = "admin", name = "pos_terminals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PosTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "terminal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "terminal_code", nullable = false, unique = true)
    private String terminalCode;

    @Column(name = "device_key_hash")
    private String deviceKeyHash;

    @Column(nullable = false)
    private String status;

    @Column(name = "registered_at")
    private OffsetDateTime registeredAt;

    public PosTerminal(Store store, String terminalCode, OffsetDateTime registeredAt) {
        this.store = store;
        this.terminalCode = terminalCode;
        this.registeredAt = registeredAt;
        this.status = "ACTIVE";
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
