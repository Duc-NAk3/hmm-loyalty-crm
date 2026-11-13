package vn.sofitech.hmm.report;

import vn.sofitech.hmm.customer.api.NewCustomer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sofitech.hmm.PostgresTestBase;
import vn.sofitech.hmm.admin.domain.Store;
import vn.sofitech.hmm.admin.repository.StoreRepository;
import vn.sofitech.hmm.catalog.domain.Category;
import vn.sofitech.hmm.catalog.domain.Product;
import vn.sofitech.hmm.catalog.dto.product.ProductDto;
import vn.sofitech.hmm.catalog.repository.CategoryRepository;
import vn.sofitech.hmm.catalog.repository.ProductRepository;
import vn.sofitech.hmm.catalog.service.ProductAdminService;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.domain.Tier;
import vn.sofitech.hmm.report.dto.customer.CustomerRowDto;
import vn.sofitech.hmm.report.service.CustomerQueryService;
import vn.sofitech.hmm.shared.PageResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bộ lọc tùy chọn trên Portal: tìm khách (Criteria API) và tìm mặt hàng (Specification).
 * Lỗi cũ: tìm khách KHÔNG có từ khóa làm PostgreSQL báo "function upper(bytea) does not exist" vì tham số null không rõ kiểu.
 */
@EnabledIf("vn.sofitech.hmm.PostgresTestBase#databaseAvailable")
class SearchFilterTest extends PostgresTestBase {

    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired StoreRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired CustomerFacade customers;
    @Autowired CustomerQueryService customerSearch;
    @Autowired ProductAdminService productSearch;

    long storeA, storeB, catMilk, catVeg;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            em.createNativeQuery("TRUNCATE admin.stores, admin.roles, catalog.categories, loyalty.tiers, loyalty.loyalty_policies, "
                    + "shared.outbox_messages CASCADE").executeUpdate();
            storeA = stores.save(new Store("HMA", "Cửa hàng A", null, null, null)).getId();
            storeB = stores.save(new Store("HMB", "Cửa hàng B", null, null, null)).getId();
            em.persist(new Tier("MEMBER", "Thành viên", 0, BigDecimal.ONE, null));
            em.persist(new Tier("GOLD", "Vàng", 10_000_000, new BigDecimal("1.5"), null));

            long lan = customers.create(new NewCustomer("0901234567", "Nguyễn Thị Lan", null, null, null, storeA, "VERIFIED", null));
            customers.create(new NewCustomer("0912345678", "Trần Văn Hùng", null, null, null, storeA, "UNVERIFIED", null));
            customers.create(new NewCustomer("0935123456", "Phạm Minh Tuấn", null, null, null, storeB, "UNVERIFIED", null));
            long merged = customers.create(new NewCustomer("0987000111", "Lan Đã Gộp", null, null, null, storeB, "UNVERIFIED", null));
            customers.changeTier(lan, "GOLD");
            em.flush();
            em.createNativeQuery("update customer.customers set status = 'MERGED' where customer_id = " + merged).executeUpdate();

            Category milk = categories.save(new Category("SUA", "Sữa", false));
            Category veg = categories.save(new Category("RAU", "Rau củ", false));
            catMilk = milk.getId();
            catVeg = veg.getId();
            Product p1 = new Product("SUA001", "Sữa tươi 1L", "Hộp", milk, BigDecimal.valueOf(8), false, false);
            p1.replaceBarcodes(List.of("8930000000017", "8930000000024"));
            products.save(p1);
            products.save(new Product("SUA002", "Sữa chua dâu", "Hộp", milk, BigDecimal.valueOf(8), false, false));
            products.save(new Product("RAU001", "Cải ngọt 500g", "Gói", veg, BigDecimal.valueOf(5), false, false));
        });
    }

    private PageResult<CustomerRowDto> findCustomers(String q, String tier, String status, Long store) {
        return customerSearch.search(q, tier, status, store, 0, 20);
    }

    private List<String> namesOf(PageResult<CustomerRowDto> page) {
        return page.items().stream().map(CustomerRowDto::getFullName).toList();
    }

    @Test
    @DisplayName("Tìm khách KHÔNG có từ khóa (lỗi cũ upper(bytea)) và chỉ lọc hạng / trạng thái / cửa hàng")
    void customerFiltersWithoutKeyword() {
        PageResult<CustomerRowDto> all = findCustomers(null, null, null, null);   // gọi đầu tiên không có từ khóa
        assertThat(all.total()).isEqualTo(3);                              // hồ sơ đã gộp bị loại
        assertThat(namesOf(all)).doesNotContain("Lan Đã Gộp");
        assertThat(all.items()).allSatisfy(r -> assertThat(r.getPhone()).contains("****"));

        assertThat(namesOf(findCustomers("  ", "GOLD", null, null))).containsExactly("Nguyễn Thị Lan");
        assertThat(findCustomers(null, "GOLD", null, null).items().getFirst().getTierName()).isEqualTo("Vàng");
        assertThat(findCustomers(null, null, "UNVERIFIED", null).total()).isEqualTo(2);
        assertThat(findCustomers(null, null, null, storeB).items().getFirst().getRegisteredStore()).isEqualTo("HMB");
        assertThat(findCustomers(null, "MEMBER", "UNVERIFIED", storeA).total()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tìm khách theo tên không dấu, SĐT dạng +84, mã thành viên chữ thường")
    void customerKeyword() {
        assertThat(namesOf(findCustomers("lan", null, null, null))).containsExactly("Nguyễn Thị Lan");
        assertThat(namesOf(findCustomers("TUẤN", null, null, null))).containsExactly("Phạm Minh Tuấn");
        assertThat(namesOf(findCustomers("+84 912 345 678", null, null, null))).containsExactly("Trần Văn Hùng");
        String code = findCustomers("lan", null, null, null).items().getFirst().getMemberCode();
        assertThat(namesOf(findCustomers(code.toLowerCase(), null, null, null))).containsExactly("Nguyễn Thị Lan");
        assertThat(findCustomers("khongco", null, null, null).total()).isZero();
    }

    @Test
    @DisplayName("Tìm mặt hàng (Specification): không lọc, theo mã / một phần tên / mã vạch, theo ngành hàng")
    void productFilters() {
        assertThat(skus(null, null)).containsExactly("RAU001", "SUA001", "SUA002");
        assertThat(skus("sua002", null)).isEmpty();                      // mã hàng phải khớp đúng
        assertThat(skus("SUA002", null)).containsExactly("SUA002");
        assertThat(skus("SỮA", null)).containsExactly("SUA001", "SUA002"); // một phần tên, không phân biệt hoa thường
        assertThat(skus("8930000000024", null)).containsExactly("SUA001"); // mã vạch thứ hai
        assertThat(skus(null, catVeg)).containsExactly("RAU001");
        assertThat(skus("sữa", catVeg)).isEmpty();
        assertThat(productSearch.list(null, catMilk, 0, 20).total()).isEqualTo(2);
    }

    private List<String> skus(String q, Long categoryId) {
        return productSearch.list(q, categoryId, 0, 20).items().stream().map(ProductDto::getSku).toList();
    }
}
