# Bếp nhà — Ứng dụng nấu ăn và công thức

## Mục tiêu

Bếp nhà gom các việc thường dùng khi nấu ăn: tìm công thức, tính khẩu phần, lập kế hoạch bữa ăn, chuẩn bị nguyên liệu và lưu lại kinh nghiệm nấu. Dữ liệu cá nhân được dùng offline trên thiết bị. Tên “Bếp nhà” hiện là tên hiển thị dự kiến trong tài liệu nhóm.

## Phạm vi và phân công

| Người phụ trách | Module |
| --- | --- |
| Người 1 | 1 — Khám phá; 2 — Tìm kiếm và bộ lọc; 8 — Gợi ý theo nguyên liệu |
| Người 2 | 3 — Chi tiết công thức và khẩu phần; 4 — Nấu từng bước, hẹn giờ; 9 — Cập nhật kho và offline |
| Người 3 | 5 — Kho cá nhân; 6 — Nấu hôm nay; 7 — Mua sắm và Tủ nguyên liệu |

Họ tên thật và tỷ lệ đóng góp chưa được cung cấp; nhóm cần bổ sung theo thực tế trước khi nộp báo cáo.

## Công nghệ

- Android, Java 11, Android Studio và Gradle Wrapper.
- `minSdk 24`, `compileSdk/targetSdk 37`.
- Kiến trúc MVVM đã được thống nhất; việc refactor và hoàn thiện các lớp dùng chung vẫn đang tiếp tục.
- SQLite/`SQLiteOpenHelper` lưu dữ liệu cá nhân. Firebase Realtime Database và Firebase Storage là nguồn dự kiến cho catalog công thức và ảnh; tích hợp Firebase chưa hoàn tất.

## Các luồng chính

- **Khám phá, tìm kiếm, gợi ý:** duyệt danh mục, tìm món tiếng Việt có dấu/không dấu và lọc kết hợp AND. “Có gì nấu nấy” xếp gợi ý theo số nguyên liệu còn thiếu, sau đó theo độ khó và thời gian. Dữ liệu xem trước hiện là fixture debug, chưa phải catalog công thức thật.
- **Kho cá nhân:** lưu yêu thích, bộ sưu tập, ghi chú, lịch sử xem và nhật ký từng lần nấu. Lịch sử xem giữ tối đa 10 món khác nhau trong 3 ngày; xem lại một món sẽ đưa món đó lên đầu.
- **Nấu hôm nay:** khẩu phần của từng món độc lập, giới hạn 1–999; có thể đổi thứ tự món. Tổng thời gian là ước tính và chưa tính các món nấu song song.
- **Mua sắm và Tủ nguyên liệu:** danh sách có hai trạng thái `Cần mua` và `Đã có`; đổi trạng thái cập nhật ngay trong tab đang xem. Mục mới mặc định là `Cần mua`, trừ khi lượng nguyên liệu khớp trong tủ đủ dùng và chưa quá hạn. Trạng thái pantry được suy ra từ hạn sử dụng.

## Quy ước dữ liệu quan trọng

- `recipeId` và `ingredientId` là mã ổn định dùng chung; không ghép nguyên liệu chỉ vì tên gần giống.
- Một công thức có thể thuộc nhiều danh mục; quan hệ dùng bảng nối.
- Định lượng thập phân xử lý bằng `BigDecimal`; chỉ quy đổi giữa các đơn vị tương thích theo bảng chuẩn. Không tự quy đổi khối lượng sang thể tích nếu thiếu khối lượng riêng.
- Công thức cần `baseServings`; lượng sau điều chỉnh = lượng gốc × khẩu phần chọn / `baseServings`. Giữ nguyên dữ liệu công thức gốc.
- Chưa thể tự tạo danh sách mua sắm chính xác từ Nấu hôm nay cho tới khi catalog cung cấp thành phần công thức và khẩu phần gốc thật.

## Mở và chạy

1. Mở repo trong Android Studio và chờ Gradle sync.
2. Chạy cấu hình `app` trên emulator hoặc thiết bị Android.
3. Có thể build debug từ thư mục repo bằng PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

Dữ liệu món preview chỉ phục vụ kiểm tra giao diện trong debug, không đại diện cho nội dung sản phẩm đã phát hành.

## Tài liệu trong repo

- [Bàn giao dự án](BAN_GIAO_DU_AN_ANDROID.md) — quyết định nghiệp vụ và tình trạng tích hợp.
- [Kiến trúc MVVM](ARCHITECTURE_MVVM.md) — ranh giới và cách tổ chức code cho cả ba người.
- [Thống nhất dữ liệu Module 5–7](THONG_NHAT_DU_LIEU_MODULE_5_7.md).
- [Thống nhất dữ liệu Module 1, 2, 8](THONG_NHAT_DU_LIEU_MODULE_1_2_8.md).
- [Thiết kế giao diện](ui-ux-bep-nha/THIET_KE.md) và [đề xuất tính năng offline](ui-ux-bep-nha/DE_XUAT_TINH_NANG_OFFLINE.md).
- Báo cáo nhóm: [Yeu cau bai tap nhom Mobile App 11.2025 (1).docx](Yeu%20cau%20bai%20tap%20nhom%20Mobile%20App%2011.2025%20%281%29.docx).

## Làm việc nhóm

Mỗi người làm trên nhánh riêng; tích hợp qua pull request và review trước khi nhập nhánh chung. Giữ thay đổi trong phạm vi module được phân công, bám các quyết định trong file bàn giao và cập nhật tài liệu khi nhóm chốt nghiệp vụ mới.
