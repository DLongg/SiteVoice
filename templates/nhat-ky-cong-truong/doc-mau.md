# Form nhật ký thi công: Mẫu mặc định

Link Doc mẫu đã có placeholder: (dán link vào đây)

## A. Form gốc (đầu vào cho bài kiểm tra chuẩn của bộ nhận diện)
Form người dùng thực tế, có chấm và ô tích; app phải tự nhận ra 13 chỗ điền ở dưới.
```
Hà Nội, ngày ..... tháng ..... năm 2026

1. Thời tiết:
- Sáng:  trời nắng    trời mưa
- Chiều: trời nắng    trời mưa
- Tối:   Bình thường  trời mưa

2. Lực lượng thi công:
* Nhân lực:  - Ban chỉ huy công trường: ............ người
             - Công nhân: ............ người
* Máy móc  thiết bị thi công chính:
(đoạn trống hoặc dòng chấm)

3. Công việc thi công trong ngày:
(nhiều dòng chấm/đoạn trống)
4. Công việc nghiệm thu trong ngày:
(nhiều dòng chấm/đoạn trống)
5. An toàn lao động và vệ sinh môi trường:
(nhiều dòng chấm/đoạn trống)
6. Ý kiến của các đơn vị:
a. Chủ đầu tư:   (dòng chấm)
b. Tư vấn giám sát:   (dòng chấm)
c. Nhà thầu thi công:   (dòng chấm)

ĐƠN VỊ THI CÔNG                TƯ VẤN GIÁM SÁT    khối chữ ký, KHÔNG phải chỗ điền
```

## B. Form đã đặt placeholder (kết quả mong đợi, 13 chỗ điền)
```
Hà Nội, {{NGAY_DAY_DU}}

1. Thời tiết:
- Sáng:  {{THOI_TIET_SANG}}
- Chiều: {{THOI_TIET_CHIEU}}
- Tối:   {{THOI_TIET_TOI}}

2. Lực lượng thi công:
* Nhân lực:  - Ban chỉ huy công trường: {{BAN_CHI_HUY}} người
             - Công nhân: {{CONG_NHAN}} người
* Máy móc  thiết bị thi công chính:
{{MAY_MOC}}

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

## C. Bảng placeholder
| Placeholder | Loại | Nguồn | Giá trị sinh ra |
|---|---|---|---|
| {{NGAY_DAY_DU}} | ngay | nhap_tay | `ngày 20 tháng 10 năm 2026` |
| {{THOI_TIET_SANG}} | o_tich | nhap_tay | `trời nắng    trời mưa ` |
| {{THOI_TIET_CHIEU}} | o_tich | nhap_tay | `trời nắng    trời mưa ` |
| {{THOI_TIET_TOI}} | o_tich | nhap_tay | `Bình thường    trời mưa ` |
| {{BAN_CHI_HUY}} | so | nhap_tay | `5` |
| {{CONG_NHAN}} | so | nhap_tay | `30` |
| {{MAY_MOC}} | van_ban | nhap_tay | danh sách máy móc mặc định |
| {{CONG_VIEC_THI_CONG}} | danh_sach | thi_cong | mỗi dòng bắt đầu `- ` |
| {{NGHIEM_THU}} | danh_sach | nghiem_thu | mỗi dòng bắt đầu `- ` |
| {{AN_TOAN}} | danh_sach | an_toan | mỗi dòng bắt đầu `- ` |
| {{Y_KIEN_CDT}} | van_ban | y_kien_cdt | văn bản |
| {{Y_KIEN_TVGS}} | van_ban | y_kien_tvgs | văn bản |
| {{Y_KIEN_NHA_THAU}} | van_ban | y_kien_nha_thau | văn bản |

Trường `chua_phan_loai` không có trong Doc; chỉ hiện ở màn hình xem trước.

## D. Lưu ý
- Giữ font, cỡ chữ, lề của form để kiểm soát số trang ( 2).
- Khối chữ ký phải được nhận diện là `bo_qua`.
