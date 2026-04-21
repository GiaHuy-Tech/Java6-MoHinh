package com.example.demo.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.model.Account;
import com.example.demo.model.Address;
import com.example.demo.model.CartDetail;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.CartDetailRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final AddressRepository addressRepository;
    private final CartDetailRepository cartDetailRepository;

    // MAP tọa độ 63 tỉnh thành + Các từ khóa viết tắt phổ biến
    private static final Map<String, double[]> PROVINCE_MAP = new HashMap<>() {{
        // === MIỀN BẮC ===
        put("hà nội", new double[]{21.0285, 105.8542});
        put("hải phòng", new double[]{20.8449, 106.6881});
        put("hà giang", new double[]{22.8233, 104.9836});
        put("cao bằng", new double[]{22.6667, 106.2500});
        put("bắc kạn", new double[]{22.1470, 105.8348});
        put("tuyên quang", new double[]{21.8153, 105.2132});
        put("lào cai", new double[]{22.4856, 103.9707});
        put("điện biên", new double[]{21.3863, 103.0189});
        put("lai châu", new double[]{22.3954, 103.4518});
        put("sơn la", new double[]{21.3283, 103.8970});
        put("yên bái", new double[]{21.7229, 104.9113});
        put("hòa bình", new double[]{20.8133, 105.3384});
        put("thái nguyên", new double[]{21.5942, 105.8482});
        put("lạng sơn", new double[]{21.8484, 106.7561});
        put("quảng ninh", new double[]{21.0069, 107.2925});
        put("bắc giang", new double[]{21.2731, 106.1946});
        put("phú thọ", new double[]{21.3146, 105.2173});
        put("vĩnh phúc", new double[]{21.3032, 105.5786});
        put("bắc ninh", new double[]{21.1861, 106.0763});
        put("hải dương", new double[]{20.9373, 106.3146});
        put("hưng yên", new double[]{20.8524, 106.0527});
        put("thái bình", new double[]{20.4463, 106.3366});
        put("hà nam", new double[]{20.5367, 105.9189});
        put("nam định", new double[]{20.4227, 106.1661});
        put("ninh bình", new double[]{20.2539, 105.9750});

        // === MIỀN TRUNG ===
        put("thanh hóa", new double[]{19.8067, 105.7852});
        put("nghệ an", new double[]{19.1418, 104.9351});
        put("hà tĩnh", new double[]{18.3428, 105.9054});
        put("quảng bình", new double[]{17.4833, 106.6000});
        put("quảng trị", new double[]{16.7456, 107.1881});
        put("thừa thiên huế", new double[]{16.4637, 107.5905});
        put("huế", new double[]{16.4637, 107.5905}); // Alias
        put("đà nẵng", new double[]{16.0471, 108.2062});
        put("quảng nam", new double[]{15.5411, 107.9944});
        put("quảng ngãi", new double[]{15.1205, 108.7923});
        put("bình định", new double[]{14.1678, 108.9056});
        put("phú yên", new double[]{13.0883, 109.3130});
        put("khánh hòa", new double[]{12.2388, 109.1967});
        put("nha trang", new double[]{12.2388, 109.1967}); // Alias
        put("ninh thuận", new double[]{11.6667, 108.9167});
        put("bình thuận", new double[]{11.0827, 108.0664});

        // === TÂY NGUYÊN ===
        put("kon tum", new double[]{14.3598, 107.9944});
        put("gia lai", new double[]{13.8055, 108.2045});
        put("đắk lắk", new double[]{12.8333, 108.0000});
        put("đăk lăk", new double[]{12.8333, 108.0000}); // Alias (Không dấu sắc)
        put("đắk nông", new double[]{12.2223, 107.7281});
        put("đăk nông", new double[]{12.2223, 107.7281}); // Alias
        put("lâm đồng", new double[]{11.5646, 107.9868});
        put("đà lạt", new double[]{11.9404, 108.4384}); // Alias

        // === MIỀN NAM ===
        put("hồ chí minh", new double[]{10.8231, 106.6297});
        put("hcm", new double[]{10.8231, 106.6297}); // Alias
        put("sài gòn", new double[]{10.8231, 106.6297}); // Alias
        put("bà rịa - vũng tàu", new double[]{10.4956, 107.1682});
        put("bà rịa", new double[]{10.4956, 107.1682}); // Alias
        put("vũng tàu", new double[]{10.3459, 107.0843}); // Alias
        put("bình dương", new double[]{11.2233, 106.6253});
        put("bình phước", new double[]{11.7500, 106.9167});
        put("đồng nai", new double[]{10.9416, 107.2917});
        put("tây ninh", new double[]{11.3000, 106.1000});
        put("long an", new double[]{10.6406, 106.1820});
        put("tiền giang", new double[]{10.4187, 106.3129});
        put("bến tre", new double[]{10.2403, 106.3753});
        put("trà vinh", new double[]{9.8437, 106.3314});
        put("vĩnh long", new double[]{10.2537, 105.9722});
        put("đồng tháp", new double[]{10.4633, 105.6321});
        put("an giang", new double[]{10.5216, 105.1259});
        put("kiên giang", new double[]{10.0125, 105.0809});
        put("cần thơ", new double[]{10.0452, 105.7469}); // KHO GỐC
        put("hậu giang", new double[]{9.7984, 105.6200});
        put("sóc trăng", new double[]{9.6033, 105.9722});
        put("bạc liêu", new double[]{9.2941, 105.7278});
        put("cà mau", new double[]{9.1769, 105.1524});
    }};

    // ─────────────────────────────────────────────────
    // PUBLIC 1: calculateFee (DÙNG TRONG CHECKOUT CONTROLLER)
    // ─────────────────────────────────────────────────
    public BigDecimal calculateFee(Address address, List<CartDetail> cartList, Account account) {
        
        // 1. KIỂM TRA MEMBERSHIP (Vàng hoặc Kim Cương -> FREE SHIP)
        if (account != null && account.getMembership() != null) {
            String rank = account.getMembership().getName();
            if ("Vàng".equalsIgnoreCase(rank) || "Kim Cương".equalsIgnoreCase(rank)) {
                return BigDecimal.ZERO;
            }
        }

        if (address == null || address.getProvince() == null) {
            return BigDecimal.valueOf(30_000);
        }

        // 2. TÌM TỌA ĐỘ (Tìm thông minh, bỏ qua hoa/thường)
        double[] origin = PROVINCE_MAP.get("cần thơ"); // Mốc kho hàng ở Cần Thơ
        double[] dest = findCoordinates(address.getProvince());

        long baseFee = 30000; // Giá mặc định nếu nhập sai/chưa hỗ trợ tên Tỉnh đó

        // 3. TÍNH KHOẢNG CÁCH (Nếu tìm thấy tọa độ tỉnh đích)
        if (dest != null) {
            double distance = calculateDistance(origin, dest);
            
            if (distance < 50) {
                baseFee = 20_000;      // Dưới 50km (Ví dụ: Nội thành Cần Thơ, Hậu Giang lân cận)
            } else if (distance < 200) {
                baseFee = 30_000;      // 50km - 200km (Ví dụ: Miền Tây, TP.HCM)
            } else if (distance < 800) {
                baseFee = 45_000;      // 200km - 800km (Ví dụ: Miền Trung, Tây Nguyên)
            } else {
                baseFee = 60_000;      // Trên 800km (Ví dụ: Miền Bắc)
            }
        }

        // 4. TÍNH PHÍ CÂN NẶNG
        double totalWeight = getTotalWeight(cartList); // Tính bằng Gram
        
        // Mặc định phí baseFee đã bao gồm 1000g (1kg) đầu tiên.
        // Cứ vượt quá 1000g, mỗi 500g tính thêm 5,000 VND.
        long weightFee = 0;
        if (totalWeight > 1000) {
            double extraWeight = totalWeight - 1000;
            // Dùng Math.ceil() để làm tròn lên: Lố 100g cũng tính là 1 block (500g)
            long blocks = (long) Math.ceil(extraWeight / 500.0);
            weightFee = blocks * 5000;
        }

        long finalFee = baseFee + weightFee;

        return BigDecimal.valueOf(finalFee);
    }

    // ─────────────────────────────────────────────────
    // PUBLIC 2: calculate (DÙNG CHO AJAX/API TRONG GIỎ HÀNG)
    // ─────────────────────────────────────────────────
    public Map<String, Object> calculate(Long addressId, Account account) {
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        List<CartDetail> cartItems = cartDetailRepository
            .findByAccount_Id(account.getId());

        BigDecimal feeShip = calculateFee(address, cartItems, account);

        BigDecimal rawTotal = cartItems.stream()
            .map(i -> i.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalTotal = rawTotal.add(feeShip);

        return Map.of(
            "feeShip", feeShip.longValue(),
            "finalTotal", finalTotal
        );
    }

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ: Tìm kiếm Tỉnh thông minh
    // ─────────────────────────────────────────────────
    private double[] findCoordinates(String inputProvince) {
        if (inputProvince == null || inputProvince.trim().isEmpty()) return null;
        
        // Chuyển thành chữ thường để so sánh (Ví dụ: "Thành phố Cần Thơ" -> "thành phố cần thơ")
        String normalizedInput = inputProvince.toLowerCase().trim();

        // Duyệt qua 63 tỉnh
        for (Map.Entry<String, double[]> entry : PROVINCE_MAP.entrySet()) {
            String mapProvince = entry.getKey(); // Key trong map đã là chữ thường
            
            // Nếu người dùng nhập "Thành phố Hồ Chí Minh" chứa chữ "hồ chí minh" -> Match!
            // Hoặc ngược lại (ít gặp) người dùng nhập "HCM", map có "hcm" -> Match!
            if (normalizedInput.contains(mapProvince) || mapProvince.equals(normalizedInput)) {
                return entry.getValue();
            }
        }
        return null; // Không tìm thấy
    }

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ: Tính khoảng cách Haversine (Theo đường chim bay)
    // ─────────────────────────────────────────────────
    private double calculateDistance(double[] a, double[] b) {
        double R = 6371; // Bán kính trái đất (km)

        double dLat = Math.toRadians(b[0] - a[0]);
        double dLon = Math.toRadians(b[1] - a[1]);

        double lat1 = Math.toRadians(a[0]);
        double lat2 = Math.toRadians(b[0]);

        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2)
                * Math.cos(lat1) * Math.cos(lat2);

        double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));

        return R * c; // Khoảng cách theo Kilomet
    }

    // ─────────────────────────────────────────────────
    // HÀM NỘI BỘ: Tính tổng cân nặng từ giỏ hàng (Gram)
    // ─────────────────────────────────────────────────
    private double getTotalWeight(List<CartDetail> cartItems) {
        if (cartItems == null) return 0;
        
        return cartItems.stream()
            .mapToDouble(item -> {
                Double w = item.getProduct().getWeight();
                int qty  = item.getQuantity();
                // Nếu sản phẩm không cấu hình cân nặng, hoặc cân nặng <= 0, mặc định là 500g
                return (w != null && w > 0 ? w : 500.0) * qty; 
            })
            .sum();
    }
}