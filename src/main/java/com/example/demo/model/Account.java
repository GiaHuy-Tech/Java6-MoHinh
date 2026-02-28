package com.example.demo.model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
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

    // Nếu bạn dùng bảng Address riêng (OneToMany) thì nên bỏ dòng String address này đi 
    // hoặc giữ lại làm địa chỉ chính tùy logic
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
    @Column(name = "BirthDay")
    LocalDate birthDay;

    @Column(columnDefinition = "bigint default 0")
    Long totalSpending = 0L;

    Boolean actived;
    Boolean role;

    // =================================================================
    // PHẦN QUAN TRỌNG: THÊM VÀO ĐỂ SỬA LỖI MAPPED BY
    // =================================================================
    
    @ManyToOne
    @JoinColumn(name = "membership_id")
    private Membership membership; 
    // Lưu ý: Tên biến phải là "membership" thì bên Membership.java mới mappedBy="membership" được

    // Nếu bạn muốn lấy tên hạng (Ví dụ: "Đồng"), hãy dùng: account.getMembership().getName()
    // Dòng dưới đây nên bỏ đi để tránh dư thừa dữ liệu:
    // @Column(columnDefinition = "nvarchar(50) default 'Đồng'")
    // String membershipLevel = "Đồng";

    
    // =================================================================
    // CÁC QUAN HỆ KHÁC THEO ERD (Thêm vào để đầy đủ model)
    // =================================================================
    
    // Nếu bạn có Entity CartDetail
    @OneToMany(mappedBy = "account")
    private List<CartDetail> cartDetails;

    // Nếu bạn có Entity Order (nhớ import Order model của bạn, cẩn thận trùng với sql Order)
    // @OneToMany(mappedBy = "account")
    // private List<Order> orders;
}