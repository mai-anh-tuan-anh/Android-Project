# Đề xuất hợp đồng dữ liệu chung cho module 5–7

**Phiên bản 1 · 20/11/2026**  
Phạm vi: Kho món cá nhân, Nấu hôm nay, Mua sắm; phối hợp với module Công thức/Khẩu phần.

> Đây là baseline dữ liệu module 5–7. Quyết định được người dùng chốt trong `BAN_GIAO_DU_AN_ANDROID.md` có hiệu lực cao hơn ghi chú đề xuất cũ tại đây. Các quy tắc cần chủ module khác xác nhận vẫn được đánh dấu chờ nhóm. Phương án không thay đổi bộ màn hình hoặc luồng đã thiết kế.

## 1. Mã định danh

- `recipeId` là ID tài liệu Firebase dạng chuỗi, duy nhất và bất biến. Tạo ID khi tạo công thức; không tạo từ tên món. Đổi tên, sửa công thức hoặc đồng bộ offline không đổi ID.
- `ingredientId` là ID chuỗi trong danh mục nguyên liệu chuẩn dùng chung. Tên hiển thị có thể sửa; không dùng tên hiển thị làm khóa. Từ đồng nghĩa chỉ cùng nguyên liệu khi được nhóm ánh xạ rõ vào cùng ID.
- Mỗi dòng nguyên liệu công thức có `recipeIngredientId` ổn định trong công thức. Danh sách mua sắm lưu nguồn theo `recipeId` và `recipeIngredientId`, không dựa vào vị trí dòng hoặc tên nguyên liệu.
- Dữ liệu cá nhân lưu ID món trên thiết bị. Đồng bộ kho công thức không xóa yêu thích, bộ sưu tập, ghi chú, kế hoạch hay danh sách mua sắm.

## 2. Khẩu phần và số lượng

- `baseServings` của công thức và `servings` người dùng chọn là số nguyên dương trong `1..999`, đã được người dùng chốt cho module 3/6/7. SQLite/UI phải cùng kiểm tra giới hạn. Migration module 6 giữ dữ liệu cũ và nới constraint từ `1..99` lên `1..999`.
- Lượng nguyên liệu định lượng được tính bằng `lượng gốc × servings / baseServings`. Tính bằng số thập phân chính xác (Java `BigDecimal`), giữ giá trị gốc, và chỉ làm tròn khi hiển thị tới 2 chữ số thập phân. Với lượng nhỏ, dùng đơn vị phù hợp trong danh mục đơn vị nấu ăn ở mục K.4 của bàn giao. Khẩu phần mới không tự thay đổi thời gian nấu.
- Lượng định tính như “vừa đủ”, “một ít” không nhân theo khẩu phần. Giữ nguyên mô tả định tính và không cộng nó với lượng số.
- Mỗi món chỉ có một dòng trong kế hoạch hiện tại. Thêm lại món đã có sẽ mở/chọn dòng đó, không tạo bản trùng; người dùng chỉnh khẩu phần trên dòng hiện có.

## 3. Danh mục và quy đổi đơn vị

- Mỗi nguyên liệu công thức nên tham chiếu `ingredientId`, lượng số tùy chọn, `unitId` chuẩn và ghi chú định tính tùy chọn. Mục nhập tay được phép không có `ingredientId`.
- Chỉ quy đổi trong cùng đại lượng: khối lượng (`mg`, `g`, `kg`) và thể tích (`ml`, `l`). Không quy đổi khối lượng ↔ thể tích nếu chưa có dữ liệu khối lượng riêng. Đơn vị đếm (`quả`, `cái`, `nhánh`...) chỉ gộp khi cùng `unitId` hoặc có ánh xạ tương đương được nhóm duyệt; không tự suy đoán.
- Khi gộp, tính trên đơn vị chuẩn của đại lượng (g hoặc ml) rồi hiển thị bằng đơn vị chuẩn nhất quán. Nguyên liệu không có ID chuẩn, khác đại lượng, hoặc lượng định tính không bị gộp nhầm. Khóa gộp cũng phân biệt loại lượng (định lượng/định tính). Danh sách có thể có nhiều dòng cùng tên khi chúng có đơn vị/đại lượng không tương thích.

## 4. Kế hoạch và danh sách mua sắm

- Kế hoạch lưu cục bộ gồm `recipeId` duy nhất, khẩu phần đã chọn và thứ tự hiển thị. Danh sách mua sắm được tạo từ các món và khẩu phần trong kế hoạch; không tạo lại bằng cách cộng dồn vào kết quả cũ.
- Mỗi danh sách mua sắm có các mục tổng hợp và các đóng góp nguồn theo món/dòng nguyên liệu. Khóa gộp đề xuất: `ingredientId + đại lượng + unitId chuẩn + loại lượng`; nhờ vậy có thể tính lại khi đổi khẩu phần hoặc bỏ món mà không làm mất nguồn còn lại.
- Nút cập nhật danh sách (luồng màn 22) tính lại các đóng góp từ công thức theo kế hoạch hiện tại và giữ nguyên mục thêm tay. Thực hiện trong một giao dịch cục bộ để không lưu trạng thái nửa chừng.
- Mục được gộp giữ trạng thái `Cần mua`, `Đã có` hoặc `Đã mua`. Khi cập nhật, giữ trạng thái nếu lượng không tăng; nếu lượng cần tăng thì chuyển về `Cần mua` để tránh hiểu nhầm là đã đủ. Mục không còn nguồn công thức sẽ bị loại nếu chỉ do công thức tạo; mục thêm tay vẫn giữ.
- Mục người dùng thêm có đóng góp riêng. Nếu cùng nguyên liệu/đơn vị với mục công thức thì hiển thị chung một dòng nhưng giữ riêng đóng góp thủ công để cập nhật công thức không xóa nó; nếu không cùng khóa gộp thì hiển thị dòng riêng.
- Khi người dùng sửa lượng của mục tổng hợp, lưu phần điều chỉnh của người dùng riêng với tổng tính từ công thức. Lần cập nhật sau áp dụng lại phần điều chỉnh trên tổng mới; không cho lưu tổng lượng âm. Nếu toàn bộ nguồn công thức bị bỏ, giữ đóng góp thủ công và phần điều chỉnh như mục do người dùng quản lý.
- Nếu người dùng chọn **Giữ danh sách cũ**, không tính lại các mục. Nếu chọn **Cập nhật**, dùng quy tắc trên. Thêm cùng một nguồn công thức lần nữa là thao tác upsert, không tạo thêm đóng góp trùng.

## 5. Hành vi dữ liệu cá nhân và món bị gỡ

- Khóa `Favorite` là `recipeId`; lưu lặp lại là idempotent. Bộ sưu tập và ghi chú cũng liên kết bằng `recipeId`, không nhân bản dữ liệu công thức.
- Khi tích hợp kho công thức thật, form tạo/sửa bộ sưu tập của module 5 lấy món từ toàn bộ kho khả dụng, không chỉ món yêu thích; hỗ trợ tìm kiếm và chỉ tải phần tiếp theo khi người dùng cuộn đến (lazy loading). Các lựa chọn đã tích phải giữ nguyên khi tìm kiếm hoặc tải thêm, dựa trên `recipeId` ổn định. Đây là mục tiêu tích hợp; bản preview hiện tại vẫn dùng 5 fixture.
- Khi món bị gỡ khỏi kho công khai, ẩn khỏi danh mục công khai nhưng không xóa dữ liệu cá nhân đang tham chiếu. Giữ tên/ảnh snapshot tối thiểu để màn hình cá nhân hiển thị mục đã gỡ; ghi chú vẫn đọc được. Không tự xóa dòng mua sắm hoặc kế hoạch của người dùng khi đồng bộ.

## 6. Mô hình bảng SQLite tối thiểu đề xuất

| Bảng | Khóa và trường cốt lõi |
| --- | --- |
| `Recipe` | `recipeId TEXT PRIMARY KEY`, `baseServings INTEGER`, trạng thái công khai và các trường nội dung do module công thức quản lý |
| `Ingredient` | `ingredientId TEXT PRIMARY KEY`, tên hiển thị, nhóm hiển thị, trạng thái hoạt động |
| `RecipeIngredient` | `recipeIngredientId TEXT PRIMARY KEY`, `recipeId`, `ingredientId NULL`, `amountDecimal TEXT NULL`, `unitId NULL`, ghi chú định tính, thứ tự |
| `Favorite` | `recipeId TEXT PRIMARY KEY`, thời điểm lưu, snapshot hiển thị tối thiểu |
| `Collection`, `CollectionRecipe` | ID bộ sưu tập; bảng liên kết có khóa duy nhất `(collectionId, recipeId)` |
| `RecipeNote` | Khóa duy nhất `recipeId`, nội dung ghi chú và thời điểm cập nhật |
| `RecipeViewHistory` | Khóa duy nhất `recipeId`, thời điểm xem gần nhất; cập nhật thay vì thêm trùng |
| `MealPlan`, `MealPlanRecipe` | ID kế hoạch; khóa duy nhất `(planId, recipeId)`, `servings`, thứ tự |
| `ShoppingList` | `listId`, `planId`, trạng thái hiện hành và thời điểm cập nhật |
| `ShoppingListItem` | `itemId`, `listId`, nguyên liệu chuẩn hoặc tên thủ công, đơn vị/đại lượng/loại lượng, lượng tổng, phần điều chỉnh, trạng thái |
| `ShoppingContribution` | `itemId`, `recipeId`, `recipeIngredientId`, lượng nguồn đã tính; khóa duy nhất theo mục và dòng nguồn |
| `ShoppingManualContribution` | `manualContributionId`, `itemId`, tên hoặc `ingredientId`, lượng/đơn vị, thời điểm thêm; giữ độc lập với đóng góp công thức |

Lượng thập phân lưu dạng chuỗi chuẩn để tránh sai số nhị phân; parse/tính bằng `BigDecimal`. Tên bảng/cột có thể điều chỉnh theo codebase, nhưng hợp đồng khóa và hành vi cần giữ.

## 7. Điểm cần nhóm xác nhận trước khi đóng hợp đồng

1. `recipeId` Firebase bất biến và danh mục `ingredientId` dùng chung đã được người dùng chấp nhận; cần chủ module 9 xác nhận cấu trúc triển khai.
2. Giới hạn khẩu phần `1..999` đã được người dùng chốt; chủ module 3/7 áp dụng cùng contract.
3. Quy tắc trạng thái khi cập nhật số lượng tăng (đặt lại `Cần mua`) là đề xuất được người dùng giao Người 3 lựa chọn; chủ module 7 xác nhận chi tiết khi triển khai.
4. Danh mục đơn vị và quy đổi cơ sở do người dùng cung cấp đã ghi tại bàn giao mục K.4. Người điều phối schema/module 7 chuyển danh mục đó thành dữ liệu chuẩn; chỉ thêm alias/quy đổi mới sau khi xác minh.

