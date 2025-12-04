package com.example.demo.model;

import java.util.Date; 

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotNull(message = "Ngày sinh không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Past(message = "Ngày sinh phải là trong quá khứ")
    Date birthDay;

    Boolean actived;

    Boolean role;
}
