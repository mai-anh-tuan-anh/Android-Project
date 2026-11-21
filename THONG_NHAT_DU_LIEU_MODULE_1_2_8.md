# Đề xuất hợp đồng dữ liệu chung cho Module 1, 2 và 8

**Phiên bản 1 · 22/11/2026**

**Phạm vi:** Module 1 – Khám phá & danh mục; Module 2 – Tìm kiếm & bộ lọc; Module 8 – Gợi ý món theo nguyên liệu.

**Trạng thái:** Tài liệu đề xuất dựa trên `BAN_GIAO_DU_AN_ANDROID.md` và `ui-ux-bep-nha/THIET_KE.md`. Những nội dung đã được handoff xác nhận được ghi rõ; quy tắc còn chờ nhóm thống nhất không được xem là hợp đồng đã duyệt.

## 1. Mục tiêu và nguyên tắc

Ba module cùng đọc một catalog công thức, nhưng có cách trình bày và lọc riêng. Module 1/2/8 không tự tạo bản sao dữ liệu công thức và không ghi trực tiếp vào database. ViewModel gọi repository/use case; repository là ranh giới dữ liệu; nguồn SQLite/Firebase thuộc phần tích hợp catalog do nhóm phân công.

Nguyên tắc đã xác nhận trong handoff:

- `recipeId` là ID Firebase dạng chuỗi, ổn định và bất biến; không dùng tên món làm khóa.
- `ingredientId` là ID nguyên liệu chuẩn dùng chung. Tên gọi/biến thể chỉ được quy đổi khi có alias được khai báo.
- Công thức có thể thuộc nhiều danh mục; quan hệ danh mục là nhiều-nhiều.
- `publishedAt` là thời điểm công bố, khác với `updatedAt` là thời điểm chỉnh sửa.
- MVVM áp dụng toàn dự án; các module này chỉ đọc catalog công thức.

## 2. Dữ liệu công thức dùng chung

| Trường logic | Kiểu gợi ý | Ý nghĩa / quy tắc |
|---|---|---|
| `recipeId` | String | ID ổn định, bất biến từ nguồn catalog. |
| `title` | String | Tên hiển thị món ăn. |
| `imageRef` | String? | Tham chiếu ảnh; có thể thiếu và cần ảnh thay thế ở UI. |
| `publicationStatus` | enum | Trạng thái công bố; chỉ món được phép hiển thị mới xuất hiện ở các danh sách công khai. Giá trị cụ thể cần khớp schema catalog. |
| `publishedAt` | timestamp? | Thời điểm công bố; dùng xếp món mới. Không thay bằng `updatedAt`. |
| `updatedAt` | timestamp? | Thời điểm nội dung được sửa; không làm món cũ trở thành món mới. |
| `durationMinutes` | integer? | Thời gian chế biến để hiển thị/lọc/xếp hạng; null nghĩa là chưa có dữ liệu. |
| `difficulty` | enum? | Mức độ khó; cần dùng chung bộ giá trị catalog. Null là chưa biết, không tự suy diễn. |
| `dietIds` | Set<String> | Các chế độ ăn phù hợp; chỉ gán khi nguồn dữ liệu xác nhận. |
| `ingredientDataStatus` | enum | `COMPLETE`, `INCOMPLETE` hoặc `UNKNOWN`; phân biệt danh sách nguyên liệu đầy đủ với dữ liệu chưa nhập đủ. |

Các tên trường trên là mô hình logic đề xuất, không bắt buộc trùng tên cột/field vật lý. Owner catalog cần ánh xạ với schema thực tế trước khi tích hợp.

## 3. Danh mục và nguyên liệu

### 3.1. Danh mục

Danh mục dùng ID ổn định và tên hiển thị tiếng Việt. Công thức liên kết với danh mục qua quan hệ nhiều-nhiều, tương đương `RecipeCategory(recipeId, categoryId)`. Ví dụ tên hiển thị có trong giao diện: Món Việt, Món Á, Món Âu, Bữa sáng, Món chay, Món nhanh, Dễ làm. Danh sách và ID chính thức phải lấy từ catalog/owner, không suy ra từ tên món.

### 3.2. Thành phần công thức

Mỗi thành phần cần `ingredientId` chuẩn để tìm kiếm và đối chiếu Module 8. Quan hệ logic `RecipeIngredient` gồm `recipeId`, `ingredientId` và thông tin lượng/đơn vị nếu nguồn catalog có cung cấp. Module 8 phiên bản hiện tại chỉ so khớp sự hiện diện của nguyên liệu, không tính lượng trong tủ lạnh.

Tên tiếng Việt, tên đồng nghĩa hoặc cách viết khác chỉ được tìm/so khớp như cùng một nguyên liệu khi ánh xạ tới cùng `ingredientId` hoặc qua bảng alias đã duyệt. Không dùng so khớp chuỗi gần đúng để tự kết luận hai nguyên liệu là một.

## 4. Quy tắc Module 1 – Khám phá

- Chỉ hiển thị công thức đã được công bố và được phép xuất hiện trong catalog công khai.
- Danh mục lọc theo `categoryId` chính xác; một công thức có thể xuất hiện ở nhiều danh mục.
- “Món mới” sắp theo `publishedAt` giảm dần. Sửa công thức chỉ cập nhật `updatedAt`, không đẩy món cũ lên đầu.
- Khi offline/lỗi tải, có thể hiển thị dữ liệu catalog gần nhất còn lưu trên thiết bị; cần thể hiện trạng thái lỗi/mất kết nối theo thiết kế.
- Trạng thái đang tải, danh sách rỗng và lỗi phải được xử lý riêng; không biến dữ liệu lỗi hoặc thiếu thành danh sách hợp lệ.

## 5. Quy tắc Module 2 – Tìm kiếm và bộ lọc

- Tìm theo tên món và nguyên liệu; hỗ trợ tiếng Việt có dấu/không dấu bằng chuẩn hóa Unicode và dấu tiếng Việt trước khi so sánh.
- Các điều kiện lọc kết hợp theo AND: món phải đáp ứng mọi bộ lọc đang bật.
- Bộ lọc gồm thời gian, độ khó, chế độ ăn và nguyên liệu cần loại trừ theo phạm vi handoff/UI.
- Khoảng thời gian theo thiết kế: dưới 15 phút; 15–30 phút (bao gồm hai đầu); trên 30 phút. Bản ghi thiếu thời gian không được gán vào một khoảng.
- Chỉ áp dụng chế độ ăn khi metadata tương ứng đã được xác nhận. Không suy diễn “món chay” từ tên hoặc danh sách thành phần chưa đầy đủ.
- Nếu dữ liệu nguyên liệu của công thức chưa đầy đủ, tìm kiếm nguyên liệu có thể không kết luận được; giữ trạng thái dữ liệu thiếu thay vì coi là chắc chắn không có nguyên liệu đó.
- Lịch sử tìm kiếm được lưu cục bộ trong 1 giờ theo handoff. Cách giới hạn số lượng, xử lý trùng và xóa lịch sử cần được owner Module 2 chốt nếu chưa được ghi ở nơi khác.
- Không có kết quả phải dùng empty state theo `ui-ux-bep-nha/states/khong-ket-qua.png`, kèm khả năng sửa từ khóa/bộ lọc.

## 6. Quy tắc Module 8 – Gợi ý theo nguyên liệu

### 6.1. Đầu vào và tính hợp lệ

Đầu vào là tập các `ingredientId` người dùng đã chọn. Loại bỏ ID trùng. Chỉ so sánh trên công thức có danh sách thành phần được xác nhận đầy đủ (`ingredientDataStatus = COMPLETE`); công thức thiếu dữ liệu không được xếp hạng như thể danh sách của nó là đầy đủ.

### 6.2. Điểm khớp

Với tập nguyên liệu công thức `R` và tập người dùng chọn `U`:

- `available = R ∩ U`
- `missing = R − U`
- `matchCount = |available|`
- `missingCount = |missing|`

Số lượng tính theo ID nguyên liệu duy nhất, không theo dòng hiển thị hoặc đơn vị/khối lượng. Công thức đủ nguyên liệu có `missingCount = 0`.

### 6.3. Xếp hạng và hiển thị

- Xếp trước món thiếu ít nguyên liệu hơn; món đủ 100% đứng trước món còn thiếu.
- Handoff ghi nhận thứ tự phá hòa đề xuất: độ khó Dễ → Khó, thời gian ngắn → dài, sau đó tên không dấu và `recipeId`; thiếu độ khó/thời gian xếp sau giá trị đã biết.
- Thứ tự phá hòa cần được nhóm xác nhận nếu chưa được chuyển thành quyết định đã duyệt trong handoff.
- Thẻ kết quả hiển thị số nguyên liệu đã có và còn thiếu; danh sách thiếu cần dựa trên cùng các ID chuẩn.
- Trường hợp chưa chọn nguyên liệu, không có kết quả hoặc catalog thiếu dữ liệu phải có trạng thái riêng, phù hợp các state trong thiết kế.
- Phiên bản đầu không xét số lượng đang có trong tủ lạnh và không tự đề xuất thay thế nguyên liệu.

## 7. Ranh giới API logic đề xuất

Các chữ ký dưới đây mô tả trách nhiệm, không quy định tên class hay công nghệ lưu trữ cụ thể:

| Truy vấn | Kết quả |
|---|---|
| `observePublishedRecipes()` | Dòng công thức đã công bố, có cache gần nhất nếu repository hỗ trợ. |
| `observeRecipesByCategory(categoryId)` | Công thức thuộc đúng danh mục. |
| `observeNewestRecipes()` | Công thức công khai theo `publishedAt` giảm dần. |
| `searchRecipes(query, filters)` | Kết quả tìm theo tên/nguyên liệu và toàn bộ bộ lọc AND. |
| `getIngredientGroups()` | Nhóm và nguyên liệu chọn được cho Màn 23. |
| `suggestRecipes(ingredientIds)` | Kết quả có danh sách nguyên liệu khớp/thiếu và thứ hạng. |

ViewModel quản lý trạng thái màn hình, lựa chọn và lỗi; repository/use case thực hiện truy vấn và phép tính nghiệp vụ. Activity/View chỉ render state và chuyển tiếp thao tác.

## 8. Trạng thái triển khai và giới hạn dữ liệu

Tại thời điểm lập tài liệu, mã hiện có một phần UI/ViewModel và logic xem trước cho các màn thuộc phạm vi này; cần đối chiếu mã hiện tại cùng nhánh tích hợp trước khi kết luận mức hoàn thiện. Fixture debug là dữ liệu xem trước lấy từ nội dung thiết kế, không phải dữ liệu món đã công bố hay dữ liệu người dùng thật. Một số fixture chưa có `publishedAt` hoặc danh sách thành phần đầy đủ, nên không thể dùng để khẳng định kết quả “món mới” hay xếp hạng gợi ý theo catalog hoàn chỉnh.

Tài liệu này không xác nhận kết nối Firebase, schema vật lý, quyền sở hữu bảng hoặc kiểm thử tích hợp nếu handoff/nhánh hiện tại chưa xác nhận các nội dung đó.

## 9. Cần thống nhất với nhóm

1. Owner catalog/schema và nguồn sự thật cho trạng thái công bố, danh mục, thời gian, độ khó, chế độ ăn và nguyên liệu.
2. Giá trị enum chính thức cho `publicationStatus`, `difficulty` và `ingredientDataStatus`; quy ước null/thiếu dữ liệu.
3. Danh sách ID danh mục và nguyên liệu chuẩn, quy trình duyệt alias/tên đồng nghĩa.
4. Xác nhận quy tắc phá hòa Module 8 và chính sách với công thức có `R = ∅`.
5. Xác nhận giới hạn/sắp xếp lịch sử tìm kiếm nếu cần giới hạn ngoài thời gian lưu 1 giờ.
6. Hợp đồng cache/offline, đồng bộ catalog và cách biểu diễn lỗi cho các truy vấn đọc.
7. API repository/use case cụ thể sau khi schema và owner được chốt.

## Tài liệu tham chiếu

- `BAN_GIAO_DU_AN_ANDROID.md` – quyết định và trạng thái bàn giao hiện tại.
- `ui-ux-bep-nha/THIET_KE.md` cùng `ui-ux-bep-nha/screens/` và `ui-ux-bep-nha/states/` – màn hình, luồng và trạng thái giao diện được duyệt.
- `THONG_NHAT_DU_LIEU_MODULE_5_7.md` – tài liệu tham khảo về cấu trúc hợp đồng dữ liệu cho các module khác.
