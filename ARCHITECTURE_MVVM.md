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

## Hợp đồng triển khai cho module khác (bắt buộc với Người 2)

- Mỗi feature đặt UI trong `presentation/<feature>/`: `<Feature>Fragment.java`, `<Feature>ViewModel.java` và `components/` khi thực sự cần. `MainActivity` chỉ implement Host interface, chọn route, khôi phục màn cấp cao và chuyển sự kiện liên module; không dựng view, dialog nghiệp vụ, validation hay truy vấn dữ liệu cho feature.
- Fragment tạo/nhận ViewModel qua `AppViewModelFactory`/`ViewModelProvider`, quan sát `LiveData` bằng `getViewLifecycleOwner()`, render state và gọi event method. Fragment không gọi repository/SQLite/file/Firebase trực tiếp. Trạng thái cần sống qua xoay màn hình nằm trong ViewModel/Fragment saved state, không đặt trong Activity field gắn với EditText.
- ViewModel nhận repository/domain contract qua constructor, giữ state và xác thực nghiệp vụ nhẹ; I/O dùng executor được cấp từ `AppContainer` và phát kết quả qua `LiveData`. Repository implementation nằm ở `data/`, interface/model nghiệp vụ ở `domain/`; schema chỉ thay khi có migration được phối hợp.
- Dùng Host callback/interface rõ tên cho route sang feature khác; không gọi method private hay thao tác widget của module khác. Một feature không tự tạo bản sao ViewModel/repository nếu state đã được share theo Activity scope.
- Áp dụng nguyên tắc trên cho Module 3/4/9 của Người 2, giữ nguyên thiết kế và các hợp đồng `recipeId`, `ingredientId`, `unitId`, khẩu phần/đơn vị. PR phải ghi rõ file/API/schema, tests/build đã chạy và trạng thái tích hợp. Mẫu hiện có: `DiscoverFragment`, `MealPlanFragment`, `ShoppingFragment`, `PersonalDataFragment`, `PantryFragment`, `CookingJournalFragment`.

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
- `ShoppingViewModel` (Module 7) phát `ShoppingState`, `SaveResult` và `PlanUpdate` qua `LiveData`; tải/lưu/xóa/đổi trạng thái, đối chiếu pantry và phát hiện thay đổi kế hoạch chạy trên executor I/O dùng chung từ `AppContainer`. `ShoppingFragment` quan sát state/result, dựng danh sách/form và sheet cập nhật kế hoạch; Activity chỉ mở route mua sắm.
- `MealPlanViewModel` (Module 6) hiện phát `PlanState` qua `LiveData`; tải kế hoạch, yêu thích, bộ sưu tập và món trong bộ sưu tập trên executor I/O. Thêm/xóa/khôi phục/sắp xếp món, đổi khẩu phần và lưu lựa chọn cập nhật state lạc quan, sau đó ghi danh sách kế hoạch trên executor. `MealPlanFragment` quan sát state và sở hữu màn Nấu hôm nay/picker.
- `PantryViewModel` (Tủ nguyên liệu trong Module 7) hiện phát `PantryState`/`MutationResult` qua `LiveData`; tải/lưu/xóa/xóa tất cả thực hiện trên executor dùng chung, xóa cập nhật UI ngay và rollback khi ghi lỗi. Màn hình tiếp tục dùng cùng thiết kế và trạng thái sắp hết/hết hạn được tính cục bộ.
- Module 7: `PantryFragment` sở hữu Tủ nguyên liệu, tìm kiếm/lọc, form thêm/sửa, xóa và trạng thái giao diện. `ShoppingFragment` sở hữu danh sách mua sắm, lọc Cần mua/Đã có, đổi trạng thái tức thời kèm Hoàn tác, xóa, bottom navigation, form thêm/sửa/xóa mục thủ công và sheet cập nhật kế hoạch; cả hai Fragment giữ draft/filter trong lifecycle phù hợp.
- Module 6 đã có `MealPlanFragment` sở hữu màn Nấu hôm nay và picker chọn món: danh sách/khẩu phần/thứ tự/hoàn tác/ước lượng thời lượng, tìm kiếm có debounce 1 giây, lọc theo tất cả/yêu thích/bộ sưu tập, selection và bottom navigation. Activity giữ điều phối route và truyền sự kiện chi tiết món/mua sắm/back/save; draft vẫn dùng `MealPlanViewModel` dùng chung.
- `PersonalDataViewModel` (Module 5) hiện phát snapshot yêu thích/lịch sử/bộ sưu tập/ghi chú qua `LiveData`; seed preview, tải dữ liệu và mọi thao tác SQLite chạy trên executor dùng chung. Yêu thích/lịch sử cập nhật cache tức thời; lưu bộ sưu tập/ghi chú trả kết quả bất đồng bộ. `PersonalDataFragment` sở hữu Kho của tôi, Yêu thích, danh sách/chi tiết/form bộ sưu tập và Ghi chú cá nhân; điều hướng sang các luồng ngoài Module 5 vẫn ở host.
- `PersonalDataFragment` quan sát `PersonalDataViewModel` và `CookingJournalViewModel` dùng chung theo scope Activity, dựng luồng kho cá nhân, form/chi tiết bộ sưu tập và ghi chú, rồi chuyển sự kiện về host cho màn chi tiết công thức, nhật ký nấu và tab. Không tạo bản sao repository/ViewModel hoặc thay đổi luồng dữ liệu khi tách UI.
- `CookingJournalViewModel` (Nhật ký nấu) phát log/kết quả qua `LiveData`; đọc/ghi/xóa log, lưu/xóa ảnh và decode thumbnail chạy trên executor I/O. `CookingJournalFragment` sở hữu nhật ký tổng hợp, nhật ký từng món, form ghi/sửa, lọc ngày/đánh giá và chọn ảnh; Activity chỉ giữ route/back/tab cấp cao, còn Fragment điều hướng về Ghi chú khi form được mở từ Module 5.
- `DiscoverSearchViewModel` tải/ghi lịch sử tìm kiếm qua executor I/O; danh sách lịch sử phát qua `LiveData`, các bộ lọc và bản nháp vẫn thuộc ViewModel. `DiscoverFragment` sở hữu ô nhập, focus và hiển thị state tìm kiếm.
- `DiscoverFragment` sở hữu Bếp nhà, danh mục, danh sách món, tìm kiếm/lọc/kết quả, gợi ý nguyên liệu và chi tiết công thức; dùng `DiscoverViewModel`, `DiscoverSearchViewModel` và `PersonalDataViewModel` qua LiveData, chuyển điều hướng liên module về host.
- `ShoppingFragment` sở hữu cả danh sách/form mua sắm và sheet cập nhật kế hoạch; draft form và filter được khôi phục qua cấu hình. `MainActivity` chỉ route đến feature và nhận callback cấp ứng dụng.
- Kiểm chứng gần nhất: `testDebugUnitTest assembleDebug connectedDebugAndroidTest` thành công; 45 instrumentation tests qua trên Pixel 7 AVD / Android 17. Bộ kiểm thử có coverage cho draft form mua sắm sau recreate, route/back trong Discover, nhật ký nấu bất đồng bộ, Fragment Kho cá nhân và picker Module 6.
- **Còn phải hoàn tất:** kiểm tra tích hợp khi Người 2 đưa Module 3/4/9 vào theo hợp đồng này; rà soát thủ công back stack/process death/keyboard-insets và responsive trên nhiều kích thước thiết bị; đánh giá dependency hướng domain/data trong các thay đổi tích hợp. `MainActivity` còn 1.034 dòng, chủ yếu là route, khôi phục màn hình và callback Host; không còn dựng giao diện Discover, Module 5–7, Tủ nguyên liệu hay Nhật ký nấu.
- Do các phần bắt buộc còn thiếu, hiện chỉ được mô tả là **MVVM foundation/package refactor in progress**. Không ghi vào báo cáo bàn giao là “đã hoàn thiện MVVM” cho tới khi checklist còn lại pass.
