# Bản vá Jovi

## Cập nhật sau bản 1.2 thử nghiệm

- Gọi **“Jovi Jovi”** khi màn hình đang mở và máy đã mở khóa để hiện khung chat ở góc trên bên phải. Khung báo **Đang nghe…**, hiện lời nói thành chữ và tự ẩn sau khi trả kết quả. Có thể bấm **×** để đóng.
- Thêm nút **Thiết lập gọi Jovi bằng giọng nói** để cấp quyền micrô, quyền thông báo và bật dịch vụ Trợ năng của Jovi.
- Sửa lỗi dịch vụ nghe nền giữ micrô khi bắt đầu nhận lệnh. Jovi đợi micrô được nhả và thử kết nối lại nếu luồng nghe nền mất tín hiệu.
- Trong giao diện tiếng Việt, **Nói với Jovi** dùng nhập giọng nói của Android. Nút **Nhận dạng ngoại tuyến** và khung chat nổi dùng mô hình Vosk tiếng Việt đi kèm APK.
- Chấp nhận một số cách phát âm tên ứng dụng, ví dụ “iu túp” và “du túp” cho YouTube. Nếu câu nhận được chưa khớp lệnh, Jovi hiện lại câu đó.
- Đã xác minh trên Pixel ảo rằng câu gọi có thể mở khung nổi và khung hiển thị chữ nhận dạng. Độ chính xác của lệnh tiếng Việt và micrô trên điện thoại thật vẫn cần kiểm thử thêm.

## 1.2 thử nghiệm

- Thêm dịch vụ micrô chạy nền nhận câu “Jovi Jovi” trên thiết bị bằng Vosk.
- Tự bật nghe nền sau khi cấp quyền. Jovi tự kết thúc phiên nghe lệnh và quay về chờ câu gọi.
- Khi phát hiện câu gọi, Jovi yêu cầu Android hiện giao diện. Các lệnh mở ứng dụng yêu cầu mở khóa điện thoại.
- Mô hình tiếng Anh Vosk Small 0.15 được đóng gói trong APK. Nhận dạng trên iQOO và độ chính xác của câu gọi cần kiểm thử.

## 1.1

- Thêm nút chọn Jovi làm trợ lý mặc định qua hộp thoại Android.
- Jovi mở khi được gọi bằng thao tác trợ lý của hệ thống trên các máy hỗ trợ.
- Từ khóa đánh thức “Jovi Jovi” chưa được triển khai.

## 1.0

- Nút Jovi cập nhật theo sự kiện cửa sổ và bỏ kiểm tra lặp mỗi 300 ms.
- Chuẩn hóa lệnh, thêm biến thể tên Jovi, giới hạn lệnh tìm kiếm vào đúng ngữ cảnh YouTube.
- Lọc kết quả video trùng, sắp theo vị trí và thử lại khi trình phát chưa hiện nút.
- Báo riêng lỗi ngôn ngữ, mạng và dịch vụ nhận dạng. Giữ lựa chọn ứng dụng đã lưu khi nâng cấp.
- Tích hợp avatar và phương án nhập giọng nói Android từ các bản thử.

## 0.6.2

- Thêm nút dùng hộp thoại nhập giọng nói Android.

## 0.6.1

- Kết thúc phiên nghe nếu dịch vụ nhận dạng không phản hồi.

## 0.6

- Màn hình thiết lập lần đầu cho phép chọn từng ứng dụng, sau đó mở hộp thoại cấp quyền micrô của Android.
- Có thể thay đổi danh sách trong **Thiết lập Jovi**. Các lệnh mở ứng dụng và điều khiển YouTube, TikTok áp dụng lựa chọn này.
- Bản này vẫn kích hoạt micrô bằng nút. Gọi “Jovi” khi máy khóa và thông báo nghe nền đang chờ triển khai.

## 0.5

- Giao diện, thông báo và các lệnh hiện có của Jovi hỗ trợ tiếng Việt và tiếng Anh.
- Trên Android 13 trở lên, mở **Ngôn ngữ · Language** để chọn ngôn ngữ riêng cho Jovi. Trên Android cũ hơn, Jovi theo ngôn ngữ hệ thống.
- Jovi dùng ngôn ngữ đã chọn để nhận dạng giọng nói. Hộp thoại Android và giao diện ứng dụng khác theo cài đặt của từng ứng dụng hoặc hệ thống.

## 0.4.1

- Đặt lại trạng thái micrô khi rời màn hình Jovi trong lúc đang nghe.

## 0.4

- Giao diện chính xanh dương nhạt, nút “Nói với Jovi” nổi bật và trạng thái micrô hiển thị ngay phía trên.
- Thêm lệnh mẫu và lối vào cài đặt Trợ năng ngay trên màn hình chính.

## 0.3

- Chọn một video đang hiện trên màn hình YouTube bằng lệnh “Mở kết quả thứ hai” hoặc “Phát video hai”.
- Thêm “Tạm dừng” và “Phát tiếp” trong trình phát YouTube.
- Các lệnh trên dùng nút Jovi trong YouTube và phụ thuộc vào việc trình phát hoặc danh sách cung cấp nút điều khiển cho Android Trợ năng.

## 0.2

- Thêm lệnh “Mở YouTube” và tìm bài hát, bài nhạc hoặc video bằng lời nói tiếng Việt.
- Bạn có thể tìm từ ứng dụng Jovi hoặc qua nút Jovi khi đang ở trong YouTube. Cụm từ đã đọc được chuyển vào ô tìm kiếm của YouTube.
- YouTube hiển thị danh sách kết quả để bạn chọn video muốn phát.
- Chế độ gọi “Jovi” khi khóa máy và các lệnh YouTube khác chưa có trong bản này.
