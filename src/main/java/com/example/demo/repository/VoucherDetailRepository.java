package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.model.VoucherDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherDetailRepository extends JpaRepository<VoucherDetail, Long> {

	// Tìm voucher chi tiết của account dựa vào mã voucher, đảm bảo chưa sử dụng
	@Query("SELECT vd FROM VoucherDetail vd " + "JOIN FETCH vd.voucher v " + "WHERE vd.account.id = :accountId "
			+ "AND v.code = :code " + "AND vd.isUsed = false " + "AND v.active = true")
	Optional<VoucherDetail> findValidVoucherForAccount(@Param("accountId") Integer accountId,
			@Param("code") String code);

	List<VoucherDetail> findByAccount_Id(Integer accountId);

	@Query("""
			SELECT vd FROM VoucherDetail vd
			JOIN FETCH vd.voucher v
			WHERE vd.account.id = :accountId
			AND vd.isUsed = false
			AND v.active = true
			AND v.expiredAt > :now
			""")
	List<VoucherDetail> findAvailableVouchers(@Param("accountId") Integer accountId, @Param("now") LocalDateTime now);
}