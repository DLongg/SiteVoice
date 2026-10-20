# Khuôn schema chung của một Mẫu

Dùng cho cả Mẫu đóng gói sẵn (`nhat-ky-cong-truong`) lẫn Mẫu app tự sinh từ form của người dùng.

## Trường cấp Mẫu
| Trường | Ý nghĩa |
|---|---|
| id, ten | định danh, tên hiển thị |
| gom_theo | "ngay" (sau này: "tuan", "thang") |
| gioi_han_trang | số trang tối đa |
| form_nguon | (Mẫu tự sinh) `{loai: "gdoc"|"docx", doc_id, ma_bam, ngay_thiet_lap}` |
| muc_ghi_chu | danh sách khóa nhãn ghi chú dùng cho phân loại |
| cho_dien | danh sách chỗ điền (bảng dưới) |

## Một chỗ điền (`cho_dien[]`)
| Trường | Ý nghĩa |
|---|---|
| ma | tên placeholder viết HOA không dấu, ví dụ `CONG_VIEC_THI_CONG` |
| ten | tên hiển thị cho người dùng |
| loai | `van_ban`, `danh_sach`, `so`, `ngay`, `o_tich` |
| nguon | `nhap_tay` hoặc khóa mục ghi chú |
| tuy_chon | với `o_tich`: danh sách lựa chọn; với `ngay`: định dạng |
| hien_thi | cách ghép chuỗi (ví dụ ô tích: `"{nhan} {o}"`) |
| gioi_han | `{toi_da_dong, ky_tu_moi_dong, toi_da_ky_tu}` (do đo sức chứa) |
| mac_dinh | giá trị mặc định cho `nhap_tay` (ví dụ `hom_qua`) |
| neo | `{truoc, sau}` đoạn chữ lân cận để tìm lại vị trí |
| do_tin_cay | 0 đến 1 từ bộ nhận diện (Mẫu mặc định: 1) |
| trang_thai | `chua_duyet`, `da_duyet`, `bo_qua` |

## Quy tắc hiển thị
- `danh_sach`: mỗi dòng bắt đầu `- `, nối bằng xuống dòng.
- `o_tich`: `` cho lựa chọn được chọn, `` cho lựa chọn còn lại.
- `ngay`: định dạng ghi trong `tuy_chon` (ví dụ `"ngày {d} tháng {m} năm {y}"`).
- Chỗ trống: thay bằng chuỗi rỗng, không để lại `{{...}}`.
