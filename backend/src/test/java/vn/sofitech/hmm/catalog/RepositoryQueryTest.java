package vn.sofitech.hmm.catalog;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sofitech.hmm.PostgresTestBase;
import vn.sofitech.hmm.admin.domain.*;
import vn.sofitech.hmm.catalog.domain.*;
import vn.sofitech.hmm.catalog.service.PriceService;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.domain.PointLot;
import vn.sofitech.hmm.loyalty.domain.PointLotRepository;
import vn.sofitech.hmm.pos.domain.SalesTransaction;
import vn.sofitech.hmm.pos.domain.SalesTransactionRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra các truy vấn JPA quan trọng trên PostgreSQL thật:
 * giá đang hiệu lực (giá riêng cửa hàng ưu tiên, bỏ giá nháp và giá tương lai – FR-12) và truy vấn lô điểm FIFO (BRL-10).
 */
@EnabledIf("vn.sofitech.hmm.PostgresTestBase#databaseAvailable")
class RepositoryQueryTest extends PostgresTestBase {

    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired CatalogFacade catalog;
    @Autowired PriceService priceService;
    @Autowired StoreRepository stores;
    @Autowired RoleRepository roles;
    @Autowired AppUserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductPriceRepository prices;
    @Autowired CustomerFacade customers;
    @Autowired SalesTransactionRepository transactions;
    @Autowired PointLotRepository lots;

    long storeA, storeB, userId, productId;
    final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            em.createNativeQuery("TRUNCATE admin.stores, admin.roles, catalog.categories, loyalty.tiers, loyalty.loyalty_policies, "
                    + "shared.outbox_messages CASCADE").executeUpdate();
            Store a = stores.save(new Store("HMA", "Cửa hàng A", null, null, null));
            storeA = a.getId();
            storeB = stores.save(new Store("HMB", "Cửa hàng B", null, null, null)).getId();
            userId = users.save(new AppUser("danhmuc", "x", "Danh mục", null, roles.save(new Role("CATALOG", "Danh mục và giá")), null)).getId();
            em.persist(new vn.sofitech.hmm.loyalty.domain.Tier("MEMBER", "Thành viên", 0, BigDecimal.ONE, null));
            Category cat = categories.save(new Category("SUA", "Sữa", false));
            Product p = new Product("SUA001", "Sữa tươi 1L", "Hộp", cat, BigDecimal.valueOf(8), false, false);
            p.replaceBarcodes(List.of("8930000000017"));
            productId = products.save(p).getId();
        });
    }

    private void price(Long storeId, long value, OffsetDateTime from, boolean approved) {
        tx.executeWithoutResult(s -> {
            ProductPrice pp = new ProductPrice(products.getReferenceById(productId), storeId, value, from);
            if (approved) pp.approve(userId);
            prices.save(pp);
        });
    }

    private long sellingPrice(long storeId) {
        return catalog.sellableByIds(List.of(productId), storeId).get(productId).price();
    }

    @Test
    @DisplayName("FR-12: giá riêng cửa hàng ưu tiên hơn giá toàn chuỗi; giá nháp và giá tương lai chưa áp dụng")
    void effectivePrice() {
        price(null, 100_000, now.minusDays(10), true);   // giá toàn chuỗi
        price(storeA, 90_000, now.minusDays(5), true);   // giá riêng cửa hàng A
        price(null, 120_000, now.minusDays(1), false);   // bản nháp chưa duyệt
        price(null, 130_000, now.plusDays(1), true);     // đã duyệt nhưng chưa tới ngày hiệu lực

        assertThat(sellingPrice(storeA)).isEqualTo(90_000);
        assertThat(sellingPrice(storeB)).isEqualTo(100_000);
    }

    @Test
    @DisplayName("FR-12: duyệt giá mới thì bản giá cũ cùng phạm vi được đóng hiệu lực, POS lấy giá mới")
    void approvingNewPriceClosesOldOne() {
        price(null, 100_000, now.minusDays(10), true);
        price(storeA, 90_000, now.minusDays(5), true);
        OffsetDateTime from = now.minusHours(1).truncatedTo(java.time.temporal.ChronoUnit.SECONDS); // PostgreSQL làm tròn tới micro giây
        long newId = priceService.create(productId, null, 110_000, from, userId);
        priceService.approve(newId, userId);

        assertThat(sellingPrice(storeB)).isEqualTo(110_000);
        assertThat(sellingPrice(storeA)).isEqualTo(90_000); // giá riêng cửa hàng không bị ảnh hưởng
        ProductPrice old = prices.findByProductIdOrderByEffectiveFromDescIdDesc(productId).stream()
                .filter(pp -> pp.getPrice() == 100_000).findFirst().orElseThrow();
        assertThat(old.getEffectiveTo().toInstant()).isEqualTo(from.toInstant()); // cùng thời điểm, CSDL trả về theo UTC
    }

    @Test
    @DisplayName("BRL-10: truy vấn lô FIFO chỉ lấy lô còn điểm, còn hạn, cũ nhất trước; tính đúng điểm sắp hết hạn")
    void fifoLotQuery() {
        long[] ids = tx.execute(s -> {
            long customerId = customers.create(new CustomerFacade.NewCustomer("0901112222", "Khách Test", null, null, null, storeA, "UNVERIFIED", null));
            long txnId = transactions.save(SalesTransaction.sale("R1", storeA, null, null, null, customerId, now)).getId();
            PointLot soon = lots.save(new PointLot(customerId, txnId, now.minusDays(340), now.plusDays(10), 7));       // sắp hết hạn
            PointLot old = lots.save(new PointLot(customerId, txnId, now.minusDays(60), now.plusMonths(10), 10));
            PointLot used = new PointLot(customerId, txnId, now.minusDays(30), now.plusMonths(11), 5);
            used.consume(5);                                                                                        // đã dùng hết
            lots.save(used);
            lots.save(new PointLot(customerId, txnId, now.minusDays(400), now.minusDays(5), 50));                 // đã hết hạn
            PointLot recent = lots.save(new PointLot(customerId, txnId, now.minusDays(5), now.plusMonths(12), 20));
            return new long[]{customerId, soon.getId(), old.getId(), recent.getId()};
        });
        List<Long> fifo = tx.execute(s -> lots.findOpenLotsForUpdate(ids[0], now).stream().map(PointLot::getId).toList());
        assertThat(fifo).containsExactly(ids[1], ids[2], ids[3]);
        assertThat(lots.sumExpiring(ids[0], now, now.plusDays(30))).isEqualTo(7);
    }
}
