package com.example.demo.model;

import java.time.LocalDate; 

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @NotBlank(message = "Email không được để trống")
    @Column(unique = true)
    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
    String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Column(columnDefinition = "nvarchar(255)")
    @Size(max = 100, message = "Họ tên không vượt quá 100 ký tự")
    String fullName;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Column(columnDefinition = "nvarchar(255)")
    @Size(max = 255, message = "Địa chỉ không vượt quá 255 ký tự")
    String address;

    @Column(columnDefinition = "nvarchar(255)")
    String photo;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    String phone;

    @NotNull(message = "Giới tính không được để trống")
    Boolean gender;

    // --- ĐÃ SỬA LẠI ĐOẠN NÀY ---
    @NotNull(message = "Ngày sinh không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Past(message = "Ngày sinh phải là trong quá khứ")
    // Ánh xạ tên biến 'birthday' vào cột 'BirthDay' trong Database để tránh lỗi không tìm thấy cột
    @Column(name = "BirthDay") 
    LocalDate birthday; 

    // --- CÁC TRƯỜNG MỚI ---
    @Column(columnDefinition = "bigint default 0")
    Long totalSpending = 0L;

    @Column(columnDefinition = "nvarchar(50) default 'Đồng'")
    String membershipLevel = "Đồng";
    
    Boolean actived;
    Boolean role;
}