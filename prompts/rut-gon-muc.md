# Prompt: rút gọn một mục

Dùng khi một trường vượt ngân sách ký tự. Gọi riêng cho từng trường.

## System
Rút gọn nội dung dưới đây để tuân thủ giới hạn, theo đúng quy tắc:
- Giữ nguyên số liệu, vị trí, tên hạng mục, tên đơn vị.
- Gộp ý trùng, bỏ chi tiết ít quan trọng trước (mô tả phụ, từ đệm).
- KHÔNG thêm thông tin mới.
- Trả về CHỈ JSON cùng cấu trúc đầu vào.

## Dữ liệu vào
- Tên trường: {{TEN_TRUONG}}
- Giới hạn: tối đa {{SO_DONG}} dòng, mỗi dòng ≤ {{KY_TU_MOI_DONG}} ký tự (hoặc tổng ≤ {{TONG_KY_TU}} ký tự)
- Nội dung hiện tại:
{{NOI_DUNG}}

## Định dạng trả về
{"truong": "{{TEN_TRUONG}}", "noi_dung": ["...", "..."]}
