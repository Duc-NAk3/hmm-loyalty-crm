package vn.sofitech.hmm.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.PointCalculator.Policy;
import vn.sofitech.hmm.loyalty.PointCalculator.Tier;
import vn.sofitech.hmm.shared.Texts;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static vn.sofitech.hmm.shared.Texts.VN;

/**
 * Tạo dữ liệu mẫu khi CSDL còn trống: 3 cửa hàng, 6 quầy, người dùng các vai trò, ~150 mặt hàng, ~800 khách và
 * lịch sử bán hàng từ 01/12/2025 đến hôm qua. Điểm, lô điểm, sổ cái và hạng được tính bằng chính {@link PointCalculator}
 * và cùng quy ước sổ cái với hệ thống thật (EARN; HOLD → RELEASE → REDEEM theo FIFO), nên số dư luôn bằng tổng sổ cái.
 * Random có seed cố định: lần nào chạy cũng ra cùng dữ liệu.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "Demo@123";
    static final ZonedDateTime HISTORY_START = LocalDate.of(2025, 12, 1).atStartOfDay(VN);
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final Random rnd = new Random(20261111L);

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer stores = jdbc.queryForObject("SELECT count(*) FROM admin.stores", Integer.class);
        if (stores != null && stores > 0) return;
        long t0 = System.currentTimeMillis();
        log.info("Empty database – seeding demo data...");
        tx.executeWithoutResult(s -> seed());
        log.info("Demo data seeded in {} ms", System.currentTimeMillis() - t0);
    }

    // ------------------------------------------------------------------ model trong bộ nhớ
    private record Store(long id, String code, String name, String region, String address, LocalDate opened, double baseDaily) {
    }

    private record Product(long id, String sku, DemoCatalog.Category cat, DemoCatalog.Item item, boolean eligible) {
    }

    private static final class Lot {
        final long id;
        int remaining;

        Lot(long id, int remaining) {
            this.id = id;
            this.remaining = remaining;
        }
    }

    private static final class Cust {
        long id;
        String code, phone, name, gender, email, status = "UNVERIFIED", tier = "MEMBER";
        LocalDate dob;
        OffsetDateTime createdAt, verifiedAt;
        long storeId, spend;
        int balance;
        double weight;
        boolean redeemLocked, neverRedeem, marketing;
        final ArrayDeque<Lot> lots = new ArrayDeque<>();
    }

    private static final class Shift {
        long id, terminalId, cashierId, managerId;
        OffsetDateTime opened, closed;
        long cash;
        int seq;
    }

    private final List<Object[]> txns = new ArrayList<>(), lines = new ArrayList<>(), payments = new ArrayList<>(),
            redemptions = new ArrayList<>(), lotsRows = new ArrayList<>(), ledger = new ArrayList<>(), tierHist = new ArrayList<>();
    private long txnId, lineId, paymentId, redemptionId, lotId, ledgerId, tierHistId;

    // ------------------------------------------------------------------ seed
    private void seed() {
        Policy policy = new Policy(1, 10_000, 100, 50, 50, 12, 15);
        List<Tier> tiers = List.of(
                new Tier("MEMBER", "Thành viên", 0, new BigDecimal("1.00")),
                new Tier("SILVER", "Bạc", 3_000_000, new BigDecimal("1.20")),
                new Tier("GOLD", "Vàng", 10_000_000, new BigDecimal("1.50")),
                new Tier("DIAMOND", "Kim cương", 30_000_000, new BigDecimal("2.00")));

        // --- quản trị
        List<Store> stores = List.of(
                new Store(1, "HM001", "Hoa Mai Mart Cầu Giấy", "Miền Bắc", "125 Trần Duy Hưng, Cầu Giấy, Hà Nội", LocalDate.of(2019, 5, 20), 24),
                new Store(2, "HM002", "Hoa Mai Mart Hải Châu", "Miền Trung", "48 Nguyễn Văn Linh, Hải Châu, Đà Nẵng", LocalDate.of(2020, 9, 10), 17),
                new Store(3, "HM003", "Hoa Mai Mart Quận 7", "Miền Nam", "302 Nguyễn Thị Thập, Quận 7, TP. Hồ Chí Minh", LocalDate.of(2021, 3, 15), 21));
        for (Store s : stores)
            jdbc.update("INSERT INTO admin.stores (store_id, store_code, name, region, address, status, opened_at) VALUES (?,?,?,?,?, 'ACTIVE', ?)",
                    s.id(), s.code(), s.name(), s.region(), s.address(), s.opened());
        OffsetDateTime terminalReg = OffsetDateTime.of(2025, 11, 20, 9, 0, 0, 0, ZoneOffset.ofHours(7));
        for (Store s : stores)
            for (int q = 1; q <= 2; q++)
                jdbc.update("INSERT INTO admin.pos_terminals (terminal_id, store_id, terminal_code, status, registered_at) VALUES (?,?,?, 'ACTIVE', ?)",
                        terminalId(s.id(), q), s.id(), s.code() + "-Q0" + q, terminalReg);

        String[][] roles = {{"ADMIN", "Quản trị hệ thống"}, {"STORE_MANAGER", "Quản lý cửa hàng"}, {"CASHIER", "Thu ngân"},
                {"CSKH", "Chăm sóc khách hàng"}, {"MARKETING", "Marketing"}, {"CATALOG", "Danh mục và giá"}};
        for (int i = 0; i < roles.length; i++)
            jdbc.update("INSERT INTO admin.roles (role_id, code, name) VALUES (?,?,?)", i + 1, roles[i][0], roles[i][1]);

        String hash = encoder.encode(DEMO_PASSWORD);
        Object[][] users = {
                {1, "admin", "Quản trị hệ thống", 1, null},
                {2, "ql.hm001", "Lê Thị Mai", 2, 1}, {3, "ql.hm002", "Phan Văn Đức", 2, 2}, {4, "ql.hm003", "Võ Thị Ngọc", 2, 3},
                {5, "tn.hm001.1", "Nguyễn Thu Hà", 3, 1}, {6, "tn.hm001.2", "Đỗ Minh Khang", 3, 1},
                {7, "tn.hm002.1", "Trần Thị Hồng", 3, 2}, {8, "tn.hm002.2", "Lý Quốc Bảo", 3, 2},
                {9, "tn.hm003.1", "Huỳnh Ngọc Trâm", 3, 3}, {10, "tn.hm003.2", "Bùi Thanh Tùng", 3, 3},
                {11, "cskh", "Đặng Phương Linh", 4, null}, {12, "marketing", "Hoàng Gia Huy", 5, null}, {13, "danhmuc", "Ngô Thị Yến", 6, null}};
        for (Object[] u : users)
            jdbc.update("INSERT INTO admin.app_users (user_id, username, password_hash, full_name, role_id, store_id, mfa_enabled, status) VALUES (?,?,?,?,?,?, false, 'ACTIVE')",
                    u[0], u[1], hash, u[2], u[3], u[4]);

        jdbc.update("INSERT INTO admin.system_parameters (param_key, param_value, description, updated_by, updated_at) VALUES (?,?,?,1, now()), (?,?,?,1, now()), (?,?,?,1, now())",
                "OTP_LENGTH", "6", "Số chữ số của mã OTP",
                "OTP_TTL_MINUTES", "5", "Thời hạn mã OTP (phút)",
                "OTP_MAX_ATTEMPTS", "3", "Số lần nhập sai OTP tối đa");

        // --- loyalty
        String[] benefits = {"Tích điểm, đổi điểm trừ tiền hóa đơn",
                "Quyền lợi hạng Thành viên + voucher sinh nhật 50.000đ",
                "Quyền lợi hạng Bạc + ưu đãi riêng hằng tháng, ưu tiên CSKH",
                "Quyền lợi hạng Vàng + quà tri ân, ưu đãi độc quyền, sự kiện riêng"};
        for (int i = 0; i < tiers.size(); i++) {
            Tier t = tiers.get(i);
            jdbc.update("INSERT INTO loyalty.tiers (tier_code, name, threshold_spend, multiplier, benefits) VALUES (?,?,?,?,?)",
                    t.code(), t.name(), t.thresholdSpend(), t.multiplier(), benefits[i]);
        }
        jdbc.update("""
                INSERT INTO loyalty.loyalty_policies (policy_id, version, earn_unit_vnd, point_value_vnd, min_redeem_points, max_redeem_pct,
                    expiry_months, hold_minutes, effective_from, status, approved_by) VALUES (1, 1, 10000, 100, 50, 50, 12, 15, ?, 'APPROVED', 1)
                """, OffsetDateTime.of(2025, 11, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7)));

        // --- danh mục và giá
        List<Product> products = seedCatalog();
        Map<Long, long[]> priceChanges = seedPrices(products, stores); // productId → [ngày đổi (epoch day), giá mới]

        // --- khách hàng (chưa ghi CSDL; ghi sau khi mô phỏng xong để có trạng thái cuối)
        List<Cust> customers = buildCustomers(stores);

        // --- mô phỏng bán hàng theo ngày
        LocalDate today = LocalDate.now(VN);
        Map<Long, double[]> cumByStore = new HashMap<>();
        for (Store s : stores) cumByStore.put(s.id(), cumulative(customers.stream().filter(c -> c.storeId == s.id()).toList()));
        double[] cumAll = cumulative(customers);
        Map<Long, List<Cust>> byStore = new HashMap<>();
        for (Store s : stores) byStore.put(s.id(), customers.stream().filter(c -> c.storeId == s.id()).toList());
        double[] catCum = new double[products.size()];
        double acc = 0;
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            acc += (double) p.cat().weight() / p.cat().items().size();
            catCum[i] = acc;
        }
        List<Shift> shifts = new ArrayList<>();
        long shiftId = 0;
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
            Map<Long, Shift> dayShifts = new HashMap<>();
            for (Store s : stores)
                for (int q = 1; q <= 2; q++) {
                    Shift sh = new Shift();
                    sh.id = ++shiftId;
                    sh.terminalId = terminalId(s.id(), q);
                    sh.cashierId = 4 + sh.terminalId;
                    sh.managerId = 1 + s.id();
                    sh.opened = day.atTime(6, 45).atZone(VN).toOffsetDateTime();
                    sh.closed = day.atTime(22, 15).atZone(VN).toOffsetDateTime();
                    dayShifts.put(sh.terminalId, sh);
                    shifts.add(sh);
                }
            record Receipt(OffsetDateTime time, Store store, Shift shift) {
            }
            List<Receipt> receipts = new ArrayList<>();
            for (Store s : stores) {
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
        }

        // --- ghi CSDL theo thứ tự khóa ngoại
        insertCustomers(customers);
        batch("INSERT INTO pos.shifts (shift_id, terminal_id, cashier_id, opened_at, opening_cash, closed_at, expected_cash, counted_cash, cash_difference, confirmed_by) VALUES (?,?,?,?,?,?,?,?,?,?)",
                shifts.stream().map(sh -> {
                    long expected = 2_000_000 + sh.cash;
                    long diff = rnd.nextDouble() < 0.04 ? (rnd.nextBoolean() ? 1 : -1) * 1_000L * (1 + rnd.nextInt(20)) : 0;
                    return new Object[]{sh.id, sh.terminalId, sh.cashierId, sh.opened, 2_000_000L, sh.closed, expected, expected + diff, diff, sh.managerId};
                }).toList());
        batch("INSERT INTO pos.sales_transactions (transaction_id, receipt_no, store_id, terminal_id, shift_id, cashier_id, customer_id, txn_type, status, source, txn_time, synced_at, net_amount, discount_amount, eligible_amount, points_earned, points_redeemed) VALUES (?,?,?,?,?,?,?, 'SALE', 'COMPLETED', 'POS', ?, ?, ?, 0, ?, ?, ?)", txns);
        batch("INSERT INTO pos.transaction_lines (line_id, transaction_id, product_id, quantity, unit_price, discount, line_amount, point_eligible) VALUES (?,?,?,?,?,0,?,?)", lines);
        batch("INSERT INTO loyalty.point_redemptions (redemption_id, customer_id, terminal_id, transaction_id, points, amount_vnd, status, otp_verified_at, hold_expires_at, created_at) VALUES (?,?,?,?,?,?, 'CONFIRMED', ?, ?, ?)", redemptions);
        batch("INSERT INTO pos.payments (payment_id, transaction_id, method, amount, redemption_id, reference) VALUES (?,?,?,?,?,?)", payments);
        // lô điểm: số điểm còn lại là trạng thái cuối sau FIFO
        Map<Long, Integer> remaining = new HashMap<>();
        customers.forEach(c -> c.lots.forEach(l -> remaining.put(l.id, l.remaining)));
        lotsRows.forEach(row -> row[6] = remaining.getOrDefault((Long) row[0], 0));
        batch("INSERT INTO loyalty.point_lots (lot_id, customer_id, transaction_id, earned_at, expires_at, points_earned, points_remaining) VALUES (?,?,?,?,?,?,?)", lotsRows);
        batch("INSERT INTO loyalty.point_ledgers (ledger_id, customer_id, entry_type, points, balance_after, lot_id, transaction_id, redemption_id, policy_id, created_by, created_at) VALUES (?,?,?,?,?,?,?,?,1,?,?)", ledger);
        batch("INSERT INTO loyalty.tier_histories (history_id, customer_id, from_tier, to_tier, reason, changed_at) VALUES (?,?,?,?,?,?)", tierHist);

        // giá chờ duyệt để demo FR-12
        jdbc.update("INSERT INTO catalog.product_prices (product_id, store_id, price, effective_from, status) VALUES (?, NULL, ?, ?, 'DRAFT')",
                products.getFirst().id(), Math.round(products.getFirst().item().price() * 1.06 / 100) * 100,
                today.plusDays(1).atStartOfDay(VN).toOffsetDateTime());

        resetSequences();
        log.info("Seeded {} stores, {} products, {} customers, {} receipts, {} ledger entries",
                stores.size(), products.size(), customers.size(), txns.size(), ledger.size());
    }

    // ------------------------------------------------------------------ bán một hóa đơn
    private void sell(OffsetDateTime time, Store store, Shift shift, Cust member, List<Product> products, double[] catCum,
                      Map<Long, long[]> priceChanges, Policy policy, List<Tier> tiers) {
        long id = ++txnId;
        String receiptNo = store.code() + "-Q0" + (shift.terminalId - (store.id() - 1) * 2) + "-" + time.format(YYMMDD) + "-" + String.format("%04d", ++shift.seq);
        int k = Math.min(12, 1 + (int) (-Math.log(1 - rnd.nextDouble()) * 3.2));
        Set<Integer> chosen = new LinkedHashSet<>();
        while (chosen.size() < k) chosen.add(pick(catCum));
        long total = 0, redeemable = 0;
        for (int idx : chosen) {
            Product p = products.get(idx);
            int qty = rnd.nextDouble() < 0.75 ? 1 : 2 + rnd.nextInt(2);
            long unit = priceAt(p, store.id(), time, priceChanges);
            long amount = unit * qty;
            total += amount;
            if (p.eligible()) redeemable += amount;
            lines.add(new Object[]{++lineId, id, p.id(), BigDecimal.valueOf(qty), unit, amount, p.eligible()});
        }
        Integer earned = null, redeemedPts = null;
        Long eligible = null;
        long pointsAmount = 0;
        if (member != null) {
            Tier tier = tiers.stream().filter(t -> t.code().equals(member.tier)).findFirst().orElseThrow();
            // Đổi điểm (khoảng 15% hóa đơn có đủ điểm)
            redeemedPts = 0;
            if (!member.neverRedeem && !member.redeemLocked && member.balance >= 50 && rnd.nextDouble() < 0.15) {
                PointCalculator.RedeemQuote q = PointCalculator.quote(redeemable, member.balance, policy);
                if (q.allowed()) {
                    int pts = Math.max(policy.minRedeemPoints(), (int) (q.maxPoints() * (0.5 + 0.5 * rnd.nextDouble())) / 10 * 10);
                    pts = Math.min(pts, q.maxPoints());
                    long rid = ++redemptionId;
                    OffsetDateTime otpAt = time.minusMinutes(1);
                    redemptions.add(new Object[]{rid, member.id, shift.terminalId, id, pts, (long) pts * policy.pointValueVnd(), otpAt, otpAt.plusMinutes(15), otpAt.minusSeconds(40)});
                    member.balance -= pts;
                    ledger.add(new Object[]{++ledgerId, member.id, "HOLD", -pts, member.balance, null, null, rid, shift.cashierId, otpAt});
                    member.balance += pts;
                    ledger.add(new Object[]{++ledgerId, member.id, "RELEASE", pts, member.balance, null, null, rid, shift.cashierId, time});
                    int left = pts;
                    while (left > 0 && !member.lots.isEmpty()) {
                        Lot lot = member.lots.peekFirst();
                        int take = Math.min(left, lot.remaining);
                        lot.remaining -= take;
                        left -= take;
                        member.balance -= take;
                        ledger.add(new Object[]{++ledgerId, member.id, "REDEEM", -take, member.balance, lot.id, id, rid, shift.cashierId, time});
                        if (lot.remaining == 0) member.lots.pollFirst();
                    }
                    if ("UNVERIFIED".equals(member.status)) {
                        member.status = "VERIFIED";
                        member.verifiedAt = otpAt;
                    }
                    pointsAmount = (long) pts * policy.pointValueVnd();
                    redeemedPts = pts;
                    payments.add(new Object[]{++paymentId, id, "POINTS", pointsAmount, rid, null});
                }
            }
            eligible = Math.max(0, redeemable - pointsAmount);
            earned = PointCalculator.earnedPoints(eligible, policy, tier.multiplier(), BigDecimal.ONE);
            if (earned > 0) {
                long lid = ++lotId;
                lotsRows.add(new Object[]{lid, member.id, id, time, PointCalculator.expiresAt(time, policy.expiryMonths()), earned, earned});
                member.lots.addLast(new Lot(lid, earned));
                member.balance += earned;
                ledger.add(new Object[]{++ledgerId, member.id, "EARN", earned, member.balance, lid, id, null, shift.cashierId, time});
            }
            member.spend += total;
            Tier target = PointCalculator.tierFor(member.spend, tiers);
            if (target.thresholdSpend() > tier.thresholdSpend()) {
                tierHist.add(new Object[]{++tierHistId, member.id, tier.code(), target.code(), "UPGRADE", time});
                member.tier = target.code();
            }
        }
        long cash = total - pointsAmount;
        if (cash > 0) {
            long tendered = cash % 10_000 == 0 ? cash : (cash / 10_000 + 1) * 10_000;
            payments.add(new Object[]{++paymentId, id, "CASH", cash, null, "tendered=" + tendered});
        }
        shift.cash += cash;
        txns.add(new Object[]{id, receiptNo, store.id(), shift.terminalId, shift.id, shift.cashierId, member == null ? null : member.id,
                time, time, total, eligible, earned, redeemedPts});
    }

    // ------------------------------------------------------------------ danh mục và giá
    private List<Product> seedCatalog() {
        List<Product> out = new ArrayList<>();
        long catId = 0, prodId = 0;
        for (DemoCatalog.Category c : DemoCatalog.CATEGORIES) {
            catId++;
            jdbc.update("INSERT INTO catalog.categories (category_id, code, name, promo_restricted) VALUES (?,?,?,?)", catId, c.code(), c.name(), c.restricted());
            int n = 0;
            for (DemoCatalog.Item it : c.items()) {
                prodId++;
                n++;
                String sku = c.code() + String.format("%03d", n);
                boolean excluded = DemoCatalog.pointExcluded(c);
                jdbc.update("INSERT INTO catalog.products (product_id, sku, name, unit, category_id, vat_rate, promo_restricted, point_excluded, status) VALUES (?,?,?,?,?,?,?,?, 'ACTIVE')",
                        prodId, sku, it.name(), it.unit(), catId, BigDecimal.valueOf(c.vat()), c.restricted(), excluded);
                jdbc.update("INSERT INTO catalog.product_barcodes (barcode, product_id) VALUES (?,?)", ean13("893" + String.format("%04d", 1000 + catId) + String.format("%05d", prodId)), prodId);
                if (n % 5 == 0) // một số mặt hàng có thêm mã vạch thứ hai (mẫu bao bì mới)
                    jdbc.update("INSERT INTO catalog.product_barcodes (barcode, product_id) VALUES (?,?)", ean13("893" + String.format("%04d", 2000 + catId) + String.format("%05d", prodId)), prodId);
                out.add(new Product(prodId, sku, c, it, !c.restricted() && !excluded));
            }
        }
        return out;
    }

    /** Giá toàn chuỗi từ 01/11/2025; 12 mặt hàng tăng giá 5% từ 01/06/2026; HM003 có giá riêng cho 6 mặt hàng. */
    private Map<Long, long[]> seedPrices(List<Product> products, List<Store> stores) {
        OffsetDateTime base = LocalDate.of(2025, 11, 1).atStartOfDay(VN).toOffsetDateTime();
        OffsetDateTime change = LocalDate.of(2026, 6, 1).atStartOfDay(VN).toOffsetDateTime();
        Map<Long, long[]> changes = new HashMap<>();
        for (Product p : products) {
            boolean changed = p.id() % 12 == 3;
            jdbc.update("INSERT INTO catalog.product_prices (product_id, store_id, price, effective_from, effective_to, status, approved_by) VALUES (?, NULL, ?, ?, ?, 'APPROVED', 1)",
                    p.id(), p.item().price(), base, changed ? change : null);
            if (changed) {
                long newPrice = Math.round(p.item().price() * 1.05 / 500) * 500;
                jdbc.update("INSERT INTO catalog.product_prices (product_id, store_id, price, effective_from, status, approved_by) VALUES (?, NULL, ?, ?, 'APPROVED', 1)",
                        p.id(), newPrice, change);
                changes.put(p.id(), new long[]{change.toEpochSecond(), newPrice});
            }
            if (p.id() % 25 == 7) // giá riêng HM003, thấp hơn 3%
                jdbc.update("INSERT INTO catalog.product_prices (product_id, store_id, price, effective_from, status, approved_by) VALUES (?, 3, ?, ?, 'APPROVED', 1)",
                        p.id(), Math.round(p.item().price() * 0.97 / 500) * 500, base);
        }
        return changes;
    }

    private long priceAt(Product p, long storeId, OffsetDateTime time, Map<Long, long[]> changes) {
        if (storeId == 3 && p.id() % 25 == 7) return Math.round(p.item().price() * 0.97 / 500) * 500;
        long[] ch = changes.get(p.id());
        if (ch != null && time.toEpochSecond() >= ch[0]) return ch[1];
        return p.item().price();
    }

    // ------------------------------------------------------------------ khách hàng
    private List<Cust> buildCustomers(List<Store> stores) {
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
        for (int i = 0; i < list.size(); i++) {
            Cust c = list.get(i);
            c.id = i + 1;
            c.code = "HM" + (10_000_001 + i);
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

    private void insertCustomers(List<Cust> customers) {
        batch("""
                INSERT INTO customer.customers (customer_id, member_code, phone, full_name, name_search, gender, date_of_birth, email, status, verified_at,
                    tier_code, point_balance, spend_12m, is_employee, redeem_locked, registered_store_id, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?, false, ?,?,?, now())
                """, customers.stream().map(c -> new Object[]{c.id, c.code, c.phone, c.name, Texts.unaccent(c.name), c.gender, c.dob, c.email,
                c.status, c.verifiedAt, c.tier, c.balance, c.spend, c.redeemLocked, c.storeId, c.createdAt}).toList());
        List<Object[]> consents = new ArrayList<>();
        for (Cust c : customers) {
            boolean atPos = c.createdAt.isAfter(HISTORY_START.toOffsetDateTime());
            Long by = atPos ? 4 + terminalId(c.storeId, 1 + (int) (c.id % 2)) : null;
            consents.add(new Object[]{c.id, "MEMBERSHIP", "GRANTED", "POS", by, c.createdAt});
            if (c.marketing) consents.add(new Object[]{c.id, "MARKETING_SMS", "GRANTED", "POS", by, c.createdAt});
        }
        batch("INSERT INTO customer.consents (customer_id, purpose, status, channel, terms_version, captured_by, captured_at) VALUES (?,?,?,?, 'v1.0', ?, ?)", consents);
    }

    // ------------------------------------------------------------------ tiện ích
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

    private void batch(String sql, List<Object[]> rows) {
        for (int i = 0; i < rows.size(); i += 2000)
            jdbc.batchUpdate(sql, rows.subList(i, Math.min(rows.size(), i + 2000)));
    }

    private void resetSequences() {
        String[][] seqs = {{"admin.stores", "store_id"}, {"admin.pos_terminals", "terminal_id"}, {"admin.roles", "role_id"},
                {"admin.app_users", "user_id"}, {"loyalty.loyalty_policies", "policy_id"}, {"customer.customers", "customer_id"},
                {"customer.consents", "consent_id"}, {"catalog.categories", "category_id"}, {"catalog.products", "product_id"},
                {"catalog.product_prices", "price_id"}, {"pos.shifts", "shift_id"}, {"pos.sales_transactions", "transaction_id"},
                {"pos.transaction_lines", "line_id"}, {"pos.payments", "payment_id"}, {"loyalty.point_redemptions", "redemption_id"},
                {"loyalty.point_lots", "lot_id"}, {"loyalty.point_ledgers", "ledger_id"}, {"loyalty.tier_histories", "history_id"}};
        for (String[] s : seqs)
            jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('" + s[0] + "', '" + s[1] + "'), COALESCE((SELECT max(" + s[1] + ") FROM " + s[0] + "), 0) + 1, false)", Long.class);
        jdbc.queryForObject("SELECT setval('customer.member_code_seq', (SELECT max(substring(member_code from 3)::bigint) FROM customer.customers) + 1, false)", Long.class);
    }
}
