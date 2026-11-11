package vn.sofitech.hmm.demo;

import java.util.List;

/** Danh mục hàng hóa mẫu: 12 ngành hàng, ~150 mặt hàng (tên chung, không dùng nhãn hiệu thật). */
final class DemoCatalog {

    record Item(String name, String unit, long price) {
    }

    /** weight: tần suất xuất hiện trong giỏ hàng; restricted: ngành hàng hạn chế khuyến mại (BRL-05). */
    record Category(String code, String name, boolean restricted, int weight, double vat, List<Item> items) {
    }

    private DemoCatalog() {
    }

    static final List<Category> CATEGORIES = List.of(
            new Category("RAU", "Rau củ quả", false, 18, 5, List.of(
                    new Item("Cải ngọt Đà Lạt 500g", "Gói", 18_000), new Item("Rau muống 500g", "Gói", 12_000),
                    new Item("Cà chua bi 500g", "Hộp", 32_000), new Item("Khoai tây Đà Lạt 1kg", "Kg", 28_000),
                    new Item("Cà rốt 1kg", "Kg", 22_000), new Item("Bắp cải trắng", "Kg", 16_000),
                    new Item("Hành tây 500g", "Gói", 15_000), new Item("Táo đỏ nhập khẩu", "Kg", 89_000),
                    new Item("Chuối tiêu", "Nải", 25_000), new Item("Cam sành", "Kg", 35_000),
                    new Item("Dưa hấu ruột đỏ", "Kg", 18_000), new Item("Xoài cát Hòa Lộc", "Kg", 65_000),
                    new Item("Nấm kim châm 150g", "Gói", 12_000))),
            new Category("THIT", "Thịt, cá, trứng", false, 16, 5, List.of(
                    new Item("Thịt ba chỉ heo 500g", "Khay", 75_000), new Item("Thịt nạc vai heo 500g", "Khay", 68_000),
                    new Item("Sườn non heo 500g", "Khay", 95_000), new Item("Thịt bò thăn 300g", "Khay", 125_000),
                    new Item("Ức gà phi lê 500g", "Khay", 52_000), new Item("Đùi gà góc tư 1kg", "Kg", 69_000),
                    new Item("Cá basa phi lê 500g", "Khay", 45_000), new Item("Tôm thẻ 500g", "Khay", 135_000),
                    new Item("Trứng gà ta hộp 10 quả", "Hộp", 38_000), new Item("Trứng vịt hộp 10 quả", "Hộp", 36_000),
                    new Item("Chả lụa 500g", "Cây", 85_000), new Item("Xúc xích heo 500g", "Gói", 59_000))),
            new Category("SUA", "Sữa và chế phẩm từ sữa", false, 12, 8, List.of(
                    new Item("Sữa tươi tiệt trùng có đường 1L", "Hộp", 34_000), new Item("Sữa tươi tiệt trùng không đường 1L", "Hộp", 34_000),
                    new Item("Lốc 4 hộp sữa tươi 180ml", "Lốc", 30_000), new Item("Sữa chua ăn có đường lốc 4", "Lốc", 26_000),
                    new Item("Sữa chua uống men sống lốc 5", "Lốc", 27_000), new Item("Sữa đặc có đường 380g", "Lon", 24_000),
                    new Item("Phô mai lát 200g", "Gói", 62_000), new Item("Bơ lạt 200g", "Hộp", 78_000),
                    new Item("Sữa đậu nành 1L", "Chai", 25_000), new Item("Sữa hạt óc chó 1L", "Hộp", 49_000))),
            new Category("SUATE", "Sữa và dinh dưỡng cho trẻ dưới 24 tháng", true, 3, 5, List.of(
                    new Item("Sữa bột trẻ em 0-6 tháng 800g", "Lon", 485_000), new Item("Sữa bột trẻ em 6-12 tháng 800g", "Lon", 465_000),
                    new Item("Sữa bột trẻ em 1-2 tuổi 900g", "Lon", 445_000), new Item("Bột ăn dặm vị rau củ 200g", "Hộp", 89_000),
                    new Item("Sữa công thức pha sẵn 180ml lốc 4", "Lốc", 72_000))),
            new Category("DOKHO", "Thực phẩm khô", false, 12, 5, List.of(
                    new Item("Gạo thơm Jasmine 5kg", "Túi", 125_000), new Item("Gạo nếp cái hoa vàng 2kg", "Túi", 68_000),
                    new Item("Mì gói tôm chua cay thùng 30", "Thùng", 115_000), new Item("Mì gói tôm chua cay", "Gói", 4_500),
                    new Item("Phở ăn liền bò", "Gói", 9_000), new Item("Bún khô 500g", "Gói", 18_000),
                    new Item("Miến dong 300g", "Gói", 32_000), new Item("Đậu xanh cà vỏ 500g", "Gói", 29_000),
                    new Item("Ngũ cốc dinh dưỡng 500g", "Hộp", 72_000), new Item("Yến mạch cán dẹt 500g", "Gói", 55_000),
                    new Item("Cá ngừ đóng hộp 150g", "Lon", 32_000), new Item("Thịt hộp heo 170g", "Lon", 35_000))),
            new Category("GIAVI", "Gia vị", false, 9, 8, List.of(
                    new Item("Nước mắm cá cơm 40 độ đạm 500ml", "Chai", 45_000), new Item("Nước tương đậu nành 500ml", "Chai", 22_000),
                    new Item("Dầu ăn đậu nành 1L", "Chai", 52_000), new Item("Dầu hướng dương 1L", "Chai", 69_000),
                    new Item("Đường tinh luyện 1kg", "Gói", 28_000), new Item("Muối i-ốt 500g", "Gói", 6_000),
                    new Item("Hạt nêm thịt heo 400g", "Gói", 38_000), new Item("Bột ngọt 400g", "Gói", 33_000),
                    new Item("Tương ớt 520g", "Chai", 21_000), new Item("Hạt tiêu đen xay 50g", "Hũ", 25_000),
                    new Item("Dầu hào 350g", "Chai", 29_000))),
            new Category("DOUONG", "Đồ uống", false, 14, 8, List.of(
                    new Item("Nước khoáng 500ml", "Chai", 5_000), new Item("Nước tinh khiết 1,5L", "Chai", 9_000),
                    new Item("Thùng nước tinh khiết 24 chai 500ml", "Thùng", 95_000), new Item("Nước ngọt có ga 330ml", "Lon", 10_000),
                    new Item("Lốc 6 lon nước ngọt có ga", "Lốc", 58_000), new Item("Trà xanh đóng chai 455ml", "Chai", 10_000),
                    new Item("Trà sữa đóng chai 350ml", "Chai", 15_000), new Item("Nước cam ép 1L", "Hộp", 42_000),
                    new Item("Cà phê hòa tan 3in1 hộp 20 gói", "Hộp", 58_000), new Item("Cà phê rang xay 500g", "Gói", 125_000),
                    new Item("Nước tăng lực 250ml", "Lon", 12_000), new Item("Bia lon 330ml", "Lon", 17_000),
                    new Item("Thùng bia 24 lon 330ml", "Thùng", 385_000))),
            new Category("RUOU", "Rượu từ 15 độ trở lên", true, 2, 10, List.of(
                    new Item("Rượu vang cường hóa 18 độ 750ml", "Chai", 245_000), new Item("Rượu mạnh 39 độ 500ml", "Chai", 165_000),
                    new Item("Rượu nếp 29 độ 500ml", "Chai", 95_000), new Item("Rượu whisky 40 độ 700ml", "Chai", 650_000))),
            new Category("BANH", "Bánh kẹo, đồ ăn vặt", false, 12, 8, List.of(
                    new Item("Bánh quy bơ hộp 300g", "Hộp", 65_000), new Item("Bánh xốp phô mai 200g", "Gói", 28_000),
                    new Item("Bánh mì sandwich 400g", "Gói", 24_000), new Item("Bánh bông lan cuộn 300g", "Hộp", 42_000),
                    new Item("Snack khoai tây vị tự nhiên 90g", "Gói", 19_000), new Item("Snack rong biển 50g", "Gói", 18_000),
                    new Item("Kẹo dẻo trái cây 100g", "Gói", 16_000), new Item("Socola sữa 100g", "Thanh", 35_000),
                    new Item("Hạt điều rang muối 250g", "Hũ", 98_000), new Item("Khô gà lá chanh 200g", "Hũ", 85_000),
                    new Item("Kẹo cao su hộp 12 thanh", "Hộp", 12_000))),
            new Category("HOMECARE", "Hóa phẩm gia dụng", false, 8, 10, List.of(
                    new Item("Nước giặt 3,8kg", "Can", 165_000), new Item("Nước xả vải 1,8L", "Túi", 98_000),
                    new Item("Nước rửa chén 750ml", "Chai", 32_000), new Item("Nước lau sàn 1L", "Chai", 38_000),
                    new Item("Giấy vệ sinh 10 cuộn", "Lốc", 72_000), new Item("Khăn giấy rút 180 tờ", "Gói", 18_000),
                    new Item("Túi đựng rác 3 cuộn", "Lốc", 29_000), new Item("Bột giặt 3kg", "Túi", 145_000),
                    new Item("Nước tẩy rửa nhà tắm 900ml", "Chai", 45_000))),
            new Category("CANHAN", "Chăm sóc cá nhân", false, 7, 10, List.of(
                    new Item("Dầu gội 650ml", "Chai", 155_000), new Item("Sữa tắm 900g", "Chai", 175_000),
                    new Item("Kem đánh răng 230g", "Tuýp", 38_000), new Item("Bàn chải đánh răng lông mềm", "Cái", 22_000),
                    new Item("Sữa rửa mặt 100g", "Tuýp", 89_000), new Item("Khăn ướt 80 tờ", "Gói", 35_000),
                    new Item("Tã dán trẻ em size M 60 miếng", "Gói", 289_000), new Item("Băng vệ sinh hằng ngày 40 miếng", "Gói", 39_000),
                    new Item("Lăn khử mùi 50ml", "Chai", 69_000))),
            new Category("THUOCLA", "Thuốc lá", true, 2, 10, List.of(
                    new Item("Thuốc lá điếu bao 20 điếu (loại A)", "Bao", 32_000), new Item("Thuốc lá điếu bao 20 điếu (loại B)", "Bao", 25_000),
                    new Item("Thuốc lá điếu cây 10 bao (loại A)", "Cây", 315_000))),
            new Category("DICHVU", "Dịch vụ và thẻ nạp", false, 2, 0, List.of(
                    new Item("Thẻ nạp điện thoại 50.000đ", "Thẻ", 50_000), new Item("Thẻ nạp điện thoại 100.000đ", "Thẻ", 100_000),
                    new Item("Thẻ quà tặng Hoa Mai Mart 200.000đ", "Thẻ", 200_000))));

    /** Mặt hàng không tích điểm theo quyết định của HMM (Q-06): thẻ nạp, thẻ quà tặng. */
    static boolean pointExcluded(Category c) {
        return c.code().equals("DICHVU");
    }

    static final String[] HO = {"Nguyễn", "Nguyễn", "Nguyễn", "Trần", "Trần", "Lê", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ",
            "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Trương", "Mai", "Cao"};
    static final String[] DEM_NAM = {"Văn", "Hữu", "Đức", "Minh", "Quốc", "Thanh", "Gia", "Hoàng", "Công", "Tuấn", "Xuân", "Đình"};
    static final String[] DEM_NU = {"Thị", "Thị", "Ngọc", "Thu", "Thanh", "Minh", "Hồng", "Phương", "Bảo", "Kim", "Mỹ", "Diệu"};
    static final String[] TEN_NAM = {"An", "Bình", "Cường", "Dũng", "Đạt", "Giang", "Hải", "Hiếu", "Hùng", "Huy", "Khang", "Khoa",
            "Long", "Minh", "Nam", "Nghĩa", "Phong", "Phúc", "Quân", "Quang", "Sơn", "Tài", "Thắng", "Thành", "Toàn", "Trung", "Tuấn", "Việt", "Vinh"};
    static final String[] TEN_NU = {"Anh", "Châu", "Chi", "Dung", "Giang", "Hà", "Hạnh", "Hằng", "Hoa", "Hương", "Lan", "Linh", "Loan",
            "Mai", "My", "Nga", "Ngân", "Nhung", "Oanh", "Phương", "Quỳnh", "Thảo", "Thu", "Trang", "Trâm", "Uyên", "Vân", "Vy", "Yến"};
    static final String[] PHONE_PREFIX = {"090", "091", "093", "094", "096", "097", "098", "086", "088", "089", "070", "076", "077",
            "078", "079", "081", "082", "083", "084", "085", "032", "033", "034", "035", "036", "037", "038", "039", "056", "058", "059"};
}
