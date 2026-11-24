# Firebase catalog Bếp Nhà

Thư mục này chứa cấu hình quản trị cho project `bep-nha-2ca75` trên Spark. Realtime Database ở Singapore cung cấp `/catalog.json`. Hosting classic tại `https://bep-nha-2ca75.web.app` chỉ phục vụ nội dung trong `public/`. App Android tải catalog qua HTTPS và dùng SQLite cho dữ liệu đã tải. Ảnh Hosting được tham chiếu bằng đường dẫn tương đối có phiên bản.

## Duyệt nội dung

- `catalog/catalog-draft.json` là **giá trị của node `/catalog`**. Không bọc thêm khóa `catalog` khi ghi vào `/catalog`. Bản nháp được chép từ màn 07, 09 và các trạng thái danh sách bước trong [thiết kế Bếp Nhà](../ui-ux-bep-nha/THIET_KE.md). Đây là nội dung minh họa để nhóm rà soát. Các lượng 300 g cá, 2 quả cà chua, 20 g me và “Vừa đủ” cho rau thơm là lượng có trong thiết kế. Không suy ra lượng khác từ ảnh hoặc fixture Android.
- `public/recipes/recipe-0001/cover-v1.jpg` là bản sao của ảnh [recipe_canh_chua.jpg](../app/src/main/res/drawable-nodpi/recipe_canh_chua.jpg) đã có trong app. Tài liệu thiết kế mô tả bộ ảnh minh họa tạo bằng ImageGen. Nhóm cần xác nhận quyền sử dụng công khai trước khi deploy. Ảnh bìa cũng được dùng cho bước 03 và 04 như trong prototype. Ảnh riêng của bước 02 chỉ có trong bộ thiết kế tổng hợp, nên chưa có `imagePath` riêng cho bước đó trong bản nháp.
- Chốt ID nguyên liệu và danh mục, độ đầy đủ của công thức, định lượng, bước nấu, timer và chế độ ăn. `dietIds` đang rỗng vì chưa xác nhận nhãn. `publicationStatus` đang là `DRAFT`, `ingredientDataStatus` là `INCOMPLETE`, và các mốc công bố/cập nhật sẽ được điền từ lần xuất bản được duyệt. Không đưa bản nháp, thông tin quản trị hoặc dữ liệu cá nhân vào `/catalog`. Rules cấp quyền đọc cho toàn bộ node này.
- Sau khi duyệt, tạo `catalog/catalog-approved.json` từ nội dung được duyệt. Chỉ đưa các món đã xác nhận `PUBLISHED` vào `recipes` công khai và các dấu gỡ vào `removed`. Kiểm tra revision lớn hơn revision đang có. Giá trị `1` trong bản nháp chỉ là đề xuất cho lần công bố đầu tiên. Kiểm tra mọi `imagePath` khớp chính xác với file trong `public/`. Ảnh thay đổi phải dùng tên phiên bản mới.

## Lệnh thực hiện sau khi duyệt

Chạy từ thư mục `firebase/` bằng tài khoản quản trị đang đăng nhập Firebase CLI. Đọc catalog hiện tại trước để bảo toàn nội dung đã công bố và chọn revision. Nếu đang có nội dung, hợp nhất nội dung cần giữ vào bản đã duyệt vì `database:set /catalog` thay toàn bộ node đó. RTDB Rules triển khai theo toàn bộ ruleset của instance, nên đối chiếu với quy tắc đang dùng trước khi triển khai.

```powershell
Set-Location C:\Projects\Android-Project\firebase
firebase database:get /catalog --project bep-nha-2ca75 --instance bep-nha-2ca75-default-rtdb
```

Sau khi xác nhận nội dung, ảnh và rules, chạy từng lệnh và kiểm tra kết quả trước khi chuyển bước. Ảnh cần lên Hosting trước khi ghi catalog. Với lần công bố đầu tiên, ghi catalog đã duyệt trước khi mở quyền đọc qua rules.

```powershell
firebase deploy --only hosting --project bep-nha-2ca75
curl.exe --fail --silent --show-error --head "https://bep-nha-2ca75.web.app/recipes/recipe-0001/cover-v1.jpg"
firebase database:set /catalog ./catalog/catalog-approved.json --project bep-nha-2ca75 --instance bep-nha-2ca75-default-rtdb
firebase deploy --only database --project bep-nha-2ca75
```

Đây là hai lần deploy giới hạn theo dịch vụ và một lần ghi đúng `/catalog`. Dùng cơ chế đăng nhập quản trị của Firebase CLI. Giữ cấu hình ứng dụng, ảnh nhật ký cá nhân và thông tin xác thực ngoài `public/`.

## Smoke test sau khi xuất bản

1. Kiểm tra HTTPS GET `/catalog.json` trả đúng revision và GET từng URL Hosting trong snapshot trả ảnh. Kiểm tra đọc root và đường dẫn bản nháp bị từ chối. Kiểm tra quyền ghi client bằng Rules Playground hoặc emulator để tránh ghi thử lên catalog thật.
2. Trên Pixel 8 API 37, gọi `AppContainer.catalogRefreshService.refresh()` từ một lượt smoke test có chủ đích, chờ kết quả trên test worker rồi dùng `recipeCatalogRepository` đọc revision và tra `recipeId` trên I/O executor. So sánh khẩu phần gốc, lượng dạng chuỗi và thứ tự bước với JSON đã duyệt.
3. Refresh lại với cùng revision để thấy `UNCHANGED`. Thử offline hoặc lỗi kết nối, rồi đọc lại SQLite và xác nhận snapshot hợp lệ vẫn có mặt. Việc mở app hiện tại chỉ kiểm tra UI cũ vì chưa có điểm kích hoạt refresh tự động.

Tài liệu Firebase chính thức gồm [Hosting classic](https://firebase.google.com/docs/hosting/quickstart), [Realtime Database REST](https://firebase.google.com/docs/database/rest/retrieve-data), [Security Rules](https://firebase.google.com/docs/database/security/get-started) và [Firebase CLI](https://firebase.google.com/docs/cli).
