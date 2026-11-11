# Bàn giao dự án Android Ứng dụng công thức nấu ăn

Cập nhật ngày 11/11/2026. Dành cho nhóm ba thành viên.

Tài liệu dựa trên lịch sử trao đổi trong cuộc trò chuyện này, tài liệu môn học đã đọc và kiểm tra workspace hiện tại. **Đã xác nhận** là yêu cầu hoặc hướng đi do người dùng nêu rõ. **Đề xuất** là phương án đã thảo luận để đặc tả hoặc triển khai. **Chưa xác minh** là hạng mục chưa có mã nguồn, cấu hình, nhật ký hoặc kết quả kiểm thử của đúng app để đối chiếu.

## A. Tổng quan và phạm vi

Tên đề tài trong hướng dẫn đã sửa là **Ứng dụng nấu ăn và công thức**, mã 08. Tên thương mại, tên project Android Studio và application ID chưa được xác nhận. App tiếng Việt hỗ trợ người dùng tìm món, tính lượng nguyên liệu, chuẩn bị mua sắm và theo dõi các bước nấu.

- **Người sử dụng app** tra cứu và nấu ăn không cần tài khoản. Món đã lưu, bộ sưu tập, ghi chú, lịch sử, kế hoạch hôm nay, danh sách mua sắm và tiến độ là dữ liệu cá nhân trên thiết bị.
- **Người quản lý nội dung** là thành viên có quyền quản lý dữ liệu trên Firebase. Phương án đã thảo luận là dùng Firebase Console để công bố, sửa và gỡ công thức. Quyền quản lý Firebase thuộc về nhóm dự án.
- **Firebase** cung cấp nội dung công khai. Món vừa công bố xuất hiện tự động trong **Món mới** sau khi app nhận và lưu được dữ liệu.
- **Phạm vi làm việc gần nhất** gồm chín module ở mục C. Đây là phạm vi kỹ thuật dùng để tiếp tục đặc tả, chưa có bằng chứng giảng viên đã duyệt.

Hướng dẫn môn yêu cầu nhóm 3–4 người, giảng viên xem xét nội dung trước khi nhóm thực hiện và mỗi thành viên phân tích, thiết kế, xây dựng tối thiểu một chức năng. Nhóm làm phần khung chung, tích hợp, bảo vệ dữ liệu cá nhân, viết báo cáo và bảo vệ. Tài liệu ghi báo cáo cuối kỳ 30% và bảo vệ cuối kỳ 30%. Mỗi phần có thang 10 điểm, phân bổ 3–4–3 cho giao diện, chức năng và tài liệu hoặc trình bày tương ứng.

Khung báo cáo yêu cầu trang bìa có phần việc chung, phần việc cá nhân và tổng tỷ lệ đóng góp 100%. Phần chung gồm lý do chọn đề tài, khảo sát ứng dụng tương tự, yêu cầu chức năng và phi chức năng, use case tổng quan, kiến trúc và công nghệ, phương thức lưu trữ, ERD hoặc cấu trúc bảng, điểm mới, điểm khó và hướng phát triển. Phần cá nhân trình bày luồng, ràng buộc, giao diện, giải pháp kỹ thuật, điểm mới, điểm khó và hướng mở rộng. Có thể dùng biểu đồ hoạt động để giải thích luồng phức tạp. Use case tổng quan và database thuộc thiết kế chung toàn hệ thống.

Bản hướng dẫn sửa liệt kê 15 nhóm tính năng. Phạm vi hiện tại bao phủ kho công thức Việt Nam và quốc tế, mua sắm, tìm kiếm, bộ lọc, chế độ nấu, yêu thích, khẩu phần, hẹn giờ và tiếng Việt. Video, chia sẻ qua mạng xã hội/email, góp ý và đánh giá, dinh dưỡng, diễn đàn và tích hợp giao hàng được đưa ra ngoài phạm vi bản đầu trong trao đổi. AI, thanh toán và đồng bộ tài khoản cũng nằm ngoài phạm vi. Việc chọn module optional xuất phát từ xác nhận của người dùng rằng có module tùy chọn. Danh mục cuối cùng vẫn cần đối chiếu với đề cương nhóm nộp.

Cookpad được khảo sát qua website Việt Nam, trang công thức, bài giới thiệu giao diện và mô tả Google Play. Các ý tưởng tham khảo gồm tìm theo nguyên liệu, tách nguyên liệu và các bước, bộ sưu tập và chọn món hôm nay. Những tính năng này đã có trên thị trường. Phần có thể trình bày khi bảo vệ là cách nhóm nối khẩu phần, mua sắm, tiến độ nấu và dữ liệu offline. [Cookpad Việt Nam](https://cookpad.com/vn/) · [Hướng dẫn giao diện](https://blog.cookpad.com/vn/kham-pha-giao-dien-moi-cua-cookpad/) · [Google Play](https://play.google.com/store/apps/details?id=com.cookpad.android.activities)

## B. Công nghệ và kiến trúc

| Hạng mục | Căn cứ hiện có | Việc cần xác nhận |
| --- | --- | --- |
| Java, Android Studio | Ghi trong hướng dẫn là công nghệ nhóm chọn | Đối chiếu project chính thức khi có mã nguồn |
| Gradle, AGP, JDK, Android SDK | Workspace chưa có cấu hình build của app công thức | Xác định phiên bản, Gradle Wrapper, minSdk, targetSdk và compileSdk |
| Giao diện | Đã thảo luận bốn mục điều hướng và các màn hình chức năng | Công nghệ giao diện, layout, theme và điều hướng thực tế |
| Firebase | Hướng đã nêu để cập nhật kho công thức | Project Firebase, sản phẩm cụ thể như Firestore hay Realtime Database, cấu trúc dữ liệu, quyền đọc/ghi, cách lưu ảnh |
| SQLite | Phương án đã thảo luận cho kho nội dung offline và dữ liệu cá nhân | Chọn cách truy cập SQLite, schema, migration và cách lưu ảnh offline |
| Kiến trúc | Có hướng tách giao diện, xử lý nghiệp vụ và lưu trữ | MVC, MVVM hoặc kiến trúc khác chưa được xác nhận |
| Dependency | Chưa có danh sách từ build của app | Firebase SDK, thư viện giao diện, ảnh, điều hướng và kiểm thử cần xác nhận khi khởi tạo hoặc đối chiếu project |

**Luồng dữ liệu thiết kế đang dùng để trao đổi** là nhóm quản lý công thức trên Firebase → app nhận nội dung công khai → lưu dữ liệu trên thiết bị → giao diện đọc dữ liệu đã lưu. Dữ liệu cá nhân được quản lý riêng trong lưu trữ cục bộ. Khi có mạng, app kiểm tra thay đổi lúc mở hoặc khi đang hoạt động. Khi mất mạng, app dùng nội dung đã tải. Ảnh offline cần cơ chế lưu riêng. Lịch cập nhật nền khi app đóng chưa được thống nhất.

## C. Đặc tả chín module

Tất cả module dưới đây hiện có nội dung phân tích trong trao đổi và **chưa đủ thông tin để xác định trạng thái triển khai**. Cột thành phần mô tả trách nhiệm và dữ liệu liên quan. Tên lớp, Activity, Fragment, Adapter, ViewModel, Repository, DAO và Database Helper thực tế chưa được xác minh.

| Module, mục đích và tác nhân | Luồng chính, đầu vào và đầu ra | Quy tắc và ngoại lệ cần đặc tả | Thành phần liên quan |
| --- | --- | --- | --- |
| **1. Khám phá và danh mục**. Người dùng xem và mở món phù hợp. | Mở Khám phá → xem danh mục, Món mới, món nhanh hoặc dễ làm → chọn thẻ món. Đầu vào là kho đã tải và danh mục. Đầu ra là danh sách và công thức được chọn. | Chỉ hiển thị món đã công bố. Món mới sắp theo ngày công bố. Khi tải lỗi, dùng dữ liệu đã có. Cần trạng thái rỗng, ảnh thay thế và quy tắc cho lần đầu mở offline. | Trang Khám phá, thẻ món, dữ liệu công thức và danh mục từ module 9. |
| **2. Tìm kiếm và bộ lọc**. Người dùng tìm món theo tiêu chí. | Nhập tên có dấu hoặc không dấu hoặc chọn nguyên liệu → áp dụng thời gian, độ khó, chế độ ăn và nguyên liệu loại trừ → xem kết quả. Lưu lịch sử tìm kiếm trên máy. | Kết quả tuân theo các bộ lọc đang bật. Phân biệt dữ liệu nguyên liệu thiếu với dữ liệu đã khai báo đầy đủ. Chốt quy tắc xử lý từ khóa trống, không có kết quả và xóa lịch sử. | Màn hình tìm kiếm, dữ liệu tên món, nguyên liệu, nhãn và lịch sử. |
| **3. Chi tiết và điều chỉnh khẩu phần**. Người dùng xem nội dung và tính lượng cần dùng. | Chọn món → xem ảnh, mô tả, thời gian chuẩn bị/nấu, dụng cụ, nguyên liệu, bước và mẹo → nhập số người. Đầu ra là định lượng theo khẩu phần đã chọn. | Lượng mới = lượng gốc × khẩu phần mới / khẩu phần gốc. Giữ nguyên công thức gốc, lượng định tính và thời gian nấu. Chặn đầu vào không hợp lệ. Giới hạn khẩu phần, làm tròn và cách hiển thị dữ liệu thiếu cần chốt. | Màn chi tiết, công thức, dòng nguyên liệu, bước nấu và phép tính dùng chung cho module 6–7. |
| **4. Nấu từng bước và hẹn giờ**. Người dùng làm theo hướng dẫn và tiếp tục phiên dở. | Bắt đầu từ chi tiết hoặc Nấu hôm nay → xem bước và nguyên liệu liên quan → hẹn giờ nếu cần → đánh dấu hoàn thành → quay lại tiếp tục hoặc kết thúc. Đầu ra là tiến độ và trạng thái hẹn giờ. | Mỗi bước có thứ tự. Phiên lưu cục bộ. Cần xác định cách tạm dừng/hủy, hết giờ khi app xuống nền, khi quyền thông báo bị từ chối và khi công thức đổi trong phiên. | Màn nấu, phiên nấu, tiến độ, hẹn giờ và thông báo nếu được cấp quyền. |
| **5. Kho món cá nhân**. Người dùng lưu và tìm lại món. | Lưu/bỏ yêu thích, tạo bộ sưu tập, mở lịch sử xem, thêm hoặc sửa ghi chú. Đầu vào là mã món và nội dung cá nhân. Đầu ra là kho món trên thiết bị. | Thao tác lưu lặp lại cần tránh bản ghi trùng. Xác định hành vi với tên bộ sưu tập trống/trùng, lịch sử rỗng và món đã gỡ. Giữ dữ liệu cá nhân khi đồng bộ kho. | Màn Kho của tôi, yêu thích, bộ sưu tập, lịch sử, ghi chú. |
| **6. Nấu hôm nay**. Người dùng chọn nhiều món cho một bữa. | Thêm hoặc bỏ món, đặt khẩu phần từng món, chuyển sang mua sắm hoặc bắt đầu nấu. Đầu ra là danh sách món và khẩu phần lưu cục bộ. | Mỗi món dùng khẩu phần đã chọn. Cần chốt cách xử lý thêm lại cùng món, kế hoạch rỗng và việc đổi khẩu phần sau khi đã tạo danh sách mua sắm. | Màn Nấu hôm nay, danh sách món, phép tính khẩu phần, điểm vào module 4 và 7. |
| **7. Danh sách mua sắm**. Người dùng chuẩn bị phần nguyên liệu còn thiếu. | Nhận món và khẩu phần → tổng hợp nguyên liệu → chỉnh lượng hoặc thêm mục thủ công → đánh dấu đã có, cần mua hoặc đã mua. Đầu ra là danh sách lưu cục bộ. | Gộp cùng nguyên liệu khi đơn vị tương thích, chẳng hạn g và kg. Giữ riêng đơn vị không quy đổi được như g và ml khi thiếu dữ liệu. Giữ lượng định tính. Cần chốt hành vi tạo lại danh sách và cập nhật khi bỏ món để tránh cộng trùng. | Màn Mua sắm, quy đổi đơn vị, danh sách và từng mục mua sắm. |
| **8. Gợi ý từ nguyên liệu sẵn có**. Người dùng tìm món từ thứ đang có. | Chọn nguyên liệu → so khớp với công thức → xem món kèm phần đã có và còn thiếu. Ưu tiên món thiếu ít nguyên liệu hơn. | Bản đầu đối chiếu có/không, chưa quản lý lượng tồn. Cần chốt thứ tự khi đồng hạng, khi không chọn nguyên liệu, khi không có món phù hợp và khi công thức thiếu thông tin. | Mục Có gì nấu nấy, dữ liệu nguyên liệu, quy tắc đối chiếu và xếp hạng. |
| **9. Cập nhật kho và offline**. Nhóm quản lý nội dung. Người dùng nhận bản cập nhật. | Nhóm kiểm tra và công bố nội dung trên Firebase → app nhận công thức mới, sửa hoặc gỡ → cập nhật SQLite → làm mới Món mới và các danh sách. | Mã công thức ổn định. Tách thời điểm công bố khỏi chỉnh sửa. Chỉ cấp quyền ghi cho nhóm. Giữ dữ liệu cá nhân khi cập nhật. Cần đặc tả lỗi tải, lần tải lại, dữ liệu không hợp lệ và gỡ nội dung. | Firebase, lưu trữ SQLite, bộ cập nhật kho, giao diện Khám phá và trạng thái kết nối. |

Use case tổng quan lấy người dùng app làm tác nhân chính. Hành vi công bố nội dung có thể mô tả trong ranh giới hệ thống rộng hơn nếu bao gồm Firebase Console. ERD và biểu đồ use case hoàn chỉnh chưa có trong các tệp đã kiểm tra.

## D. Thiết kế cơ sở dữ liệu

Chưa tìm thấy schema, migration, bảng đang chạy hoặc dữ liệu Firebase của app. Bảng dưới là **bản phác thảo**, dùng các tên thực thể đã xuất hiện trong trao đổi. Các kiểu là gợi ý cho SQLite. Tên thuộc tính, khóa và ràng buộc chưa phải hợp đồng đã duyệt.

| Thực thể đã thảo luận | Thuộc tính và kiểu gợi ý | Khóa, quan hệ và ràng buộc đề xuất |
| --- | --- | --- |
| `Recipe` | Mã ổn định TEXT, tên TEXT, mô tả TEXT, khẩu phần INTEGER, thời gian chuẩn bị/nấu INTEGER, độ khó TEXT, ngày công bố/chỉnh sửa INTEGER | Mã món là PK. Khẩu phần gốc > 0. Thời gian không âm. Cần chốt biểu diễn công bố/gỡ và quan hệ danh mục. |
| `Category` | Mã và tên TEXT | Mã danh mục là PK. Quyết định mỗi món có một hay nhiều danh mục trước khi chốt FK. |
| `Ingredient` | Mã và tên chuẩn TEXT | Mã nguyên liệu là PK. Mã chuẩn giúp tìm kiếm và gộp mua sắm. |
| `RecipeIngredient` | Mã dòng, mã món, mã nguyên liệu TEXT, lượng số có thể rỗng, đơn vị và ghi chú TEXT | Mã dòng là PK đề xuất. Hai FK liên kết `Recipe` và `Ingredient`, biểu diễn quan hệ nhiều–nhiều. Cho phép nguyên liệu xuất hiện ở nhiều công đoạn. Chốt biểu diễn số lượng và lượng định tính. |
| `CookingStep` | Mã bước, mã món, nội dung TEXT, thứ tự và thời lượng gợi ý INTEGER | PK mã bước, FK món. Thứ tự duy nhất trong món. Liên kết nguyên liệu theo bước nếu cần hiển thị ở module 4. |
| `Favorite` | Mã món TEXT, thời điểm lưu INTEGER | Mỗi mã món có tối đa một lượt lưu trong kho cục bộ. Chốt cách giữ dữ liệu khi công thức bị gỡ. |
| `ShoppingList`, `ShoppingListItem` | Mã danh sách/mục TEXT, mã nguyên liệu tùy chọn, tên nhập tay TEXT, lượng, đơn vị TEXT, trạng thái TEXT | Danh sách 1–n mục. Cho phép mục thủ công. Cần giữ nguồn món hoặc quy tắc cập nhật tương đương nếu hỗ trợ tạo lại danh sách. |
| `CookingSession`, `CookingStepProgress` | Mã phiên/món/bước TEXT, trạng thái TEXT, thời điểm INTEGER | Phiên 1–n bản ghi bước. Chốt khóa, lưu hẹn giờ và cách giữ phiên đang nấu khi nội dung đổi. |

Bộ sưu tập và liên kết món, ghi chú, lịch sử xem/tìm kiếm, món trong Nấu hôm nay, nhãn chế độ ăn, ảnh và hẹn giờ cũng có nhu cầu dữ liệu. Cấu trúc và kiểu của chúng chưa được thống nhất. Dữ liệu gợi ý nguyên liệu có thể tính từ kho công thức và lựa chọn hiện tại, tùy quyết định có lưu lựa chọn hay không.

**CRUD theo hướng đã thảo luận.** Nhóm quản lý tạo, sửa, công bố hoặc gỡ công thức ở Firebase. App đọc nội dung công khai, tạo/cập nhật bản sao SQLite và phản ánh việc gỡ trên giao diện. Người dùng tạo, đọc, sửa và xóa dữ liệu cá nhân trên thiết bị. Chưa có API, DAO hoặc câu SQL thực tế. Khuyến nghị áp dụng một lần cập nhật kho trong giao dịch SQLite khi nội dung liên quan phải nhất quán, và thống nhất cách giữ liên kết tới món đã gỡ trước khi bật xóa dây chuyền.

## E. Mã nguồn và điều hướng

Thư mục workspace `project` chứa ba tài liệu tham khảo và thư mục `.source-review`. Kiểm tra chưa thấy Gradle, manifest, source Android hoặc cấu hình Firebase của app tại đây. Một số project Android nằm ở các thư mục lân cận dưới thư mục cha, nhưng tên và lượt tìm theo nội dung liên quan chưa đủ để xác nhận chúng là app công thức. Chưa thể gán bất kỳ Activity, Fragment, Adapter, ViewModel, Repository, DAO, Database Helper, package hoặc đoạn mã nào cho dự án.

Luồng điều hướng **đề xuất đã trao đổi** có bốn mục chính.

- **Khám phá** → danh mục/tìm kiếm/gợi ý → chi tiết công thức.
- **Chi tiết công thức** → lưu món, đổi khẩu phần, thêm vào Nấu hôm nay hoặc bắt đầu nấu.
- **Nấu hôm nay** → chỉnh khẩu phần từng món → Mua sắm hoặc Nấu từng bước.
- **Mua sắm** → theo dõi nguyên liệu. **Kho của tôi** → mở lại chi tiết món.

Hợp đồng tích hợp đề xuất là truyền mã công thức ổn định giữa các màn hình và truyền khẩu phần hoặc đọc khẩu phần đã lưu khi lập danh sách và mở phiên nấu. Module 3, 6 và 7 dùng cùng quy tắc định lượng. Module 1, 2, 3 và 8 dùng chung kho nội dung cục bộ. Hành vi của phiên đã bắt đầu khi nội dung công thức được sửa còn cần quyết định.

## F. Phân công và Git

Nhóm có ba người, tên thật chưa được cung cấp. Phân công cũ dựa trên phạm vi tám module. Với chín module mới, bảng dưới là **đề xuất cập nhật**, kế thừa trách nhiệm cũ. Mỗi người phụ trách trọn phân tích, đặc tả, thiết kế giao diện, đề xuất dữ liệu, lập trình, kiểm thử và phần báo cáo cho module được giao.

| Người | Module đề xuất | Giao diện tích hợp cần bàn giao |
| --- | --- | --- |
| Người 1 | 1 Khám phá, 2 Tìm kiếm và bộ lọc, 8 Gợi ý nguyên liệu | Danh mục, dữ liệu cần tra cứu, mã công thức để mở chi tiết |
| Người 2 | 3 Chi tiết và khẩu phần, 4 Nấu từng bước và hẹn giờ, 9 Cập nhật kho và offline | Mô hình công thức cục bộ, phép tính khẩu phần, trạng thái cập nhật, phiên nấu |
| Người 3 | 5 Kho món, 6 Nấu hôm nay, 7 Mua sắm | Mã món lưu cục bộ, kế hoạch và khẩu phần, danh sách nguyên liệu |

Module 4 và 9 có rủi ro khối lượng cao. Trong phương án này, cả nhóm cùng chuẩn bị dữ liệu mẫu và đối chiếu schema. Người 1 tham gia kiểm thử cập nhật Món mới, Người 3 tham gia kiểm thử việc giữ dữ liệu cá nhân sau đồng bộ. Tỷ lệ đóng góp ghi theo phần việc thực tế, tổng 100%.

Use case tổng quan và database do cả nhóm hợp nhất từ đặc tả từng module. Phải thống nhất sớm mã công thức/nguyên liệu, quy tắc đơn vị, cấu trúc dữ liệu và điều hướng. Các điểm dễ xung đột khi merge là điều hướng chung, resource giao diện, cấu hình Gradle, schema/migration và dữ liệu mẫu.

Git hiện quan sát được ở thư mục cha `C:/Users/ADMIN/Downloads/Android`. Checkout ở `main`, theo dõi `origin/main`, commit `b2e546b` có tiêu đề `first commit`. Có thay đổi staged, unstaged và untracked ngoài thư mục dự án. Những dữ kiện này thuộc repo cha. Chưa xác định repo chính thức, branch của ba người hay quy ước merge cho app. Phương án khi có repo chính thức là mỗi phần việc có branch riêng, review thay đổi schema/điều hướng trước khi merge, và mỗi người chạy cùng kịch bản tích hợp sau khi ghép.

## G. Tiến độ

Các cột người phụ trách dùng phân công đề xuất ở mục F. Nội dung hoàn thành ghi nhận kết quả trao đổi hoặc rà soát tài liệu. Trạng thái của từng module dưới đây là trạng thái **triển khai**, không suy ra từ thiết kế.

| Module/Công việc | Người phụ trách | Đã hoàn thành | Còn thiếu | Trạng thái | Bước tiếp theo |
| --- | --- | --- | --- | --- | --- |
| 1. Khám phá | Người 1 | Mô tả phạm vi và Món mới | Thiết kế màn, nguồn dữ liệu, mã và test cần xác minh | Chưa đủ thông tin để xác định | Chốt cấu trúc thẻ món và quy tắc Món mới |
| 2. Tìm kiếm/bộ lọc | Người 1 | Chốt nhóm tiêu chí ở mức yêu cầu | Đặc tả truy vấn, trạng thái rỗng, mã và test | Chưa đủ thông tin để xác định | Lập bộ dữ liệu kiểm tra tiếng Việt và bộ lọc |
| 3. Chi tiết/khẩu phần | Người 2 | Có ví dụ đặc tả và công thức tính | Ràng buộc khẩu phần, làm tròn, giao diện, mã và test | Chưa đủ thông tin để xác định | Chốt quy tắc định lượng dùng chung |
| 4. Nấu/hẹn giờ | Người 2 | Mô tả bước, phiên và hẹn giờ | Trạng thái phiên, xử lý nền, mã và test | Chưa đủ thông tin để xác định | Vẽ luồng và đặc tả khôi phục |
| 5. Kho cá nhân | Người 3 | Liệt kê chức năng và lưu trên máy | Schema và xử lý món bị gỡ, mã và test | Chưa đủ thông tin để xác định | Chốt quy tắc lưu và mở lại món |
| 6. Nấu hôm nay | Người 3 | Mô tả luồng chọn nhiều món | Hợp đồng dữ liệu với 3, 4, 7, mã và test | Chưa đủ thông tin để xác định | Đặc tả thêm, bỏ và đổi khẩu phần |
| 7. Mua sắm | Người 3 | Có quy tắc gộp đơn vị tương thích | Chốt tạo lại, giữ nguồn và chỉnh sửa, mã và test | Chưa đủ thông tin để xác định | Đặc tả phép gộp và kiểm tra cộng trùng |
| 8. Gợi ý nguyên liệu | Người 1 | Phân biệt với tìm kiếm thông thường | Chốt xếp hạng và ngoại lệ, mã và test | Chưa đủ thông tin để xác định | Viết ví dụ có/thiếu nguyên liệu |
| 9. Cập nhật/offline | Người 2 | Hướng Firebase → SQLite → Món mới | Project Firebase, quyền, schema, mã và test | Chưa đủ thông tin để xác định | Chốt hợp đồng nội dung và luồng cập nhật |
| Use case tổng quan và database | Cả nhóm | Có nhóm tác nhân, thực thể và phương pháp phân tích | Biểu đồ, schema chính thức, review liên module | Đang thực hiện ở mức phân tích | Hợp nhất đặc tả từ chín module |
| Khảo sát và báo cáo | Cả nhóm | Đã đọc tài liệu, đối chiếu Cookpad và nắm khung báo cáo | Báo cáo nhóm/cá nhân và tỷ lệ thực tế | Đang thực hiện ở mức chuẩn bị | Liên kết yêu cầu với thiết kế, mã và test |
| Build, tích hợp, APK và kiểm thử | Cả nhóm | Chưa có kết quả xác minh của app | Repo chính thức, build chạy, báo cáo test và APK | Chưa đủ thông tin để xác định | Xác định project và chạy baseline khi có mã |

## H. Lỗi và vấn đề đã ghi nhận

| Vấn đề | Triệu chứng | Nguyên nhân hoặc căn cứ | Xử lý đã thực hiện | Trạng thái |
| --- | --- | --- | --- | --- |
| Nội dung đề tài bản cũ mâu thuẫn | Tiêu đề là app công thức nhưng danh sách chức năng mô tả phòng chat và nhóm sở thích | Phần mô tả trong bản cũ không khớp đề tài. Nguyên nhân biên tập sâu hơn chưa được xác định. | Người dùng sửa file hướng dẫn. Đọc lại bản sửa có công thức, video, mua sắm và các chức năng nấu ăn. | Đã khắc phục trong tài liệu. |
| Khó xác nhận project triển khai | Workspace chỉ thấy tài liệu, repo cha chứa các project lân cận | Chưa có bằng chứng gắn những project đó với app công thức | Kiểm tra vị trí repo, file cấu hình và tìm từ khóa liên quan | Chưa xác định đường dẫn hoặc repo chính thức của mã app. |

Lịch sử được cung cấp chưa có lỗi Android Studio/Gradle, lỗi lập trình, database, giao diện hoặc tích hợp của đúng app kèm triệu chứng, nguyên nhân và bản sửa đã xác minh. Các tình huống mất mạng, món bị gỡ, định lượng không hợp lệ, hết giờ và xung đột khi merge là hạng mục kiểm thử hoặc thiết kế, chưa phải lỗi đã xảy ra.

## I. Quyết định và thay đổi

| Chủ đề | Phương án trước | Trạng thái gần nhất |
| --- | --- | --- |
| Đề tài | Bản cũ lẫn mô tả cộng đồng/chat | Bản sửa và người dùng xác nhận app tập trung vào món ăn |
| Phạm vi | 15 nhóm chức năng trong hướng dẫn, rồi đề xuất MVP tám module | Phạm vi làm việc mới nhất gồm chín module. Bổ sung Nấu hôm nay, gợi ý nguyên liệu, cập nhật/offline. Gộp chế độ nấu với hẹn giờ, khẩu phần với chi tiết, và tìm kiếm với bộ lọc |
| Tài khoản | Cân nhắc tài khoản để đồng bộ và quản lý | Người dùng nêu app dự định không cần đăng nhập. Dữ liệu cá nhân lưu trên máy trong phương án thiết kế hiện tại |
| Nội dung | Cân nhắc kho cài sẵn hoặc công cụ quản trị | Người dùng yêu cầu cập nhật món trên Firebase và tự đưa vào Món mới. Firebase Console và SQLite là cách triển khai đã thảo luận |
| Món mới | Chưa có tiêu chí sắp xếp cụ thể | Bản tổng hợp gần nhất đề xuất sắp theo ngày công bố và giữ ngày chỉnh sửa riêng |
| Trách nhiệm | Chia theo luồng hoặc chuyên môn | Người dùng yêu cầu chia theo module, mỗi người phân tích và thiết kế phần mình. Use case tổng quan và database được tổng hợp chung |
| Kiến trúc, Git, schema | Đã thảo luận nguyên tắc tách trách nhiệm và lưu dữ liệu | Chưa có xác nhận mô hình kiến trúc cụ thể, cấu trúc package, schema chính thức hoặc repo và branch app |

## J. Kế hoạch phát triển

1. **Xác định project chính thức và phạm vi nộp.** Đối chiếu đường dẫn repo nếu nhóm đã có mã. Gắn tên thật vào phân công và ghi nhận module optional trong đề cương giảng viên xem xét.
2. **Đặc tả và thiết kế chung.** Viết luồng chính, ngoại lệ, đầu vào/ra và quy tắc của từng module. Hợp nhất use case, ERD, hợp đồng nội dung Firebase và điều hướng. Chốt quy tắc công bố/gỡ món, mã định danh, đơn vị, khẩu phần và quyền đọc/ghi.
3. **Thiết lập hoặc xác minh baseline.** Chốt cấu hình Android Studio/Gradle/SDK, phương pháp giao diện, kiến trúc, dependency, SQLite và cách nạp dữ liệu ban đầu. Build app khung và chạy trên môi trường demo.
4. **Tích hợp theo thứ tự phụ thuộc.** Hoàn thành kho công thức và cập nhật thử cùng màn Khám phá/Chi tiết. Tiếp theo ghép tìm kiếm, gợi ý, kho cá nhân, khẩu phần, Nấu hôm nay và mua sắm. Ghép chế độ nấu/hẹn giờ trên cùng mã công thức và dữ liệu bước.
5. **Kiểm thử theo đặc tả.** Kiểm tra công bố, sửa, gỡ món, mất mạng và kết nối lại. Kiểm tra tìm tiếng Việt có/không dấu, lọc và gợi ý với dữ liệu thiếu. Kiểm tra khẩu phần bằng 0, số âm, làm tròn, lượng định tính, gộp g/kg và giữ riêng g/ml. Thử tạo lại danh sách để phát hiện cộng trùng. Thử đóng/mở, đưa app xuống nền và quay lại phiên nấu, cùng tình huống dữ liệu cá nhân còn nguyên sau cập nhật. Ghi kết quả mong đợi và kết quả thực tế.
6. **Bàn giao và bảo vệ.** Tạo APK sau khi build và kiểm thử, chuẩn bị dữ liệu demo và kịch bản xuyên suốt từ món mới đến hoàn tất phiên nấu. Báo cáo nhóm có use case tổng quan và ERD/cấu trúc bảng, mỗi người có ít nhất một chức năng chạy được và giải thích được phần mình. Ghi tổng đóng góp 100%.

## Nguồn trong workspace

- [Hướng dẫn dự án nhóm Mobile App D22](<1.Huong dan du an nhom Mobile App D22.doc>)
- [Khung báo cáo bài tập nhóm Mobile App](<Yeu cau bai tap nhom Mobile App 11.2025 (1).docx>)
- [Giáo trình Phân tích thiết kế và đảm bảo chất lượng phần mềm](<BG HP TTTN 2 CNPM 2020 final.pdf>). Mục 3.1.2–3.1.3 về nghiệp vụ và use case, 3.2.1–3.2.2 về kịch bản và thực thể, 4.2.1 về thiết kế CSDL, chương 5–6 về kiểm thử và rà soát.
