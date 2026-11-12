package vn.sofitech.hmm.demo;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sofitech.hmm.admin.domain.*;
import vn.sofitech.hmm.catalog.domain.Category;
import vn.sofitech.hmm.catalog.domain.Product;
import vn.sofitech.hmm.catalog.domain.ProductPrice;
import vn.sofitech.hmm.customer.domain.Consent;
import vn.sofitech.hmm.customer.domain.Customer;
import vn.sofitech.hmm.customer.domain.CustomerRepository;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.PointCalculator.Policy;
import vn.sofitech.hmm.loyalty.domain.*;
import vn.sofitech.hmm.pos.domain.Payment;
import vn.sofitech.hmm.pos.domain.SalesTransaction;
import vn.sofitech.hmm.pos.domain.Shift;
import vn.sofitech.hmm.shared.Texts;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static vn.sofitech.hmm.shared.Texts.VN;

/**
 * Tạo dữ liệu mẫu khi CSDL còn trống (ghi qua JPA): 3 cửa hàng, 6 quầy, người dùng các vai trò, 115 mặt hàng, 800 khách và
 * lịch sử bán hàng từ 01/12/2025 đến hôm qua. Điểm, lô điểm, sổ cái và hạng được tính bằng chính {@link PointCalculator}
 * và cùng quy ước sổ cái với hệ thống thật (EARN; HOLD → RELEASE → REDEEM theo FIFO), nên số dư luôn bằng tổng sổ cái.
 * Random có seed cố định: lần nào chạy cũng ra cùng dữ liệu.
 * <p>
 * Ghi theo từng ngày, mỗi ngày 3 lượt flush để đúng thứ tự khóa ngoại (các liên kết khác module chỉ là ID nên Hibernate
 * không tự sắp xếp): (1) ca, hóa đơn, dòng hàng, tiền mặt → (2) lần đổi điểm → (3) thanh toán bằng điểm, lô điểm, sổ cái, lịch sử hạng.
 * Công cụ này ghi trực tiếp vào mọi module nên được loại khỏi kiểm tra ranh giới module (ModularityTest).
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "Demo@123";
    static final ZonedDateTime HISTORY_START = LocalDate.of(2025, 12, 1).atStartOfDay(VN);
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    private static final long OPENING_CASH = 2_000_000;

    private final EntityManager em;
    private final StoreRepository storeRepo;
    private final CustomerRepository customerRepo;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final Random rnd = new Random(20261111L);

    public DemoDataSeeder(EntityManager em, StoreRepository storeRepo, CustomerRepository customerRepo, PasswordEncoder encoder,
                          TransactionTemplate tx) {
        this.em = em;
        this.storeRepo = storeRepo;
        this.customerRepo = customerRepo;
        this.encoder = encoder;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (storeRepo.count() > 0) return;
        long t0 = System.currentTimeMillis();
        log.info("Empty database – seeding demo data...");
        tx.executeWithoutResult(s -> seed());
        log.info("Demo data seeded in {} ms", System.currentTimeMillis() - t0);
    }

    // ------------------------------------------------------------------ mô hình mô phỏng trong bộ nhớ
    private record StoreDef(long id, String code, String name, String region, String address, LocalDate opened, double baseDaily) {
    }

    private record ProductDef(long id, DemoCatalog.Category cat, DemoCatalog.Item item, boolean eligible) {
    }

    /** Lô điểm đang theo dõi FIFO trong mô phỏng. */
    private static final class Lot {
        final long id;
        final int earned;
        int remaining;

        Lot(long id, int earned) {
            this.id = id;
            this.earned = earned;
            this.remaining = earned;
        }
    }

    private static final class Cust {
        long id;
        String phone, name, gender, email, status = "UNVERIFIED", tier = "MEMBER";
        LocalDate dob;
        OffsetDateTime createdAt, verifiedAt;
        long storeId, spend;
        int balance;
        double weight;
        boolean redeemLocked, neverRedeem, marketing;
        final ArrayDeque<Lot> lots = new ArrayDeque<>();
    }

    private static final class ShiftDef {
        long id, terminalId, cashierId, managerId, cash;
        int seq;
    }

    /** Bản ghi chờ ghi ở lượt flush 2 và 3 của ngày. */
    private final List<PointRedemption> dayRedemptions = new ArrayList<>();
    private final List<Object[]> dayPointPayments = new ArrayList<>(); // [SalesTransaction, amount, PointRedemption]
    private final List<Object> dayLoyalty = new ArrayList<>();          // PointLot, PointLedger, TierHistory theo thứ tự tạo
    private final Map<Long, Lot> changedLots = new LinkedHashMap<>();
    private LoyaltyPolicy policyEntity;
    private int receiptCount, ledgerCount;

    // ------------------------------------------------------------------ seed
    private void seed() {
        Policy policy = new Policy(1, 10_000, 100, 50, 50, 12, 15);
        List<PointCalculator.Tier> tiers = List.of(
                new PointCalculator.Tier("MEMBER", "Thành viên", 0, new BigDecimal("1.00")),
                new PointCalculator.Tier("SILVER", "Bạc", 3_000_000, new BigDecimal("1.20")),
                new PointCalculator.Tier("GOLD", "Vàng", 10_000_000, new BigDecimal("1.50")),
                new PointCalculator.Tier("DIAMOND", "Kim cương", 30_000_000, new BigDecimal("2.00")));

        // --- quản trị
        List<StoreDef> stores = List.of(
                new StoreDef(1, "HM001", "Hoa Mai Mart Cầu Giấy", "Miền Bắc", "125 Trần Duy Hưng, Cầu Giấy, Hà Nội", LocalDate.of(2019, 5, 20), 24),
                new StoreDef(2, "HM002", "Hoa Mai Mart Hải Châu", "Miền Trung", "48 Nguyễn Văn Linh, Hải Châu, Đà Nẵng", LocalDate.of(2020, 9, 10), 17),
                new StoreDef(3, "HM003", "Hoa Mai Mart Quận 7", "Miền Nam", "302 Nguyễn Thị Thập, Quận 7, TP. Hồ Chí Minh", LocalDate.of(2021, 3, 15), 21));
        Map<Long, vn.sofitech.hmm.admin.domain.Store> storeEntities = new HashMap<>();
        for (StoreDef s : stores) {
            vn.sofitech.hmm.admin.domain.Store e = new vn.sofitech.hmm.admin.domain.Store(s.code(), s.name(), s.region(), s.address(), s.opened());
            em.persist(e);
            storeEntities.put(s.id(), e);
        }
        OffsetDateTime terminalReg = OffsetDateTime.of(2025, 11, 20, 9, 0, 0, 0, ZoneOffset.ofHours(7));
        for (StoreDef s : stores)
            for (int q = 1; q <= 2; q++) em.persist(new PosTerminal(storeEntities.get(s.id()), s.code() + "-Q0" + q, terminalReg));

        String[][] roleDefs = {{"ADMIN", "Quản trị hệ thống"}, {"STORE_MANAGER", "Quản lý cửa hàng"}, {"CASHIER", "Thu ngân"},
                {"CSKH", "Chăm sóc khách hàng"}, {"MARKETING", "Marketing"}, {"CATALOG", "Danh mục và giá"}};
        List<Role> roles = new ArrayList<>();
        for (String[] r : roleDefs) {
            Role role = new Role(r[0], r[1]);
            em.persist(role);
            roles.add(role);
        }
        String hash = encoder.encode(DEMO_PASSWORD);
        Object[][] users = {
                {"admin", "Quản trị hệ thống", 1, null},
                {"ql.hm001", "Lê Thị Mai", 2, 1L}, {"ql.hm002", "Phan Văn Đức", 2, 2L}, {"ql.hm003", "Võ Thị Ngọc", 2, 3L},
                {"tn.hm001.1", "Nguyễn Thu Hà", 3, 1L}, {"tn.hm001.2", "Đỗ Minh Khang", 3, 1L},
                {"tn.hm002.1", "Trần Thị Hồng", 3, 2L}, {"tn.hm002.2", "Lý Quốc Bảo", 3, 2L},
                {"tn.hm003.1", "Huỳnh Ngọc Trâm", 3, 3L}, {"tn.hm003.2", "Bùi Thanh Tùng", 3, 3L},
                {"cskh", "Đặng Phương Linh", 4, null}, {"marketing", "Hoàng Gia Huy", 5, null}, {"danhmuc", "Ngô Thị Yến", 6, null}};
        for (Object[] u : users)
            em.persist(new AppUser((String) u[0], hash, (String) u[1], null, roles.get((Integer) u[2] - 1),
                    u[3] == null ? null : storeEntities.get((Long) u[3])));

        OffsetDateTime now = OffsetDateTime.now(VN);
        em.persist(new SystemParameter("OTP_LENGTH", "6", "Số chữ số của mã OTP", 1L, now));
        em.persist(new SystemParameter("OTP_TTL_MINUTES", "5", "Thời hạn mã OTP (phút)", 1L, now));
        em.persist(new SystemParameter("OTP_MAX_ATTEMPTS", "3", "Số lần nhập sai OTP tối đa", 1L, now));

        // --- loyalty
        String[] benefits = {"Tích điểm, đổi điểm trừ tiền hóa đơn",
                "Quyền lợi hạng Thành viên + voucher sinh nhật 50.000đ",
                "Quyền lợi hạng Bạc + ưu đãi riêng hằng tháng, ưu tiên CSKH",
                "Quyền lợi hạng Vàng + quà tri ân, ưu đãi độc quyền, sự kiện riêng"};
        for (int i = 0; i < tiers.size(); i++) {
            PointCalculator.Tier t = tiers.get(i);
            em.persist(new vn.sofitech.hmm.loyalty.domain.Tier(t.code(), t.name(), t.thresholdSpend(), t.multiplier(), benefits[i]));
        }
        policyEntity = new LoyaltyPolicy(1, 10_000, 100, 50, 50, 12, 15, OffsetDateTime.of(2025, 11, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7)), 1L);
        em.persist(policyEntity);

        // --- danh mục và giá
        List<ProductDef> products = seedCatalog();
        Map<Long, long[]> priceChanges = seedPrices(products);
        em.flush();

        // --- khách hàng: ghi trạng thái ban đầu; trạng thái cuối (điểm, hạng...) cập nhật sau khi mô phỏng
        List<Cust> customers = buildCustomers();
        persistCustomers(customers);
        em.flush();
        em.clear();
        policyEntity = em.getReference(LoyaltyPolicy.class, policyEntity.getId());

        // --- mô phỏng bán hàng theo ngày
        LocalDate today = LocalDate.now(VN);
        Map<Long, double[]> cumByStore = new HashMap<>();
        for (StoreDef s : stores) cumByStore.put(s.id(), cumulative(customers.stream().filter(c -> c.storeId == s.id()).toList()));
        double[] cumAll = cumulative(customers);
        Map<Long, List<Cust>> byStore = new HashMap<>();
        for (StoreDef s : stores) byStore.put(s.id(), customers.stream().filter(c -> c.storeId == s.id()).toList());
        double[] catCum = new double[products.size()];
        double acc = 0;
        for (int i = 0; i < products.size(); i++) {
            ProductDef p = products.get(i);
            acc += (double) p.cat().weight() / p.cat().items().size();
            catCum[i] = acc;
        }
        List<ShiftDef> shifts = new ArrayList<>();
        long totalDays = Duration.between(HISTORY_START.toLocalDate().atStartOfDay(), today.atStartOfDay()).toDays();

        for (LocalDate day = HISTORY_START.toLocalDate(); day.isBefore(today); day = day.plusDays(1)) {
            long dayIndex = Duration.between(HISTORY_START.toLocalDate().atStartOfDay(), day.atStartOfDay()).toDays();
            double trend = 0.8 + 0.4 * dayIndex / Math.max(1, totalDays);
            double weekday = switch (day.getDayOfWeek()) {
                case SATURDAY, SUNDAY -> 1.35;
                case FRIDAY -> 1.1;
                default -> 1.0;
            };
            double tet = (day.getMonthValue() == 2 && day.getDayOfMonth() >= 10 && day.getDayOfMonth() <= 16) ? 1.8 : 1.0; // mua sắm Tết
            Map<Long, ShiftDef> dayShifts = new HashMap<>();
            for (StoreDef s : stores)
                for (int q = 1; q <= 2; q++) {
                    ShiftDef sh = new ShiftDef();
                    sh.terminalId = terminalId(s.id(), q);
                    sh.cashierId = 4 + sh.terminalId;
                    sh.managerId = 1 + s.id();
                    Shift entity = Shift.historical(sh.terminalId, sh.cashierId, day.atTime(6, 45).atZone(VN).toOffsetDateTime(), OPENING_CASH,
                            day.atTime(22, 15).atZone(VN).toOffsetDateTime());
                    em.persist(entity);
                    sh.id = entity.getId();
                    dayShifts.put(sh.terminalId, sh);
                    shifts.add(sh);
                }
            record Receipt(OffsetDateTime time, StoreDef store, ShiftDef shift) {
            }
            List<Receipt> receipts = new ArrayList<>();
            for (StoreDef s : stores) {
                int n = (int) Math.round(s.baseDaily() * trend * weekday * tet * (0.85 + 0.3 * rnd.nextDouble()));
                for (int i = 0; i < n; i++) {
                    int minute = 7 * 60 + (int) (rnd.nextDouble() * 15 * 60);
                    OffsetDateTime t = day.atTime(minute / 60, minute % 60, rnd.nextInt(60)).atZone(VN).toOffsetDateTime();
                    receipts.add(new Receipt(t, s, dayShifts.get(terminalId(s.id(), 1 + rnd.nextInt(2)))));
                }
            }
            receipts.sort(Comparator.comparing(Receipt::time));
            for (Receipt r : receipts) {
                double memberRate = 0.55 + 0.15 * dayIndex / Math.max(1, totalDays);
                Cust member = null;
                if (rnd.nextDouble() < memberRate) {
                    boolean local = rnd.nextDouble() < 0.88;
                    List<Cust> pool = local ? byStore.get(r.store().id()) : customers;
                    double[] cum = local ? cumByStore.get(r.store().id()) : cumAll;
                    for (int tries = 0; tries < 3 && member == null; tries++) {
                        Cust c = pool.get(pick(cum));
                        if (c.createdAt.isBefore(r.time())) member = c;
                    }
                }
                sell(r.time(), r.store(), r.shift(), member, products, catCum, priceChanges, policy, tiers);
            }
            flushDay();
        }

        // --- trạng thái cuối: đối chiếu tiền mặt của ca, số điểm còn lại của lô, hồ sơ khách
        Map<Long, long[]> shiftResult = new LinkedHashMap<>();
        for (ShiftDef sh : shifts) {
            long diff = rnd.nextDouble() < 0.04 ? (rnd.nextBoolean() ? 1 : -1) * 1_000L * (1 + rnd.nextInt(20)) : 0;
            shiftResult.put(sh.id, new long[]{sh.cash, OPENING_CASH + sh.cash + diff, sh.managerId});
        }
        updateInChunks(Shift.class, shiftResult.keySet(), (Shift s) -> {
            long[] v = shiftResult.get(s.getId());
            s.reconcile(v[0], v[1], v[2]);
        });
        updateInChunks(PointLot.class, changedLots.keySet(), (PointLot l) -> l.setPointsRemaining(changedLots.get(l.getId()).remaining));
        Map<Long, Cust> byId = new HashMap<>();
        customers.forEach(c -> byId.put(c.id, c));
        updateInChunks(Customer.class, byId.keySet(), (Customer e) -> {
            Cust c = byId.get(e.getId());
            e.adjustPoints(c.balance, true, now);
            e.addSpend(c.spend, now);
            e.changeTier(c.tier, now);
            if ("VERIFIED".equals(c.status)) e.markVerified(c.verifiedAt);
            if (c.redeemLocked) e.lockRedemption(true);
        });

        // giá chờ duyệt để demo FR-12
        Product first = em.find(Product.class, products.getFirst().id());
        em.persist(new ProductPrice(first, null, Math.round(products.getFirst().item().price() * 1.06 / 100) * 100,
                today.plusDays(1).atStartOfDay(VN).toOffsetDateTime()));
        em.flush();

        log.info("Seeded {} stores, {} products, {} customers, {} receipts, {} ledger entries",
                stores.size(), products.size(), customers.size(), receiptCount, ledgerCount);
    }

    /** Ghi dữ liệu của một ngày theo đúng thứ tự khóa ngoại rồi giải phóng bộ nhớ. */
    private void flushDay() {
        em.flush();                                              // (1) ca, hóa đơn, dòng hàng, tiền mặt
        dayRedemptions.forEach(em::persist);
        em.flush();                                              // (2) lần đổi điểm
        for (Object[] p : dayPointPayments)
            ((SalesTransaction) p[0]).addPayment(Payment.Method.POINTS, (Long) p[1], ((PointRedemption) p[2]).getId(), null);
        dayLoyalty.forEach(em::persist);
        em.flush();                                              // (3) thanh toán điểm, lô điểm, sổ cái, lịch sử hạng
        em.clear();
        policyEntity = em.getReference(LoyaltyPolicy.class, policyEntity.getId());
        dayRedemptions.clear();
        dayPointPayments.clear();
        dayLoyalty.clear();
    }

    // ------------------------------------------------------------------ bán một hóa đơn
    private void sell(OffsetDateTime time, StoreDef store, ShiftDef shift, Cust member, List<ProductDef> products, double[] catCum,
                      Map<Long, long[]> priceChanges, Policy policy, List<PointCalculator.Tier> tiers) {
        String receiptNo = store.code() + "-Q0" + (shift.terminalId - (store.id() - 1) * 2) + "-" + time.format(YYMMDD) + "-" + String.format("%04d", ++shift.seq);
        SalesTransaction txn = SalesTransaction.sale(receiptNo, store.id(), shift.terminalId, shift.id, shift.cashierId,
                member == null ? null : member.id, time);
        int k = Math.min(12, 1 + (int) (-Math.log(1 - rnd.nextDouble()) * 3.2));
        Set<Integer> chosen = new LinkedHashSet<>();
        while (chosen.size() < k) chosen.add(pick(catCum));
        long redeemable = 0;
        for (int idx : chosen) {
            ProductDef p = products.get(idx);
            int qty = rnd.nextDouble() < 0.75 ? 1 : 2 + rnd.nextInt(2);
            long unit = priceAt(p, store.id(), time, priceChanges);
            long amount = unit * qty;
            if (p.eligible()) redeemable += amount;
            txn.addLine(p.id(), BigDecimal.valueOf(qty), unit, amount, p.eligible());
        }
        long total = txn.getNetAmount();

        // Đổi điểm (khoảng 15% hóa đơn có đủ điểm) – tính trước trong bộ nhớ, ghi sau khi có ID hóa đơn
        int redeemPts = 0;
        OffsetDateTime otpAt = time.minusMinutes(1);
        PointCalculator.Tier tier = null;
        if (member != null) {
            tier = tiers.stream().filter(t -> t.code().equals(member.tier)).findFirst().orElseThrow();
            if (!member.neverRedeem && !member.redeemLocked && member.balance >= 50 && rnd.nextDouble() < 0.15) {
                PointCalculator.RedeemQuote q = PointCalculator.quote(redeemable, member.balance, policy);
                if (q.allowed()) {
                    int pts = Math.max(policy.minRedeemPoints(), (int) (q.maxPoints() * (0.5 + 0.5 * rnd.nextDouble())) / 10 * 10);
                    redeemPts = Math.min(pts, q.maxPoints());
                }
            }
        }
        long pointsAmount = (long) redeemPts * policy.pointValueVnd();
        long cash = total - pointsAmount;
        if (cash > 0) {
            long tendered = cash % 10_000 == 0 ? cash : (cash / 10_000 + 1) * 10_000;
            txn.addPayment(Payment.Method.CASH, cash, null, "tendered=" + tendered);
        }
        shift.cash += cash;
        long eligible = Math.max(0, redeemable - pointsAmount);
        int earned = member == null ? 0 : PointCalculator.earnedPoints(eligible, policy, tier.multiplier(), BigDecimal.ONE);
        if (member != null) txn.applyLoyalty(eligible, earned, redeemPts);
        em.persist(txn); // ID hóa đơn có ngay (sequence), dòng hàng và tiền mặt ghi kèm (cascade)
        receiptCount++;
        if (member == null) return;

        long txnId = txn.getId();
        if (redeemPts > 0) {
            PointRedemption red = PointRedemption.confirmedHistory(member.id, shift.terminalId, txnId, redeemPts, pointsAmount,
                    otpAt.minusSeconds(40), otpAt);
            dayRedemptions.add(red);
            dayPointPayments.add(new Object[]{txn, pointsAmount, red});
            member.balance -= redeemPts;
            ledger(member, PointLedger.EntryType.HOLD, -redeemPts, null, null, red, shift.cashierId, otpAt);
            member.balance += redeemPts;
            ledger(member, PointLedger.EntryType.RELEASE, redeemPts, null, null, red, shift.cashierId, time);
            int left = redeemPts;
            while (left > 0 && !member.lots.isEmpty()) {
                Lot lot = member.lots.peekFirst();
                int take = Math.min(left, lot.remaining);
                lot.remaining -= take;
                changedLots.put(lot.id, lot);
                left -= take;
                member.balance -= take;
                ledger(member, PointLedger.EntryType.REDEEM, -take, em.getReference(PointLot.class, lot.id), txnId, red, shift.cashierId, time);
                if (lot.remaining == 0) member.lots.pollFirst();
            }
            if ("UNVERIFIED".equals(member.status)) {
                member.status = "VERIFIED";
                member.verifiedAt = otpAt;
            }
        }
        if (earned > 0) {
            PointLot lot = new PointLot(member.id, txnId, time, PointCalculator.expiresAt(time, policy.expiryMonths()), earned);
            em.persist(lot); // chỉ cấp ID; dòng được ghi ở lượt flush (3) cùng sổ cái
            dayLoyalty.add(lot);
            member.lots.addLast(new Lot(lot.getId(), earned));
            member.balance += earned;
            ledger(member, PointLedger.EntryType.EARN, earned, lot, txnId, null, shift.cashierId, time);
        }
        member.spend += total;
        PointCalculator.Tier target = PointCalculator.tierFor(member.spend, tiers);
        if (target.thresholdSpend() > tier.thresholdSpend()) {
            dayLoyalty.add(new TierHistory(member.id, tier.code(), target.code(), TierHistory.Reason.UPGRADE, time));
            member.tier = target.code();
        }
    }

    private void ledger(Cust member, PointLedger.EntryType type, int points, PointLot lot, Long txnId, PointRedemption red,
                        long cashierId, OffsetDateTime at) {
        dayLoyalty.add(new PointLedger(member.id, type, points, member.balance, lot, txnId, red, policyEntity, cashierId, at));
        ledgerCount++;
    }

    // ------------------------------------------------------------------ danh mục và giá
    private List<ProductDef> seedCatalog() {
        List<ProductDef> out = new ArrayList<>();
        for (DemoCatalog.Category c : DemoCatalog.CATEGORIES) {
            Category cat = new Category(c.code(), c.name(), c.restricted());
            em.persist(cat);
            int n = 0;
            for (DemoCatalog.Item it : c.items()) {
                n++;
                boolean excluded = DemoCatalog.pointExcluded(c);
                Product p = new Product(c.code() + String.format("%03d", n), it.name(), it.unit(), cat, BigDecimal.valueOf(c.vat()),
                        c.restricted(), excluded);
                em.persist(p); // IDENTITY: ID có ngay
                long id = p.getId();
                List<String> barcodes = new ArrayList<>(List.of(ean13("893" + String.format("%04d", 1000 + cat.getId()) + String.format("%05d", id))));
                if (n % 5 == 0) // một số mặt hàng có thêm mã vạch thứ hai (mẫu bao bì mới)
                    barcodes.add(ean13("893" + String.format("%04d", 2000 + cat.getId()) + String.format("%05d", id)));
                p.replaceBarcodes(barcodes);
                out.add(new ProductDef(id, c, it, !c.restricted() && !excluded));
            }
        }
        return out;
    }

    /** Giá toàn chuỗi từ 01/11/2025; 12 mặt hàng tăng giá 5% từ 01/06/2026; HM003 có giá riêng cho 6 mặt hàng. */
    private Map<Long, long[]> seedPrices(List<ProductDef> products) {
        OffsetDateTime base = LocalDate.of(2025, 11, 1).atStartOfDay(VN).toOffsetDateTime();
        OffsetDateTime change = LocalDate.of(2026, 6, 1).atStartOfDay(VN).toOffsetDateTime();
        Map<Long, long[]> changes = new HashMap<>();
        for (ProductDef p : products) {
            Product product = em.getReference(Product.class, p.id());
            boolean changed = p.id() % 12 == 3;
            ProductPrice basePrice = new ProductPrice(product, null, p.item().price(), base);
            basePrice.approve(1L);
            if (changed) basePrice.closeAt(change);
            em.persist(basePrice);
            if (changed) {
                long newPrice = Math.round(p.item().price() * 1.05 / 500) * 500;
                ProductPrice np = new ProductPrice(product, null, newPrice, change);
                np.approve(1L);
                em.persist(np);
                changes.put(p.id(), new long[]{change.toEpochSecond(), newPrice});
            }
            if (p.id() % 25 == 7) { // giá riêng HM003, thấp hơn 3%
                ProductPrice sp = new ProductPrice(product, 3L, Math.round(p.item().price() * 0.97 / 500) * 500, base);
                sp.approve(1L);
                em.persist(sp);
            }
        }
        return changes;
    }

    private long priceAt(ProductDef p, long storeId, OffsetDateTime time, Map<Long, long[]> changes) {
        if (storeId == 3 && p.id() % 25 == 7) return Math.round(p.item().price() * 0.97 / 500) * 500;
        long[] ch = changes.get(p.id());
        if (ch != null && time.toEpochSecond() >= ch[0]) return ch[1];
        return p.item().price();
    }

    // ------------------------------------------------------------------ khách hàng
    private List<Cust> buildCustomers() {
        List<Cust> list = new ArrayList<>();
        Set<String> phones = new HashSet<>(Set.of("0987654321")); // dành cho kịch bản đăng ký mới tại quầy
        // Khách cố định cho kịch bản demo
        list.add(fixed("Nguyễn Thị Lan", "0901234567", "FEMALE", LocalDate.of(1988, 3, 12), "lan.nguyen@example.com", 1, 14, false, true));
        list.add(fixed("Trần Văn Hùng", "0912345678", "MALE", LocalDate.of(1979, 8, 2), null, 1, 6, true, false));
        list.add(fixed("Phạm Minh Tuấn", "0935123456", "MALE", LocalDate.of(1992, 11, 25), null, 2, 8, false, false));
        list.forEach(c -> phones.add(c.phone));
        double[] storeShare = {0.40, 0.27, 0.33};
        while (list.size() < 800) {
            Cust c = new Cust();
            boolean female = rnd.nextDouble() < 0.62;
            c.gender = female ? "FEMALE" : "MALE";
            c.name = DemoCatalog.HO[rnd.nextInt(DemoCatalog.HO.length)] + " "
                    + (female ? DemoCatalog.DEM_NU[rnd.nextInt(DemoCatalog.DEM_NU.length)] : DemoCatalog.DEM_NAM[rnd.nextInt(DemoCatalog.DEM_NAM.length)]) + " "
                    + (female ? DemoCatalog.TEN_NU[rnd.nextInt(DemoCatalog.TEN_NU.length)] : DemoCatalog.TEN_NAM[rnd.nextInt(DemoCatalog.TEN_NAM.length)]);
            String phone;
            do {
                phone = DemoCatalog.PHONE_PREFIX[rnd.nextInt(DemoCatalog.PHONE_PREFIX.length)] + String.format("%07d", rnd.nextInt(10_000_000));
            } while (!phones.add(phone) || phone.startsWith("0869")); // 0869xxxxxx dành cho file chuyển đổi mẫu
            c.phone = phone;
            c.dob = rnd.nextDouble() < 0.7 ? LocalDate.of(1960 + rnd.nextInt(46), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)) : null;
            c.email = rnd.nextDouble() < 0.3 ? Texts.unaccent(c.name).replace(" ", ".") + (10 + rnd.nextInt(89)) + "@example.com" : null;
            double u = rnd.nextDouble();
            c.storeId = u < storeShare[0] ? 1 : u < storeShare[0] + storeShare[1] ? 2 : 3;
            c.weight = Math.min(35, 1 / Math.sqrt(Math.max(1e-4, rnd.nextDouble())));
            // 65% đã là thành viên trước khi hệ thống mới chạy; 35% đăng ký mới trong năm
            if (rnd.nextDouble() < 0.65)
                c.createdAt = HISTORY_START.minusDays(1 + rnd.nextInt(10)).plusMinutes(rnd.nextInt(600)).toOffsetDateTime();
            else
                c.createdAt = HISTORY_START.plusDays(rnd.nextInt(340)).plusHours(8).plusMinutes(rnd.nextInt(780)).toOffsetDateTime();
            c.marketing = rnd.nextDouble() < 0.6;
            list.add(c);
        }
        return list;
    }

    private Cust fixed(String name, String phone, String gender, LocalDate dob, String email, long store, double weight,
                       boolean redeemLocked, boolean neverRedeem) {
        Cust c = new Cust();
        c.name = name;
        c.phone = phone;
        c.gender = gender;
        c.dob = dob;
        c.email = email;
        c.storeId = store;
        c.weight = weight;
        c.redeemLocked = redeemLocked;
        c.neverRedeem = neverRedeem;
        c.marketing = true;
        c.createdAt = HISTORY_START.minusDays(5).plusHours(10).toOffsetDateTime();
        if (redeemLocked || !neverRedeem) {
            c.status = "VERIFIED";
            c.verifiedAt = c.createdAt.plusDays(1);
        }
        return c;
    }

    /** Ghi hồ sơ (ban đầu Chưa xác thực, hạng Thành viên) và consent; mã thành viên lấy từ sequence như đăng ký thật. */
    private void persistCustomers(List<Cust> customers) {
        for (Cust c : customers) {
            Customer e = new Customer("HM" + customerRepo.nextMemberNumber(), c.phone, c.name, c.gender, c.dob, c.email, c.storeId,
                    Customer.Status.UNVERIFIED, c.createdAt);
            em.persist(e);
            c.id = e.getId();
            boolean atPos = c.createdAt.isAfter(HISTORY_START.toOffsetDateTime());
            Long by = atPos ? 4 + terminalId(c.storeId, 1 + (int) (c.id % 2)) : null;
            em.persist(new Consent(e, Consent.Purpose.MEMBERSHIP, Consent.Status.GRANTED, "POS", by, c.createdAt));
            if (c.marketing) em.persist(new Consent(e, Consent.Purpose.MARKETING_SMS, Consent.Status.GRANTED, "POS", by, c.createdAt));
        }
    }

    // ------------------------------------------------------------------ tiện ích
    /** Nạp theo lô 1.000 bản ghi, sửa rồi flush (Hibernate gom UPDATE theo batch). */
    private <T> void updateInChunks(Class<T> type, Collection<Long> ids, java.util.function.Consumer<T> change) {
        List<Long> all = new ArrayList<>(ids);
        String entity = type.getSimpleName();
        for (int i = 0; i < all.size(); i += 1000) {
            em.createQuery("select e from " + entity + " e where e.id in :ids", type)
                    .setParameter("ids", all.subList(i, Math.min(all.size(), i + 1000))).getResultList().forEach(change);
            em.flush();
            em.clear();
        }
    }

    private static long terminalId(long storeId, int q) {
        return (storeId - 1) * 2 + q;
    }

    private static double[] cumulative(List<Cust> list) {
        double[] cum = new double[list.size()];
        double acc = 0;
        for (int i = 0; i < list.size(); i++) cum[i] = acc += list.get(i).weight;
        return cum;
    }

    private int pick(double[] cum) {
        double x = rnd.nextDouble() * cum[cum.length - 1];
        int i = Arrays.binarySearch(cum, x);
        return i >= 0 ? i : Math.min(cum.length - 1, -i - 1);
    }

    private static String ean13(String twelve) {
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (twelve.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return twelve + (10 - sum % 10) % 10;
    }
}
