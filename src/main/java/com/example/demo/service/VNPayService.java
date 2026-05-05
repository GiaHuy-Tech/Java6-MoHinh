package com.example.demo.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.stereotype.Service;

import com.example.demo.config.VNPayConfig;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VNPayService {

	public String createOrder(long amount, String orderInfor, String urlReturn){
	    String vnp_Version = "2.1.0";
	    String vnp_Command = "pay";
	    String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
	    String vnp_IpAddr = "127.0.0.1";
	    String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
	    String orderType = "order-type";

	    Map<String, String> vnp_Params = new HashMap<>();
	    vnp_Params.put("vnp_Version", vnp_Version);
	    vnp_Params.put("vnp_Command", vnp_Command);
	    vnp_Params.put("vnp_TmnCode", vnp_TmnCode);

	    // ❗ FIX QUAN TRỌNG NHẤT: KHÔNG nhân 100 ở đây nữa
	    vnp_Params.put("vnp_Amount", String.valueOf(amount));

	    vnp_Params.put("vnp_CurrCode", "VND");
	    vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
	    vnp_Params.put("vnp_OrderInfo", orderInfor);
	    vnp_Params.put("vnp_OrderType", orderType);

	    vnp_Params.put("vnp_Locale", "vn");
	    vnp_Params.put("vnp_ReturnUrl", urlReturn);
	    vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

	    Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

	    String vnp_CreateDate = formatter.format(cld.getTime());
	    vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

	    cld.add(Calendar.MINUTE, 15);
	    String vnp_ExpireDate = formatter.format(cld.getTime());
	    vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

	    List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
	    Collections.sort(fieldNames);

	    StringBuilder hashData = new StringBuilder();
	    StringBuilder query = new StringBuilder();

	    for (int i = 0; i < fieldNames.size(); i++) {
	        String fieldName = fieldNames.get(i);
	        String fieldValue = vnp_Params.get(fieldName);

	        if (fieldValue != null && !fieldValue.isEmpty()) {
	            try {
	                hashData.append(fieldName)
	                        .append('=')
	                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

	                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
	                     .append('=')
	                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

	                if (i < fieldNames.size() - 1) {
	                    query.append('&');
	                    hashData.append('&');
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    String vnp_SecureHash = VNPayConfig.hmacSHA512(
	            VNPayConfig.secretKey,
	            hashData.toString()
	    );

	    String paymentUrl = VNPayConfig.vnp_PayUrl + "?" +
	            query + "&vnp_SecureHash=" + vnp_SecureHash;

	    return paymentUrl;
	}

    public int orderReturn(HttpServletRequest request){
        Map fields = new HashMap();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        // Gọi hàm hashAllFields đã thêm ở Bước 1
        String signValue = VNPayConfig.hashAllFields(fields);

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                return 1; // Giao dịch thành công
            } else {
                return 0; // Giao dịch thất bại
            }
        } else {
            return -1; // Sai chữ ký
        }
    }
}