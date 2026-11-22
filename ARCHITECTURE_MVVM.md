# Kiến trúc MVVM thống nhất cho Bếp Nhà

## Mục đích và phạm vi

Đây là chuẩn kiến trúc bắt buộc cho code mới và phần refactor của tất cả module, áp dụng cho Người 1, Người 2 và Người 3. Không yêu cầu viết lại ứng dụng từ đầu. Refactor theo từng lát tính năng, giữ nguyên giao diện, điều hướng, quy tắc nghiệp vụ và dữ liệu hiện có.

Nền tảng giữ nguyên: Java 11, AndroidX, XML cho host/layout phù hợp, ViewBinding khi truy cập view XML, ViewModel, LiveData, SQLiteOpenHelper và các thư viện đang dùng. Không thêm Kotlin, Compose, thư viện mới hoặc tầng trừu tượng không có nhu cầu cụ thể.

## Cấu trúc package đích

Chỉ tạo package khi có lớp sử dụng thực tế; không tạo thư mục rỗng.

```text
com.example.myapplication/
├── core/
│   ├── common/       # tiện ích thuần, không chứa nghiệp vụ module
│   ├── result/       # trạng thái/kết quả dùng chung nếu có nhu cầu
│   └── ui/           # thành phần trình bày dùng chung thực sự
├── data/
│   ├── local/
│   │   ├── database/ # SQLiteOpenHelper, entity/mapper DB nếu cần
│   │   └── storage/  # file/ảnh cục bộ
│   ├── model/        # DTO/entity riêng tầng data nếu cần
│   ├── repository/   # implementation của domain repository
│   ├── remote/       # chỉ thêm khi data source từ xa tồn tại
│   └── mapper/       # chỉ thêm khi có chuyển đổi model thực tế
├── domain/
│   ├── model/        # model nghiệp vụ không phụ thuộc Android UI
│   ├── repository/   # hợp đồng truy cập dữ liệu
│   ├── usecase/      # nghiệp vụ có ý nghĩa; không bọc từng getter/setter
│   └── catalog/      # danh mục và quy tắc dùng chung đã chốt
├── presentation/
│   ├── main/         # MainActivity, host điều hướng và app-level state
│   ├── cookingjournal/
│   ├── discover/
│   ├── mealplan/
│   ├── pantry/
│   ├── shopping/
│   └── personaldata/
└── di/               # AppContainer và khởi tạo phụ thuộc thủ công
```

Các feature package được tạo khi có Fragment/ViewModel/component cụ thể. Mỗi feature có thể chứa `components/` khi UI có phần thực sự dùng lại hoặc Adapter riêng.

## Quy tắc phụ thuộc

```text
presentation → domain
data → domain
domain ↛ presentation, Android UI, SQLite, Firebase hoặc data implementation
```

- **View (Activity, Fragment, Adapter, View):** vẽ state, nhận thao tác người dùng, gọi ViewModel và quan sát LiveData. Không gọi Repository, SQLite, file store hoặc thực hiện phép tính nghiệp vụ.
- **ViewModel:** giữ state của màn hình, nhận event, gọi use case hoặc repository interface, phát LiveData cho View. Không giữ Activity/View/Context, không thao tác widget, không truy vấn DB trực tiếp.
- **Domain:** giữ model và quy tắc nghiệp vụ; định nghĩa repository interface/use case khi có ích. Không phụ thuộc Presentation hoặc Android UI.
- **Data:** hiện thực repository interface; điều phối database, storage và remote data source; chuyển đổi model tại mapper khi cần. I/O chạy ngoài main thread.
- **DI:** `AppContainer` tạo một `PersonalDataDbHelper`, storage và các repository dùng chung; ViewModel nhận dependency qua constructor và `ViewModelProvider.Factory`. Không tạo repository/database trong Activity, ViewModel hay global singleton tùy tiện.
- **MainActivity:** chỉ quản lý lifecycle cấp Activity, host/navigation toàn app, system bars/insets và phối hợp màn hình cấp cao. Không chứa UI/logic riêng của feature.
- **Fragment:** sở hữu presentation của feature, observe ViewModel và gửi event. Điều hướng/back stack phải giữ hành vi hiện hành.

## Quy tắc cho Người 2 và các module dùng chung

Người 2 phải áp dụng cùng cấu trúc và quy tắc trên khi triển khai module 3/4/9 hoặc cung cấp catalog/công thức cho module khác:

1. Đặt UI/Fragment/ViewModel trong `presentation/<feature>/`; không thêm màn hình hoặc nghiệp vụ vào `MainActivity`.
2. Đặt model nghiệp vụ trong `domain/model/`; đặt catalog/quy tắc chung đã thống nhất trong `domain/catalog/`.
3. Khai báo hợp đồng dữ liệu trong `domain/repository/`; implementation và Firebase/SQLite adapter ở `data/`.
4. Dùng `recipeId`, `ingredientId`, `unitId` và quy tắc BigDecimal đã chốt; không tạo khóa từ tên hiển thị và không thêm schema/API dùng chung khi chưa phối hợp.
5. Cấp dependency qua `AppContainer`/factory hiện có; không tạo singleton song song hoặc gọi module khác qua Activity nội bộ.
6. Giữ Java/XML, giao diện đã duyệt, responsive behavior và test hiện có. Thêm test cho nghiệp vụ mới và migration nếu schema thay đổi.
7. Mỗi PR ghi rõ module, package được sửa, schema/API contract, test chạy và giới hạn tích hợp còn lại.

## Kế hoạch refactor theo pha

1. Baseline và sơ đồ lớp/phụ thuộc; xác định UI, điều hướng, business logic và I/O đang nằm trong MainActivity.
2. Tạo model nghiệp vụ/repository boundary và dependency container; bỏ phụ thuộc từ data/domain tới Activity.
3. Chuyển class vào package feature/data/domain, cập nhật test và xác minh compile.
4. Tách MainActivity thành navigation host và feature Fragments/components theo lát nhỏ; giữ nguyên state restoration, back stack, dialogs, keyboard và thiết kế.
5. Đưa I/O ra khỏi main thread và chuyển state ViewModel sang LiveData, bảo toàn event/result hiện tại.
6. Chạy build/test sau từng pha; cuối cùng chạy full unit + instrumentation + assemble và kiểm tra migration, khởi động, điều hướng, UI/responsive trên viewport đại diện.

Không bắt đầu pha tiếp theo khi compile/test của pha hiện tại hỏng. Nếu test không thể chạy trên môi trường hiện tại, ghi trạng thái `NOT RUN`; không suy diễn pass.

## Baseline trước refactor hiện tại

- Commit gốc: `cfa93db` (`Reapply "feat(discover): complete modules 1 2 and 8"`).
- `testDebugUnitTest`, `assembleDebug`, `connectedDebugAndroidTest`: build thành công; 36 instrumentation tests qua trên Pixel 7 AVD / Android 17.
- `MainActivity.java`: 3.468 dòng; chứa navigation và UI/render/event handling của nhiều feature.
- `activity_main.xml` hiện chỉ là layout placeholder; UI feature chủ yếu dựng bằng Java. Giữ nguyên UI đã duyệt khi thay host/layout; chỉ đưa nội dung vào XML khi có thể bảo toàn kích thước, spacing, typography, màu sắc, scroll/IME và trạng thái.
- `PersonalDataRepository` là singleton tự khởi tạo DB/photo store; nhiều ViewModel tự lấy repository. `PersonalDataDbHelper` chứa persistence cho các feature 5–7 và model nested; `RecipePreview` đang nằm trong MainActivity và được data/repository/ViewModel tham chiếu. Đây là các phụ thuộc cần loại bỏ theo từng pha.
- `.idea/gradle.xml` và `.idea/misc.xml` có thay đổi cục bộ trước refactor; không đưa các thay đổi đó vào commit kiến trúc nếu không được yêu cầu.

## Trạng thái refactor đang thực hiện

Đây là trạng thái giữa chừng, không phải xác nhận refactor hoàn tất:

- Đã tách `Recipe` cùng các model kế hoạch, bộ sưu tập, mua sắm, tủ nguyên liệu và nhật ký khỏi `MainActivity`/SQLite helper sang `domain/model/`.
- Đã chuyển `PersonalDataDbHelper`, photo storage, catalog và các ViewModel vào package data/domain/feature tương ứng. Tên bảng, phiên bản database và migration không được đổi trong bước này.
- Đã thêm `domain/repository/PersonalDataRepository`, implementation SQLite `data/repository/LocalPersonalDataRepository`, `BepNhaApplication`, `AppContainer` và `AppViewModelFactory`. ViewModel module 5–7 cùng tìm kiếm nhận repository dependency qua factory thay vì tự tạo database/singleton.
- `MainActivity` đã chuyển vào `presentation/main/`; dữ liệu fixture debug đã chuyển vào `PreviewRecipeCatalog`.
- `ShoppingViewModel` (Module 7) hiện phát `ShoppingState`, `SaveResult` và `PlanUpdate` qua `LiveData`; tải/lưu/xóa/đổi trạng thái, đối chiếu pantry và phát hiện thay đổi kế hoạch chạy trên executor I/O dùng chung từ `AppContainer`. Đổi trạng thái/xóa cập nhật giao diện lạc quan ngay, có rollback state khi ghi thất bại. `MainActivity` quan sát state/result để render và hiện thông báo.
- `MealPlanViewModel` (Module 6) hiện phát `PlanState` qua `LiveData`; tải kế hoạch, yêu thích, bộ sưu tập và món trong bộ sưu tập trên executor I/O. Thêm/xóa/khôi phục/sắp xếp món, đổi khẩu phần và lưu lựa chọn cập nhật state lạc quan, sau đó ghi danh sách kế hoạch trên executor. Luồng UI hiện tại trong `MainActivity` quan sát state để dựng lại màn hình.
- `PantryViewModel` (Tủ nguyên liệu trong Module 7) hiện phát `PantryState`/`MutationResult` qua `LiveData`; tải/lưu/xóa/xóa tất cả thực hiện trên executor dùng chung, xóa cập nhật UI ngay và rollback khi ghi lỗi. Màn hình tiếp tục dùng cùng thiết kế và trạng thái sắp hết/hết hạn được tính cục bộ.
- `PersonalDataViewModel` (Module 5) hiện phát snapshot yêu thích/lịch sử/bộ sưu tập/ghi chú qua `LiveData`; seed preview, tải dữ liệu và mọi thao tác SQLite chạy trên executor dùng chung. Yêu thích/lịch sử cập nhật cache tức thời; lưu bộ sưu tập/ghi chú trả kết quả bất đồng bộ. `MainActivity` vẫn sở hữu renderer và điều hướng các màn này.
- Kiểm chứng gần nhất: `testDebugUnitTest assembleDebug connectedDebugAndroidTest` thành công; 40 instrumentation tests qua trên Pixel 7 AVD / Android 17, gồm test async cho yêu thích/lịch sử, Shopping, kế hoạch và Pantry.
- **Còn phải hoàn tất:** tách UI/state/điều hướng feature khỏi `MainActivity` thành các Fragment được liệt kê ở cấu trúc đích; chuyển `CookingJournalViewModel`, `DiscoverSearchViewModel`/lưu lịch sử tìm kiếm và các truy cập repository còn lại sang state/I/O bất đồng bộ; đưa thao tác ảnh nhật ký ra khỏi main thread; kiểm thử lại điều hướng/back stack, process recreation, keyboard/insets và thiết kế responsive sau khi tách Fragment; kiểm tra dependency hướng domain/data, loại bỏ import không cần và chạy full build/unit/instrumentation trước khi chốt.
- Do các phần bắt buộc còn thiếu, hiện chỉ được mô tả là **MVVM foundation/package refactor in progress**. Không ghi vào báo cáo bàn giao là “đã hoàn thiện MVVM” cho tới khi checklist còn lại pass.
