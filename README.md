# Jovi

Trợ lý giọng nói tiếng Việt cho Android. Bản thử nghiệm hiện tại hỗ trợ lệnh mở TikTok, YouTube, Messenger, Zalo, WhatsApp và ứng dụng SMS.

## Gọi Jovi khi đang dùng điện thoại

1. Trong Jovi, chọn **Thiết lập gọi Jovi bằng giọng nói**. Cấp quyền micrô và thông báo nếu được hỏi rồi bật dịch vụ **Jovi cho TikTok và YouTube** trong Trợ năng.
2. Về màn hình chính hoặc mở ứng dụng khác khi điện thoại đang mở khóa. Nói **“Jovi Jovi”**.
3. Khi khung chat ở góc trên bên phải hiện **Đang nghe…**, nói lệnh như **“Mở YouTube”**. Jovi sẽ hiện câu nhận được trong khung.

Xem [nhật ký thay đổi](CHANGELOG.md) để biết các bản sửa và phần đã kiểm thử.

## Thử TikTok

1. Mở Jovi, chạm **Nói với Jovi** và cấp quyền micrô.
2. Chọn **Cài đặt điều khiển TikTok và YouTube** và bật dịch vụ **Jovi cho TikTok và YouTube**.
3. Nói “Jovi, mở TikTok”. Khi đang ở bảng tin TikTok, chạm nút **Jovi** rồi nói “Video tiếp theo” để vuốt lên.

## Tìm trên YouTube

- Trong Jovi, chạm **Nói với Jovi** rồi nói “Jovi, tìm bài hát Believer trên YouTube”.
- Trong YouTube, bật dịch vụ Trợ năng **Jovi cho TikTok và YouTube**, chạm nút **Jovi** rồi nói “Tìm video cách pha cà phê”.
- YouTube mở màn hình kết quả. Chạm **Jovi** rồi nói “Mở kết quả thứ hai” để mở video thứ hai đang hiển thị. Khi đang xem, chạm nút **Jovi** và nói “Tạm dừng” hoặc “Phát tiếp”.

## Phạm vi phát triển

- Gọi “Jovi” khi màn hình tắt và điện thoại khóa. Các tác vụ cần mở khóa sẽ chờ xác thực của Android.
- Quản lý cuộc gọi SIM khi người dùng chọn Jovi làm ứng dụng Điện thoại mặc định.
- Tích hợp Messenger, Zalo, WhatsApp và SMS để nhận diện người gửi, tìm người theo tên, ghi nguyên văn, đọc lại rồi gửi sau xác nhận.
- Dùng tính năng tự điền của Android cho ứng dụng được phép. Ứng dụng ngân hàng nằm ngoài phạm vi tích hợp.

Các mục trên đang chờ triển khai hoặc xác minh.

## Build

Chạy `gradlew.bat :app:assembleDebug` với Android SDK 36.1 và JDK 21.
