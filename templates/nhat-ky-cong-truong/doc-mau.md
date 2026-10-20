# Google Doc mẫu: Nhật ký thi công

Link Doc mẫu: https://docs.google.com/document/d/1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO/edit?usp=sharing (ID: `1qe5AOo45Q9N1XenU7gsdAQkbbmSZ5guO`)

## File mẫu điện tử & File gốc tham chiếu

- [Nhat_ky_thi_cong_mau.docx](Nhat_ky_thi_cong_mau.docx): Bản mẫu điện tử đã được tạo tự động từ file gốc, thay thế toàn bộ các dòng dấu chấm dài bằng các placeholder `{{...}}` chuẩn xác, giữ nguyên 100% căn lề, font chữ, bảng thời tiết và chữ ký 2 trang A4. Dùng file này tải lên Google Drive và "Mở bằng Google Docs" để tạo Doc mẫu.
- [Nhat_ky_thi_cong.docx](Nhat_ky_thi_cong.docx): File Word gốc người dùng cung cấp (dùng để in ra viết tay với các dòng chấm dài).

## Cấu trúc nội dung trong Doc mẫu

```
Hà Nội, ngày {{NGAY}} tháng {{THANG}} năm {{NAM}}

1. Thời tiết:
- Sáng:  trời nắng {{SANG_NANG}}   trời mưa {{SANG_MUA}}
- Chiều: trời nắng {{CHIEU_NANG}}  trời mưa {{CHIEU_MUA}}
- Tối:   Bình thường {{TOI_BINH_THUONG}}  trời mưa {{TOI_MUA}}

2. Lực lượng thi công:
* Nhân lực:  - Ban chỉ huy công trường: {{BAN_CHI_HUY}} người
             - Công nhân: {{CONG_NHAN}} người
* Máy móc – thiết bị thi công chính: {{MAY_MOC}}

3. Công việc thi công trong ngày:
{{CONG_VIEC_THI_CONG}}

4. Công việc nghiệm thu trong ngày:
{{NGHIEM_THU}}

5. An toàn lao động và vệ sinh môi trường:
{{AN_TOAN}}

6. Ý kiến của các đơn vị:
a. Chủ đầu tư:
{{Y_KIEN_CDT}}
b. Tư vấn giám sát:
{{Y_KIEN_TVGS}}
c. Nhà thầu thi công:
{{Y_KIEN_NHA_THAU}}

ĐƠN VỊ THI CÔNG                TƯ VẤN GIÁM SÁT
```

## Bảng placeholder
| Placeholder | Nguồn | Ghi chú |
|---|---|---|
| {{NGAY}} {{THANG}} {{NAM}} | nhap_tay.ngay | NGAY, THANG dạng 2 chữ số tùy ý; NAM 4 chữ số |
| {{SANG_NANG}} {{SANG_MUA}} | thoi_tiet_sang | thay bằng ☑ hoặc ☐ |
| {{CHIEU_NANG}} {{CHIEU_MUA}} | thoi_tiet_chieu | ☑ hoặc ☐ |
| {{TOI_BINH_THUONG}} {{TOI_MUA}} | thoi_tiet_toi | ☑ hoặc ☐ |
| {{BAN_CHI_HUY}} {{CONG_NHAN}} | nhan_luc_* | số |
| {{MAY_MOC}} | may_moc | chuỗi |
| {{CONG_VIEC_THI_CONG}} | cong_viec_thi_cong | mỗi dòng bắt đầu "- " |
| {{NGHIEM_THU}} | nghiem_thu | mỗi dòng bắt đầu "- " |
| {{AN_TOAN}} | an_toan | mỗi dòng bắt đầu "- " |
| {{Y_KIEN_CDT}} {{Y_KIEN_TVGS}} {{Y_KIEN_NHA_THAU}} | y_kien_* | văn bản |

Trường `chua_phan_loai` không đưa vào Doc; chỉ hiển thị trong màn hình xem trước.

## Lưu ý định dạng
- Giữ font, cỡ chữ, lề trong Doc mẫu để kiểm soát số trang (≤ 2).
- Sau khi in thử, chỉnh `ky_tu_moi_dong` và `toi_da_dong` trong `schema.json` cho vừa.
